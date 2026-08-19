# LiveKit SFU (local / compose)

Dev mode uses the documented placeholder pair:

- API key: `devkey`
- API secret: `secret`

Do not use these keys in production.

```bash
# Docker (from truckerload/)
docker compose up livekit -d

# Or without Docker:
sh ./scripts/run-livekit.sh
```

Signal/WebSocket: `ws://localhost:7880`  
Point Ktor at the same URL via `LIVEKIT_URL` / `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET`.
The Android app receives that URL from `POST /v1/voice/token`.
