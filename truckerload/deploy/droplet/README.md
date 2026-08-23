# Truck-log droplet

Production host `107.170.0.183` (`Truck-log`).

- Ktor: `https://107.170.0.183.sslip.io`

Caddy terminates TLS (Let’s Encrypt). Ktor listens on `127.0.0.1:8080`.

Android debug builds keep `SYNC_BACKEND_URL` blank (local-first).
To use this droplet, set `SYNC_BACKEND_URL=https://107.170.0.183.sslip.io` and
`LOCAL_ONLY_MODE=false`.
