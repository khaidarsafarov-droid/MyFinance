# LiveKit SFU

## Local (dev)

Dev mode uses the documented placeholder pair:

- API key: `devkey`
- API secret: `secret`

Do not use these keys on a public IP.

```bash
# Docker (from truckerload/)
docker compose up livekit -d

# Or without Docker:
sh ./scripts/run-livekit.sh
```

Signal/WebSocket: `ws://localhost:7880`

## Production (DigitalOcean Droplet `Truck-log`)

LiveKit **v1.13.5** runs as systemd `livekit` on Ubuntu 24.04.

| | |
| --- | --- |
| Public IPv4 | `107.170.0.183` |
| Private IPv4 | `10.100.0.3` |
| Public IPv6 | `2604:a880:0:202a:0:1:5979:9000` |
| Client URL | `ws://107.170.0.183:7880` |
| Health | `http://107.170.0.183:7880/` → `OK` |
| Ports | TCP 7880 (signal), TCP 7881 (ICE), UDP 7882 (ICE mux) |

Production API keys are **only** on the droplet (`/etc/livekit/livekit.yaml`) and
must be copied into Ktor as `LIVEKIT_URL` / `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET`.
They are not in git. Open the DigitalOcean web console if you need to read them.

```
LIVEKIT_URL=ws://107.170.0.183:7880
```

The Android app never embeds these values; it receives `url` + `token` from
`POST /v1/voice/token`.

Config template: `livekit.prod.yaml`.
