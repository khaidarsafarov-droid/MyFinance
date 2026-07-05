"""
Dispatch by file type: mime_type / extension -> appropriate extractor.
"""
from __future__ import annotations

import io
from pathlib import Path
from typing import Optional, Tuple

from .pdf_extractor import extract_text_from_pdf
from .docx_extractor import extract_text_from_docx
from .xlsx_extractor import extract_text_from_xlsx, extract_text_from_csv
from .image_extractor import extract_text_from_image


# mime -> (extractor_name, supported)
SUPPORTED_MIME: dict[str, Tuple[str, bool]] = {
    "application/pdf": ("pdf", True),
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": ("docx", True),
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": ("xlsx", True),
    "application/vnd.ms-excel": ("xlsx", True),
    "text/csv": ("csv", True),
    "text/plain": ("txt", True),
    "image/jpeg": ("image", True),
    "image/jpg": ("image", True),
    "image/png": ("image", True),
    "image/webp": ("image", True),
}

EXT_TO_MIME: dict[str, str] = {
    ".pdf": "application/pdf",
    ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ".xls": "application/vnd.ms-excel",
    ".csv": "text/csv",
    ".txt": "text/plain",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
}


def get_file_type(file_name: str, mime_type: Optional[str]) -> Optional[str]:
    """Return internal type: pdf, docx, xlsx, csv, txt, image or None if unsupported."""
    mime = (mime_type or "").strip().lower()
    if mime and mime in SUPPORTED_MIME:
        return SUPPORTED_MIME[mime][0]

    ext = (Path(file_name).suffix or "").lower()
    if ext in EXT_TO_MIME:
        mime = EXT_TO_MIME[ext]
        if mime in SUPPORTED_MIME:
            return SUPPORTED_MIME[mime][0]

    return None


def extract_text(
    data: bytes,
    file_name: str,
    mime_type: Optional[str] = None,
) -> str:
    """
    Extract text from file. Raises ValueError for unsupported format or on extract error.
    """
    file_type = get_file_type(file_name, mime_type)
    if not file_type:
        raise ValueError(f"Формат не поддерживается: {file_name} ({mime_type or 'unknown'})")

    try:
        if file_type == "pdf":
            return extract_text_from_pdf(data)
        if file_type == "docx":
            return extract_text_from_docx(data)
        if file_type == "xlsx":
            return extract_text_from_xlsx(data)
        if file_type == "csv":
            return extract_text_from_csv(data)
        if file_type == "txt":
            return data.decode("utf-8", errors="replace").strip()
        if file_type == "image":
            return extract_text_from_image(data, mime_type)
    except Exception as e:
        raise ValueError(f"Ошибка при чтении файла: {e!s}") from e

    raise ValueError(f"Неизвестный тип: {file_type}")
