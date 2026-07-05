# TruckerLoad Backend

Node.js + TypeScript + Express backend: Telegram bot (Telegraf), Gemini API, FCM push, PostgreSQL (Prisma), Redis/Bull.

## Setup

```bash
cp .env.example .env
# Edit .env: DATABASE_URL, BOT_TOKEN, GEMINI_API_KEY, SERVER_URL, REDIS_URL, FIREBASE_* (optional)

npm install
npx prisma generate
npx prisma migrate dev
npm run build
npm start
```

## Env

- `PORT` — server port (default 3000)
- `SERVER_URL` — public URL for webhook (e.g. https://your-server.com)
- `DATABASE_URL` — PostgreSQL connection string
- `BOT_TOKEN` — Telegram Bot API token
- `GEMINI_API_KEY` — Google Gemini API key
- `REDIS_URL` — Redis for Bull queue (default redis://localhost:6379)
- `FIREBASE_*` — optional, for FCM push

## API

- `GET/POST/PATCH/DELETE /api/loads` — loads
- `GET /api/loads/:id` — load by id (tripId)
- `GET/POST/PATCH/DELETE /api/paychecks`, `/api/diesel`
- `GET /api/weeks/:year/:week` — week summary
- `POST /api/devices/register` — body `{ "fcmToken": "..." }`
- `GET /api/analyze/history/:jobId` — analysis job status
- `POST /api/analyze/history` — body `{ "chatId": "..." }` — start history analysis job

## Bot

- **Webhook**: `POST /webhook` (set via `SERVER_URL` + `/webhook`)
- **Commands**: `/start`, `/analyze` (enqueue history analysis job)
- **Text**: load-like messages → Gemini parse → save load → FCM push
- **Document/Photo**: classify (paycheck/diesel) → Gemini parse → save → FCM push

## Deploy

```bash
npm run build
pm2 start dist/index.js --name truckerload-backend
```
