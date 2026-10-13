# Tesseract Android Client

Black Candle's Android client for Tesseract (internal Matrix chat), a rebranded fork of Element X Android. Defaults to the Black Candle homeserver (chat.internal.blackcandletech.com) with MAS/OIDC sign-in via authd. Distributed by direct APK install (sideload) — no Play Store, no FCM.

## Upstream pin

- Upstream: element-hq/element-x-android
- Release tag: v26.09.4
- Fork: https://github.com/blackcandlelabs/tesseract-android.git
- Branch: tesseract

## Patch inventory

1. plugins/src/main/kotlin/config/BuildTimeConfig.kt — APPLICATION_ID com.blackcandle.tesseract (was io.element.android.x), APPLICATION_NAME "Tesseract", METADATA_HOST_REVERSED com.blackcandle.tesseract (OAuth redirect scheme for MAS login), PUSH_CONFIG_INCLUDE_FIREBASE=false (push arrives with the Sygnal/FCM milestone; UnifiedPush stays compiled in).
2. libraries/matrix/api AccountProvider.kt + login usages — default account provider is chat.internal.blackcandletech.com (symbol renamed matrixOrgAccountProvider -> tesseractAccountProvider; tests updated). Users can still choose another server manually.
3. appicon/element — launcher icons replaced with the original Tesseract candle mark (amber #E8A33D on #08090B) at all densities; adaptive-icon background #08090B.

## Build (release APK, sideload)

Toolchain: JDK 21, Android SDK platform 37 + build-tools 37.0.0 (see the tesseract-android-build Docker image used by Black Candle).

    ./gradlew :app:assembleFdroidRelease

produces an unsigned universal APK under app/build/outputs/apk/fdroid/release/. Sign with the Tesseract release keystore (held offline by the founders; generated 2026-10-12):

    zipalign -p 4 app-fdroid-universal-release-unsigned.apk aligned.apk
    apksigner sign --ks tesseract-release.keystore --ks-key-alias tesseract --out tesseract.apk aligned.apk
    apksigner verify --print-certs tesseract.apk

## Monthly rebase

    git remote add upstream https://github.com/element-hq/element-x-android.git || true
    git fetch upstream --tags
    git checkout tesseract
    git checkout -b rebase/tesseract-vNEXT
    git rebase vNEXT_TAG
    # resolve, rebuild, re-verify the patch inventory, open a PR for approval

Rebases ship as PRs for founder approval, never silently (SPEC section 18).
