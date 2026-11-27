# TruckoRig 1.6.3 — Play Store release notes (what’s new)

## User-facing (EN)

• Fixed a crash when opening the Maintenance screen.
• Fixed a crash when opening the document scanner.

## Internal

- `versionCode` 23, `versionName` 1.6.3 (1.6.2 was `versionCode` 22).
- R8 keep rule for `ComponentRegistrar` constructors (ML Kit text recognition), PR #510.
- `OCRService` creates the ML Kit recognizer lazily and falls back to Tesseract.
- Debug build uses applicationId suffix `.debug`, PR #509.
