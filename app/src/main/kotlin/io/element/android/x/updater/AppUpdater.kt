/*
 * Copyright (c) 2026 Black Candle Technologies
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.updater

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.element.android.x.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Tesseract fork addition: self-update via GitHub Releases.
 *
 * The update channel is the public GitHub repository
 * `blackcandlelabs/tesseract-android`. A release is installable only if it
 * carries a `release.json` asset describing its builds per flavor. The app
 * selects the newest build for its own flavor with a higher versionCode,
 * downloads the APK matching the device ABI, and verifies it before handing
 * off to the Android package installer:
 *  1. SHA-256 of the file must match the digest published in release.json
 *     (and the digest GitHub reports for the asset, when present).
 *  2. The APK's signing certificate must match the pinned Tesseract release
 *     certificate.
 * Any mismatch discards the download. Fail closed, always.
 */
@Serializable
private data class GhAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val digest: String? = null,
)

@Serializable
private data class GhRelease(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean = false,
    val assets: List<GhAsset> = emptyList(),
)

@Serializable
private data class ReleaseIndex(
    val schema: Int,
    val builds: List<ReleaseBuild> = emptyList(),
)

@Serializable
private data class ReleaseBuild(
    val flavor: String,
    @SerialName("version_code") val versionCode: Long,
    @SerialName("version_name") val versionName: String,
    val files: List<ReleaseFile> = emptyList(),
)

@Serializable
private data class ReleaseFile(
    val abi: String,
    val name: String,
    val sha256: String,
)

private data class UpdateCandidate(
    val tagName: String,
    val build: ReleaseBuild,
    val file: ReleaseFile,
    val asset: GhAsset,
)

class AppUpdater(private val activity: Activity) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    suspend fun checkForUpdate() {
        delay(3_000)
        if (activity.isFinishing || activity.isDestroyed) return
        val prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CHECK, 0L) < CHECK_INTERVAL_MS) return
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        val candidate = runCatching { findCandidate() }
            .onFailure { Timber.tag(TAG).w(it, "Update check failed") }
            .getOrNull() ?: return
        withContext(Dispatchers.Main) {
            if (!activity.isFinishing && !activity.isDestroyed) prompt(candidate)
        }
    }

    private suspend fun findCandidate(): UpdateCandidate? = withContext(Dispatchers.IO) {
        val installed = activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode
        val flavor = BuildConfig.FLAVOR
        val releasesText = httpGet(RELEASES_URL) ?: return@withContext null
        val releases = json.decodeFromString<List<GhRelease>>(releasesText)
        for (release in releases) {
            if (release.draft) continue
            val indexAsset = release.assets.firstOrNull { it.name == "release.json" } ?: continue
            val indexText = httpGet(indexAsset.browserDownloadUrl) ?: continue
            val index = runCatching { json.decodeFromString<ReleaseIndex>(indexText) }.getOrNull() ?: continue
            if (index.schema != 1) continue
            val build = index.builds.firstOrNull { it.flavor == flavor && it.versionCode > installed } ?: continue
            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "universal"
            val file = build.files.firstOrNull { it.abi == abi }
                ?: build.files.firstOrNull { it.abi == "universal" }
                ?: continue
            val asset = release.assets.firstOrNull { it.name == file.name } ?: continue
            return@withContext UpdateCandidate(release.tagName, build, file, asset)
        }
        null
    }

    private fun prompt(candidate: UpdateCandidate) {
        AlertDialog.Builder(activity)
            .setTitle("Update available")
            .setMessage("Tesseract ${candidate.build.versionName} is ready. Install the update now?")
            .setPositiveButton("Update") { _, _ -> downloadAndInstall(candidate) }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstall(candidate: UpdateCandidate) {
        val progress = AlertDialog.Builder(activity)
            .setMessage("Downloading update…")
            .setCancelable(false)
            .show()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { downloadAndVerify(candidate) }
            }
            progress.dismiss()
            result.fold(
                onSuccess = { file -> install(file) },
                onFailure = { error ->
                    Timber.tag(TAG).e(error, "Update download or verification failed")
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        showMessage("Update failed", "The update could not be verified and was discarded.")
                    }
                },
            )
        }
    }

    private fun downloadAndVerify(candidate: UpdateCandidate): File {
        val target = File(activity.cacheDir, "tesseract-update.apk")
        if (target.exists()) {
            check(target.delete()) { "Cannot clear previous download" }
        }
        val request = Request.Builder()
            .url(candidate.asset.browserDownloadUrl)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = response.body ?: error("Empty response body")
            body.byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        val digest = sha256Hex(target)
        check(digest.equals(candidate.file.sha256, ignoreCase = true)) { "SHA-256 does not match release.json" }
        val published = candidate.asset.digest
        if (published != null) {
            check(published.equals("sha256:$digest", ignoreCase = true)) { "SHA-256 does not match the GitHub asset digest" }
        }
        check(apkSignerSha256(target).equals(PINNED_CERT_SHA256, ignoreCase = true)) { "APK signer does not match the pinned release certificate" }
        return target
    }

    private fun apkSignerSha256(apk: File): String {
        @Suppress("DEPRECATION")
        val info = activity.packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("Downloaded file is not a valid APK")
        val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        val certificate = signers?.firstOrNull()?.toByteArray() ?: error("APK has no signer")
        return sha256Hex(certificate)
    }

    private fun install(apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            AlertDialog.Builder(activity)
                .setTitle("Allow app installs")
                .setMessage("Tesseract needs permission to install updates. Enable \"Allow from this source\", then reopen Tesseract and the update will install.")
                .setPositiveButton("Open settings") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.packageName),
                    )
                    activity.startActivity(intent)
                }
                .setNegativeButton("Later", null)
                .show()
            return
        }
        val uri = FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    private fun showMessage(title: String, message: String) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun httpGet(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    companion object {
        private const val TAG = "AppUpdater"
        private const val PREFS_NAME = "tesseract_updater"
        private const val KEY_LAST_CHECK = "last_check_ms"
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private const val USER_AGENT = "Tesseract-Updater"
        private const val RELEASES_URL = "https://api.github.com/repos/blackcandlelabs/tesseract-android/releases?per_page=10"
        private const val PINNED_CERT_SHA256 = "512f0b7d9da8b114fa27c42585ad96d81322afdd5efe1db898816e08d2332082"
        private val json = Json { ignoreUnknownKeys = true }
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
