# Tesseract 2026.10.1

First GitHub release of Tesseract for Android — and the build that turns on self-updating.

**Base:** Element X v26.09.4 (fork `tesseract` branch @ 1d7cf6a9b0), gplay flavor, versionName 26.09.4.

## What's in it

- **Login fixed.** The app now registers with the homeserver using Tesseract's own OAuth identity (the previous build presented upstream Element identity, which the server's registration policy rejects). Sign-in with Black Candle completes normally.
- **Push notifications.** Firebase Cloud Messaging via Black Candle's own push gateway (Sygnal at chat.internal.blackcandletech.com), Firebase project `bct-tesseract`.
- **Self-updater (new).** On launch, the app checks this repository's GitHub releases. A release is installable only if it carries a `release.json` asset; the app picks the newest build for its own flavor with a higher version code, downloads the APK for the device's ABI, and verifies it *before* prompting:
  - SHA-256 of the download must match `release.json` and the digest GitHub reports for the asset;
  - the APK's signing certificate must match the pinned Tesseract release certificate (SHA-256 `512f0b7d9da8b114fa27c42585ad96d81322afdd5efe1db898816e08d2332082`).
  
  Any mismatch discards the download. Installation is handed to the Android package installer (the app will ask for the "install unknown apps" permission the first time).

## Install (first time is manual — this build introduces the updater)

1. Download the APK for your device: `arm64-v8a` for the Galaxy S26 Ultra and Pixel 8 Pro; `universal` works anywhere.
2. Install it over the existing Tesseract app. Same signing key — your data and login are preserved.
3. Every later release arrives in the app itself.

## Assets

Five ABI splits + `release.json` (the updater contract). Per-file version codes and SHA-256 digests are in `release.json` and in the release's asset digests.
