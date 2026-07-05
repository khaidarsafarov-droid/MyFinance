# Telegram Bot (Python) — поддержка файлов

Бот принимает текстовые сообщения и файлы, извлекает текст и передаёт его в общую логику обработки.

## Поддерживаемые форматы

| Тип        | Форматы                    | Библиотеки        |
|-----------|----------------------------|-------------------|
| PDF       | .pdf (текст + OCR для сканов) | PyMuPDF, pdfplumber, pytesseract |
| Документы | .docx                      | python-docx       |
| Таблицы   | .xlsx, .csv                | openpyxl, pandas  |
| Текст     | .txt                       | встроенно         |
| Изображения | .jpg, .png, .webp        | pytesseract, Pillow |

## Ограничения

- Максимальный размер файла: **20 МБ**
- При ошибке чтения или неподдерживаемом формате бот отправит сообщение об ошибке

## Установка

```bash
cd telegram_bot
pip install -r requirements.txt
```

Для OCR (PDF-сканы и изображения) установите Tesseract:

- **Windows:** https://github.com/UB-Mannheim/tesseract/wiki
- **macOS:** `brew install tesseract tesseract-lang`
- **Linux:** `sudo apt install tesseract-ocr tesseract-ocr-rus`

## Запуск

```bash
export TELEGRAM_BOT_TOKEN=your_bot_token
export CEREBRAS_API_KEY=your_cerebras_key   # первый приоритет
export GEMINI_API_KEY=your_gemini_key       # fallback
python bot.py
```

Или создайте файл `.env` (не коммитить) с `TELEGRAM_BOT_TOKEN=...` и загружайте его через `python-dotenv` при необходимости.

## AI: Bot + (Cerebras + Gemini) + App

Бот использует **Cerebras** (первым) и **Gemini** (fallback) для парсинга лоудов, зарплаты и дизеля. Единый AI-стек с приложением TruckerLoad.
