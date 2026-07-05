"""
AI service: Cerebras (first) + Gemini (fallback).
Bot + (Cerebras + Gemini) + App — единый AI-стек для парсинга и чата.
"""
import json
import logging
import os
from typing import Optional

import requests

logger = logging.getLogger(__name__)

CEREBRAS_URL = "https://api.cerebras.ai/v1/chat/completions"
CEREBRAS_MODEL = "llama3.1-8b"
GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"


def _cerebras_complete(prompt: str, system: str) -> Optional[str]:
    """Cerebras API — быстрый ответ. При ошибке возвращает None."""
    key = os.environ.get("CEREBRAS_API_KEY")
    if not key:
        return None
    try:
        r = requests.post(
            CEREBRAS_URL,
            headers={
                "Authorization": f"Bearer {key}",
                "Content-Type": "application/json",
            },
            json={
                "model": CEREBRAS_MODEL,
                "messages": [
                    {"role": "system", "content": system},
                    {"role": "user", "content": prompt},
                ],
                "temperature": 0.2,
                "max_tokens": 1024,
            },
            timeout=30,
        )
        if r.status_code not in range(200, 300):
            logger.warning("Cerebras error %s: %s", r.status_code, r.text[:200])
            return None
        data = r.json()
        content = (
            data.get("choices", [{}])[0]
            .get("message", {})
            .get("content", "")
            .strip()
        )
        return content if content else None
    except Exception as e:
        logger.warning("Cerebras request failed: %s", e)
        return None


def _gemini_complete(prompt: str, system: str) -> Optional[str]:
    """Gemini API — fallback при недоступности Cerebras."""
    key = os.environ.get("GEMINI_API_KEY")
    if not key:
        return None
    url = f"{GEMINI_URL}?key={key}"
    full_text = f"{system}\n\n---\n\n{prompt}"
    try:
        r = requests.post(
            url,
            headers={"Content-Type": "application/json"},
            json={
                "contents": [{"parts": [{"text": full_text}]}],
                "generationConfig": {"temperature": 0.2, "maxOutputTokens": 1024},
            },
            timeout=60,
        )
        if r.status_code not in range(200, 300):
            logger.warning("Gemini error %s: %s", r.status_code, r.text[:200])
            return None
        data = r.json()
        parts = (
            data.get("candidates", [{}])[0]
            .get("content", {})
            .get("parts", [])
        )
        text = (parts[0].get("text", "") if parts else "").strip()
        return text if text else None
    except Exception as e:
        logger.warning("Gemini request failed: %s", e)
        return None


def ai_complete(prompt: str, system: str) -> Optional[str]:
    """
    Cerebras first, Gemini fallback.
    Bot + (Cerebras + Gemini) + App
    """
    result = _cerebras_complete(prompt, system)
    if result:
        return result
    result = _gemini_complete(prompt, system)
    return result


# Промпты для парсинга (синхрон с App)
PARSE_LOADS_SYSTEM = """
Роль: Ты — системный интегратор между мессенджером и базой данных приложения TruckerLoad.
Триггер: Как только получено текстовое сообщение (СМС/Лоуд), извлеки все грузы для синхронизации.
Верни ТОЛЬКО валидный JSON (без markdown):
{ "loads": [ { "tripId": "string", "date": "YYYY-MM-DD", "totalRate": number, "totalMiles": number, "pointA": "string", "pointB": "string", "puCount": number, "delCount": number, "stops": [], "penalties": [] } ] }
Нет валидных грузов: { "loads": [] }.
"""

PARSE_PAYCHECK_SYSTEM = """
Задача: проанализируй текст (Driver Settlement / платёжка) и извлеки данные для раздела "зарплата".
netAmount = ТОЛЬКО из поля "Зарплата" или "Grand Total".
Верни JSON: { "driverName": string|null, "weekStartDate": "YYYY-MM-DD"|null, "weekEndDate": "YYYY-MM-DD"|null, "grossAmount": number|null, "netAmount": number, "currency": "USD", "confidence": "high"|"medium"|"low" }
"""

PARSE_DIESEL_SYSTEM = """
Извлеки из текста/чека за дизель данные. Верни ТОЛЬКО JSON:
{ "date": "YYYY-MM-DD"|null, "totalAmount": number, "gallons": number|null, "pricePerGallon": number|null, "location": string|null, "vendor": string|null, "currency": "USD", "confidence": "high"|"medium"|"low" }
totalAmount — итоговая сумма за топливо.
"""


def _extract_json(text: str) -> Optional[dict]:
    """Извлекает JSON из ответа (убирает markdown)."""
    cleaned = text.replace("```json", "").replace("```", "").strip()
    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        return None


def parse_loads(text: str) -> tuple[list[dict], Optional[str]]:
    """Парсит грузы. Возвращает (список loads, сообщение об ошибке)."""
    out = ai_complete(text, PARSE_LOADS_SYSTEM)
    if not out:
        return [], "AI недоступен. Добавьте GEMINI_API_KEY или CEREBRAS_API_KEY."
    data = _extract_json(out)
    if not data:
        return [], None
    loads = data.get("loads") or []
    if isinstance(loads, list):
        return loads, None
    return [], None


def parse_paycheck(text: str) -> Optional[dict]:
    """Парсит зарплату из текста."""
    out = ai_complete(text, PARSE_PAYCHECK_SYSTEM)
    if not out:
        return None
    return _extract_json(out)


def parse_diesel(text: str) -> Optional[dict]:
    """Парсит дизель из текста."""
    out = ai_complete(text, PARSE_DIESEL_SYSTEM)
    if not out:
        return None
    return _extract_json(out)


def smart_reply(text: str, file_name: str = "(сообщение)") -> str:
    """
    Умный ответ: парсит loads/paycheck/diesel через Cerebras+Gemini.
    Bot + (Cerebras + Gemini) + App
    """
    loads, err = parse_loads(text)
    if err:
        # AI недоступен — fallback
        lines = [s for s in text.splitlines() if s.strip()]
        preview = "\n".join(lines[:30]) if len(lines) > 30 else text.strip()
        if len(lines) > 30:
            preview += f"\n\n... (всего строк: {len(lines)})"
        return f"Извлечённый текст из «{file_name}»:\n\n{preview}\n\n{err}"
    if loads:
        items = []
        for L in loads[:5]:
            trip = L.get("tripId", "?")
            rate = L.get("totalRate", 0)
            a, b = L.get("pointA", ""), L.get("pointB", "")
            items.append(f"• {trip}: ${rate:.2f} | {a} → {b}")
        tail = f"\n... и ещё {len(loads) - 5}" if len(loads) > 5 else ""
        return f"✅ Распознано {len(loads)} груз(ов):\n" + "\n".join(items) + tail

    pc = parse_paycheck(text)
    if pc and (pc.get("netAmount") or 0) > 0:
        amt = pc.get("netAmount", 0)
        return f"📥 Зарплата: ${amt:,.2f} (неделя {pc.get('weekStartDate', '?')})"

    d = parse_diesel(text)
    if d and (d.get("totalAmount") or 0) > 0:
        amt = d.get("totalAmount", 0)
        return f"⛽ Дизель: ${amt:,.2f}"

    # Не распознано — краткий превью
    lines = [s for s in text.splitlines() if s.strip()]
    preview = "\n".join(lines[:15]) if len(lines) > 15 else text.strip()
    if len(lines) > 15:
        preview += f"\n... ({len(lines)} строк)"
    return f"Текст из «{file_name}»:\n\n{preview}\n\n❓ Не распознано как лоуд, зарплата или дизель."
