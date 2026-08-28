# TruckoRig 1.5.7 — Play Store release notes (what’s new)

## User-facing (EN)

• More reliable load import and duplicate handling  
• Safer cloud sync (no accidental wipe of offline loads)  
• Share the app from Settings with one tap  
• Stability and journal fixes

## User-facing (RU)

• Надёжнее импорт лоудов и проверка дубликатов  
• Безопаснее облачная синхронизация (offline-лоуды не пропадают)  
• «Поделиться приложением» в Настройках  
• Исправления стабильности журнала

## Internal (do not paste verbatim to Play)

- Audit fixes: orphan-safe pull, initial merge, fuzzy duplicate match, session graph for Telegram workers  
- CDC upsert, LWW tie → remote, server inbox cursor-before-ack  
- Removed share-app body copy; English SoftUI marketing screenshots under `docs/play-store-screenshots-en/`
