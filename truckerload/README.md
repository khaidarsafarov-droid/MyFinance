# Truck Log (Android)

## Document scanner (ML Kit)

- Dependency: `play-services-mlkit-document-scanner:16.0.0-beta1`
- Requires **Google Play Services** on a **physical device**
- Fallback: **Scan with camera** opens the geotagged camera flow when GMS is unavailable
- Up to 20 pages per scan session

## OCR

- **ML Kit Latin** + **Tesseract (`rus+eng`)** hybrid via `OCRService` / `HybridOCRService`
- `LanguageDetector` identifies Russian, English, or mixed text
- First Tesseract use downloads trained data (~4 MB per language)

## Photo gallery

- Grid view with filters (all / today / week / by load)
- Full-screen detail, share, delete, link to load
- Linked photos appear on load detail screen

## Navigation (phone)

```
[Journal] [Goal] [Analytics]  [📸] [📄]  [Settings]
```

## Testing checklist

- Camera: geotag watermark, save, share, gallery, retake
- Scanner: GMS check, ML Kit scan, OCR (Latin + Cyrillic), PDF save/share, scan gallery
- Photos: link to load, view from load detail
- Analytics: bottom nav tab on phone
