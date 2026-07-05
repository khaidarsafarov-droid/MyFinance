"""
PDF text extraction: text-based PDFs and scanned PDFs (OCR via pytesseract).
"""
from __future__ import annotations

import io
from typing import Optional

try:
    import fitz  # PyMuPDF
except ImportError:
    fitz = None

try:
    import pdfplumber
except ImportError:
    pdfplumber = None

try:
    import pytesseract
    from PIL import Image
except ImportError:
    pytesseract = None
    Image = None


def extract_text_from_pdf(data: bytes, use_ocr_for_scans: bool = True) -> str:
    """
    Extract text from PDF page by page. For text PDFs use PyMuPDF/pdfplumber;
    for scanned pages use OCR (pytesseract) on rendered page images.
    """
    if not data:
        return ""

    # Prefer PyMuPDF for rendering and text
    if fitz is not None:
        return _extract_with_pymupdf(data, use_ocr_for_scans)

    if pdfplumber is not None:
        return _extract_with_pdfplumber(data)

    raise RuntimeError("Install PyMuPDF or pdfplumber: pip install PyMuPDF pdfplumber")


def _extract_with_pymupdf(data: bytes, use_ocr: bool) -> str:
    doc = fitz.open(stream=data, filetype="pdf")
    parts: list[str] = []
    try:
        for page_num in range(len(doc)):
            page = doc[page_num]
            text = page.get_text().strip()
            if use_ocr and (not text or len(text) < 50) and pytesseract is not None and Image is not None:
                # Likely scanned: render page to image and OCR
                pix = page.get_pixmap(dpi=150, alpha=False)
                img = Image.open(io.BytesIO(pix.tobytes("png")))
                text = pytesseract.image_to_string(img, lang="rus+eng").strip()
            if text:
                parts.append(text)
    finally:
        doc.close()
    return "\n\n".join(parts) if parts else ""


def _extract_with_pdfplumber(data: bytes) -> str:
    parts: list[str] = []
    with pdfplumber.open(io.BytesIO(data)) as pdf:
        for page in pdf.pages:
            text = page.extract_text()
            if text and text.strip():
                parts.append(text.strip())
    return "\n\n".join(parts) if parts else ""
