"""
Image text extraction via OCR (pytesseract + Pillow).
Supports: jpg, png, webp.
"""
from __future__ import annotations

import io
from typing import Optional

try:
    import pytesseract
    from PIL import Image
except ImportError:
    pytesseract = None
    Image = None


def extract_text_from_image(data: bytes, mime_type: Optional[str] = None) -> str:
    """OCR text from image (jpg, png, webp)."""
    if Image is None or pytesseract is None:
        raise RuntimeError("Install pytesseract and Pillow: pip install pytesseract Pillow")

    img = Image.open(io.BytesIO(data))
    # Convert to RGB if necessary (e.g. RGBA, P)
    if img.mode not in ("RGB", "L"):
        img = img.convert("RGB")
    text = pytesseract.image_to_string(img, lang="rus+eng")
    return (text or "").strip()
