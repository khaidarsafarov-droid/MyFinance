# TruckoRig 1.5.10 — Play Store release notes (what’s new)

## User-facing (EN)

• Telegram import without a persistent notification  
• Loads still arrive while you use the app; background catch-up every ~15 minutes

## User-facing (RU)

• Импорт из Telegram без постоянного уведомления  
• Пока приложение открыто — сразу; в фоне — раз в ~15 минут

## Internal

- Removed `FOREGROUND_SERVICE_DATA_SYNC` / Telegram FGS
- In-process poller + existing `TelegramSyncWorker`
- `versionCode` 15
