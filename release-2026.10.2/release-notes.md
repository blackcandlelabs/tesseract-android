# Tesseract 2026.10.2

The full rebrand release. If you installed 2026.10.1, this update arrives in the app itself — that's the self-updater working end to end.

**Base:** Element X v26.09.4 (fork `tesseract` branch @ 7a7fc5e9e7), gplay flavor, versionName 26.09.4.

## What's in it

- **Every Element string is gone from the English UI.** Login and onboarding ("Welcome to Tesseract", candle logo), the previous-app key-backup flow reworded neutrally, "Tesseract Call", "Tesseract Default / Tesseract Fade" notification sounds, company-server examples throughout.
- **Amber skin.** The app's accent color system (buttons, highlights, badges, links) now runs on a Tesseract amber ramp anchored on #E8A33D, in light, dark, and high-contrast themes. Success/confirmation states keep their green. Onboarding backgrounds and the candle mark complete the look.
- Legal links point at the canonical Black Candle pages (tesseract.blackcandletech.com/terms and /privacy).

Non-English locales still carry upstream strings in this pass; the founders' en-US experience is fully rebranded.

## Install

From 2026.10.1: open the app and accept the update prompt (download is SHA-256- and signer-verified before the installer opens). Fresh installs: download the arm64-v8a APK (Galaxy S26 Ultra, Pixel 8 Pro) or universal, and install over any existing Tesseract build — same signing key, data preserved.
