# Truck-log droplet

Production host `107.170.0.183` (`Truck-log`).

- Ktor: `https://107.170.0.183.sslip.io`
- LiveKit signal: `wss://lk.107.170.0.183.sslip.io`
- ICE: UDP 7882 / TCP 7881 on the public IPv4

Caddy terminates TLS (Let’s Encrypt). Ktor listens on `127.0.0.1:8080`.
LiveKit listens on `127.0.0.1`/public `7880` and is proxied by Caddy.

Android debug/friends builds keep `SYNC_BACKEND_URL` blank (local-first).
To use this droplet, set `SYNC_BACKEND_URL=https://107.170.0.183.sslip.io` and
`LOCAL_ONLY_MODE=false`. Google ID tokens are accepted by `/v1/voice/token`.
