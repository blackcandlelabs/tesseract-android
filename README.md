# Tesseract Android Client

Black Candle's Android client for Tesseract (internal Matrix chat), a rebranded fork of Element X Android. Defaults to the Black Candle homeserver (chat.internal.blackcandletech.com) with MAS/OIDC sign-in via authd. Distributed via this repo’s GitHub Releases with a built-in self-updater; push via FCM through Black Candle’s own Sygnal gateway.

## Upstream pin

- Upstream: element-hq/element-x-android
- Release tag: v26.09.4
- Fork: https://github.com/blackcandlelabs/tesseract-android.git
- Branch: tesseract

## Patch inventory

1. plugins/src/main/kotlin/config/BuildTimeConfig.kt — APPLICATION_ID com.blackcandle.tesseract (was io.element.android.x), APPLICATION_NAME "Tesseract", METADATA_HOST_REVERSED com.blackcandle.tesseract (OAuth redirect scheme for MAS login), push via the Firebase provider in the gplay flavor (see item 4); UnifiedPush stays compiled in.
2. libraries/matrix/api AccountProvider.kt + login usages — default account provider is chat.internal.blackcandletech.com (symbol renamed matrixOrgAccountProvider -> tesseractAccountProvider; tests updated). Users can still choose another server manually.
3. appicon/element — launcher icons replaced with the original Tesseract candle mark (amber #E8A33D on #08090B) at all densities; adaptive-icon background #08090B.
4. libraries/pushproviders/firebase FirebaseConfig.kt — PUSHER_HTTP_URL re-pointed from matrix.org’s Sygnal to https://chat.internal.blackcandletech.com/_matrix/push/v1/notify (our Sygnal). FCM push requires the gplay flavor with google-services.json (Firebase project bct-tesseract) injected at app/ — a build input, not committed.
5. plugins/src/main/kotlin/config/BuildTimeConfig.kt (OAuth identity) — METADATA_HOST_REVERSED com.blackcandletech.tesseract (OAuth redirect scheme; MAS registration policy requires the scheme to be the reverse-DNS of the client_uri host) and URL_WEBSITE/URL_LOGO/URL_ACCEPTABLE_USE/URL_PRIVACY/URL_POLICY pointed at https://tesseract.blackcandletech.com (was: element.io defaults, which made MAS reject dynamic client registration with invalid_redirect_uri). OAUTH_CLIENT_URL_PATH null.
6. Self-updater (app/src/main/kotlin/io/element/android/x/updater/AppUpdater.kt, hooked from MainActivity.onCreate; REQUEST_INSTALL_PACKAGES added to the app manifest) — the update channel is this repo's GitHub Releases. A release is installable only if it carries a release.json asset ({schema:1, builds:[{flavor, version_code, version_name, files:[{abi, name, sha256, version_code}]}]}); the app selects the newest build for its own flavor whose file for the device ABI carries a higher versionCode than the installed build, downloads the ABI-matching APK, verifies its SHA-256 against release.json and the GitHub asset digest, verifies the APK signer certificate against the pinned release certificate (SHA-256 512f0b7d…2082), then hands off to the system installer. Fails closed on any mismatch. Companion change in app/build.gradle.kts: a fork build offset (+1, bumped per release) is added to the base versionCode before the ABI digit so fork releases always exceed the previous fork build on the same upstream base.

7. Full UI rebrand (2026-10-13) — every user-visible "Element" string in the English resources is replaced (login incl. the Element Pro / Element Classic flows reworded neutrally, "Tesseract Call", "Tesseract Default/Fade" notification sounds, welcome title "Welcome to Tesseract", matrix.org examples re-pointed at chat.internal.blackcandletech.com); the appconfig production/desktop application names are "Tesseract"; the login and design-system logo drawables carry the candle flame artwork from the launcher foreground assets (appicon module); onboarding backgrounds are solid #08090B; the Compound accent semantics (bg/border/icon/text/badge accent groups in the generated SemanticColors files) are re-pointed from the green palette tokens to the orange tokens, and the orange ramps in the generated Light/Dark/HC token files are redefined as a Tesseract amber ramp anchored on #E8A33D (success semantics keep the green ramp); the onboarding gradient start color matches; legal-URL fallbacks in the preferences module point at tesseract.blackcandletech.com. Non-English locales still carry upstream strings in this pass.

8. Firebase push provider actually included (2026-10-13) \u2014 BuildTimeConfig.PUSH_CONFIG_INCLUDE_FIREBASE set to true. The flag is a compile-time module gate: app/build.gradle.kts attaches libraries:pushproviders:firebase as a gplayImplementation dependency only when it is true. The initial rebrand had set it false (read as a runtime toggle), which silently excluded the entire Firebase provider \u2014 including item 4\u2019s gateway patch \u2014 from every shipped build; 2026.10.1 registered no pusher because of it (verified: zero firebase classes in its dex). The fdroid flavor stays Firebase-free because the dependency is gplay-scoped. Release check: confirm the provider classes are present in the built APK\u2019s dex before staging.

9. Push actually reaches our Firebase project (2026-10-13) — three fixes. (a) The firebase module's resource files (src/main + src/release + src/debug res/values/firebase.xml) now carry the Black Candle Firebase project (bct-tesseract) instead of upstream's vector-alpha: the google-services Gradle plugin is commented out in app/build.gradle.kts, so these resources ARE the Firebase config and the tree's google-services.json is never consumed; 2026.10.2 initialized Firebase against Element's project and FCM rejected delivery with SENDER_ID_MISMATCH (proven in Sygnal's log). (b) BuildTimeConfig.PUSHER_APP_ID_RELEASE/DEBUG = com.blackcandle.tesseract (was null, i.e. the upstream default im.vector.app.android); the production Sygnal routes both app IDs as a belt-and-braces measure. (c) UnifiedPush is now an fdroidImplementation-only dependency: 2026.10.1 had persisted UnifiedPush as the current provider with no distributor installed, and the registration flow never falls through to Firebase once a provider is stored, so gplay installs could dead-end with no pusher at all. The self-updater also stamps its last-check time only after a completed scan and the interval drops from 6h to 1h (2026.10.2's offer was eaten by the old stamp-on-attempt 6h gate). Release checks: APK strings must show our sender ID and must NOT contain 912726360885; gplay dex must contain the firebase provider and no unifiedpush classes.

## Build (release APK, sideload)

Toolchain: JDK 21, Android SDK platform 37 + build-tools 37.0.0 (see the tesseract-android-build Docker image used by Black Candle).

    ./gradlew :app:assembleFdroidRelease

    ./gradlew :app:assembleGplayRelease   # push-capable (Firebase) build; needs app/google-services.json

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
