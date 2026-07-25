# TruckerLoad

The product source is in [`truckerload/`](truckerload/): a native Android app,
shared Kotlin contracts, and its Ktor cloud API. It is local-first and can run without
the backend; configuring an HTTPS backend URL enables PostgreSQL-backed account
snapshots, server Telegram ingestion, FCM wake-ups, and S3-compatible media APIs.

Start with:

- [project README](truckerload/README.md)
- [target architecture](truckerload/docs/TARGET_ARCHITECTURE.md)
- [backend setup](truckerload/docs/BACKEND_SETUP.md)
- [migration rollout](truckerload/docs/MIGRATION_ROLLOUT.md)
- [DigitalOcean deployment](truckerload/deploy/digitalocean/DEPLOYMENT.md)

From `truckerload/`, verify the repository with:

```bash
sh ./gradlew :backend:server:test :app:testDebugUnitTest :app:assembleDebug
```
