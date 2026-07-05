"""
Telegram bot with file support: PDF, DOCX, XLSX, CSV, TXT, images (OCR).
Extracted text is passed to the same processing logic as plain text messages.
"""
import asyncio
import io
import logging
import os
from typing import Optional

from telegram import Update
from telegram.ext import (
    Application,
    ContextTypes,
    MessageHandler,
    filters,
    CommandHandler,
)

from extractors.dispatch import get_file_type, extract_text
from ai_service import smart_reply

logging.basicConfig(
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    level=logging.INFO,
)
logger = logging.getLogger(__name__)

# Max file size 20 MB
MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024


def get_token() -> str:
    token = os.environ.get("TELEGRAM_BOT_TOKEN") or os.environ.get("BOT_TOKEN")
    if not token:
        raise RuntimeError("Set TELEGRAM_BOT_TOKEN or BOT_TOKEN environment variable")
    return token


async def download_file(context: ContextTypes.DEFAULT_TYPE, file_id: str) -> Optional[bytes]:
    """Download file by file_id and return bytes."""
    bot = context.bot
    file = await bot.get_file(file_id)
    buf = io.BytesIO()
    await file.download_to_memory(buf)
    return buf.getvalue()


def process_extracted_text(text: str, file_name: str) -> str:
    """
    Bot + (Cerebras + Gemini) + App — умная обработка через AI.
    """
    if not text or not text.strip():
        return "Текст из файла не извлечён или пуст."
    return smart_reply(text.strip(), file_name)


async def handle_document(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    if not update.message or not update.message.document:
        return
    doc = update.message.document
    file_name = doc.file_name or "document"
    mime_type = doc.mime_type
    file_id = doc.file_id

    if doc.file_size and doc.file_size > MAX_FILE_SIZE_BYTES:
        await update.message.reply_text(
            f"Файл слишком большой (макс. 20 МБ). Размер: {doc.file_size / (1024*1024):.1f} МБ."
        )
        return

    if get_file_type(file_name, mime_type) is None:
        await update.message.reply_text(
            f"Формат не поддерживается: {file_name}. Поддерживаются: PDF, DOCX, XLSX, CSV, TXT, JPG, PNG, WEBP."
        )
        return

    status_msg = await update.message.reply_text("Обрабатываю файл...")
    try:
        data = await download_file(context, file_id)
        if not data:
            await status_msg.edit_text("Не удалось скачать файл.")
            return
        text = extract_text(data, file_name, mime_type)
        reply = process_extracted_text(text, file_name)
        await status_msg.edit_text(reply)
    except ValueError as e:
        await status_msg.edit_text(f"Ошибка: {e}")
        logger.warning("Extract error: %s", e)
    except Exception as e:
        logger.exception("File handling failed")
        await status_msg.edit_text(f"Ошибка при обработке файла: {e!s}")


async def handle_photo(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    if not update.message or not update.message.photo:
        return
    # Largest photo size
    photos = update.message.photo
    photo = photos[-1]
    file_id = photo.file_id
    file_name = "photo.jpg"

    if photo.file_size and photo.file_size > MAX_FILE_SIZE_BYTES:
        await update.message.reply_text("Изображение слишком большое (макс. 20 МБ).")
        return

    status_msg = await update.message.reply_text("Обрабатываю изображение...")
    try:
        data = await download_file(context, file_id)
        if not data:
            await status_msg.edit_text("Не удалось скачать изображение.")
            return
        text = extract_text(data, file_name, "image/jpeg")
        reply = process_extracted_text(text, file_name)
        await status_msg.edit_text(reply)
    except ValueError as e:
        await status_msg.edit_text(f"Ошибка: {e}")
        logger.warning("Image extract error: %s", e)
    except Exception as e:
        logger.exception("Photo handling failed")
        await status_msg.edit_text(f"Ошибка при обработке изображения: {e!s}")


async def handle_text(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Plain text goes to the same logic (e.g. for Gemini)."""
    if not update.message or not update.message.text:
        return
    text = update.message.text.strip()
    reply = process_extracted_text(text, "(сообщение)")
    await update.message.reply_text(reply)


async def start_cmd(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    if update.message:
        await update.message.reply_text(
            "Отправьте текст или файл (PDF, DOCX, XLSX, CSV, TXT, JPG/PNG/WEBP). "
            "Максимум 20 МБ. Текст будет извлечён и обработан."
        )


def main() -> None:
    token = get_token()
    app = Application.builder().token(token).build()

    app.add_handler(CommandHandler("start", start_cmd))
    app.add_handler(MessageHandler(filters.Document.ALL, handle_document))
    app.add_handler(MessageHandler(filters.PHOTO, handle_photo))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_text))

    logger.info("Bot running...")
    app.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    main()
