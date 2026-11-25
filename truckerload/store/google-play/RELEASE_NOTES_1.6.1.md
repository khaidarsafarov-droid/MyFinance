# TruckoRig 1.6.1 — Play Store release notes (what’s new)

## User-facing (EN)

• Statements and reports now use a new PDF engine, so exports are more reliable
• Better screen-reader support, larger touch targets, and text that wraps at big font sizes
• Easier on your battery: background work stops sooner
• Faster start: the app shows its first screen before it finishes setup tasks
• Importing loads from text files and Telegram history is more accurate

## Internal

- `versionCode` 19, `versionName` 1.6.1 (Play has 18 from 1.6.0)
- PDF generation moved from iText to PdfBox-Android (Apache 2.0). PDF statements stay English-only.
- Photo GPS, date, and time stamps are unchanged.
