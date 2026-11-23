# Share sheet import

TruckoRig appears in the Android system share sheet. A share is kept only when on-device
scoring says it is a load (rate confirmation, load tender, or dispatch). Anything else is
not saved.

Processing stays on the phone. Text comes from the share itself, from the PDF text layer
(`PdfTextLayerExtractor`), or from the existing on-device OCR path (`LoadDocumentTextExtractor`
→ ML Kit Latin, then Tesseract when the Latin result is empty or Cyrillic). Nothing in this
flow is uploaded.

## Why a separate activity

The official receiving guide is
[Receive data from other apps](https://developer.android.com/develop/ui/compose/sharing/receive).
`MainActivity` is the launcher task (`singleTop`, deep links, the Loads journal). Putting
`ACTION_SEND` on it would mix share extras with those deep links and would leave a second
recents entry while the driver is still in Gmail or WhatsApp.

`ShareReceiverActivity` is the share target instead:

- `exported=true` with `CATEGORY_DEFAULT`, which `startActivity` always adds.
- One filter each for `ACTION_SEND` and `ACTION_SEND_MULTIPLE`, and for `text/plain`,
  `application/pdf`, and `image/*`. A catch-all `*/*` is intentionally absent, so videos
  and random files do not list TruckoRig.
- `launchMode=singleTask` and `taskAffinity=""` so a second share reuses this activity
  instead of stacking, and the share task is not the app's normal task.
- `excludeFromRecents=true` so the checker does not sit in Recents.
- `configChanges` includes orientation so a rotate does not run the import twice.
  `onCreate` also ignores a restored instance: the URI grant does not survive process death.

On a real load the receiver starts `MainActivity` on the Loads journal (`Routes.HOME`,
the tab labeled Loads) and finishes. On a reject it stays up only long enough to show
the message, then `finish()` returns to the app that shared.

## What the intent can contain

Senders are inconsistent. The reader checks all of these:

| Payload | Where it lives |
| --- | --- |
| Plain text | `EXTRA_TEXT`, else the first `ClipData` item that has text |
| One file | `ACTION_SEND` + `EXTRA_STREAM` |
| Several files | `ACTION_SEND_MULTIPLE` + `EXTRA_STREAM` as an `ArrayList<Uri>` |
| URI grant | `FLAG_GRANT_READ_URI_PERMISSION` on the intent, and the same URIs repeated in `ClipData` (the grant follows `ClipData`) |
| Mail subject | `EXTRA_SUBJECT`, prefixed as `Subject: …` because the subject is often "Rate confirmation" |

Only `content://` URIs are opened. `file://` and `http(s)://` are dropped.

The read grant is temporary and usually dies with the receiving activity. The bytes are
copied into `cache/share-inbox/` before classification. The cache copy is deleted after
the original is moved into the scans folder or the share is rejected. Persistable URI
permission is not requested; most senders do not offer it.

## Limits and checks

- At most 8 files, 20 MB each, 40 MB per share. Larger input is refused and nothing is saved.
- Shared text is capped at 500 KB.
- The type on the intent or from `ContentResolver.getType` must be `text/plain`,
  `application/pdf`, or `image/*`.
- The first bytes must agree with that type (`%PDF`, JPEG, PNG, GIF, WEBP, HEIC, or
  text with no NUL). A renamed executable is not parsed as a PDF.
- The stored file name is generated (`AttachmentNaming`). The sender's name is only a
  sanitized display label: no path separators, no `..`.
- `FileProvider` stays `exported=false`. The receiver does not re-export the shared URI.

## Load or not

`ShareLoadClassifier` scores the text. Points come from keyword groups (rate confirmation,
load tender, dispatch, load # / PO / PRO / BOL / trip id, shipper, consignee, pickup,
delivery, broker, MC#, carrier, and the Russian equivalents), a rate of at least $100,
pickup and delivery (City, ST / street / ZIP), a date, a load number, and miles.
Fuel receipts, store receipts, and settlements add a negative score.

A share is a load only when keyword hits, points, and at least three signal groups clear
the bar (or a rate plus both places, or both places plus several load keywords). Below
that, the result is reject: no row, no file.

`ShareImportDecider` then reuses `MessageParseService` (Relay, delimited, broker templates,
`RateConfirmationLoadParser`, `FlexibleLoadParser`). High confidence with holes still
saves a load. Missing rate, pickup, delivery, miles, date, or load number is listed in
the Loads snackbar (`Load added. Fill in …`) with Undo. Low confidence never saves.

Duplicates, checked before insert:

- SHA-256 of the copied bytes (or of the text when there is no file).
- Same load number and broker. Both brokers blank counts only when both sides are blank.
  A missing broker does not match a different broker.
- An existing trip id already in Room (loads saved before this feature).

The journal has no broker column. The broker is kept only in the share index
(`truckerload_share_import`, per profile) for this check. The original text remains on
the load as `rawMessage`, and the file is a scan row (`ScanRepository`) linked to the load.

Several files with the same load number become one load and several attachments. Different
load numbers become separate loads. If each page is too thin but the pages together score
as one load, they are saved once.

Undo deletes that load (and its scans) and clears the index entry so the same share can
be imported again.

## Direct Share

Optional, and included. `shortcuts.xml` declares a `share-target` for the same three MIME
types and the category `com.truckorig.category.SHARE_LOAD`. After unlock, the app publishes
one dynamic shortcut in that category so the share sheet can pin TruckoRig as a direct
target. The four launcher shortcuts are unchanged.

## Not in this flow

- `text/html`, Word, and `*/*` mixed shares do not show TruckoRig. Copy the text or send
  the PDF or the photos.
- OCR can miss a faded scan. The driver can still add the load by paste inside the app.
- Sharing while signed out (and not in local-only mode) does not save. The screen asks
  the driver to sign in and share again. The temporary grant is not held across login.
- Opening Loads after a save switches the journal filter to All so a load dated outside
  the current week is actually on screen.
