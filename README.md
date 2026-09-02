# TruckoRig

The product source is in [`truckerload/`](truckerload/): a native Android
trucking journal. It is local-first: Room, an on-device Telegram bot, and
optional Google Drive backup. There is no product backend or Supabase service.

Start with:

- [project README](truckerload/README.md)
- [target architecture](truckerload/docs/TARGET_ARCHITECTURE.md)
- [Google Sign-In](truckerload/docs/AUTH_GOOGLE.md)

From `truckerload/`, verify the repository with:

```bash
sh ./gradlew :app:testDebugUnitTest :app:assembleDebug
```
