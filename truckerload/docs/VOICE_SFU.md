# Групповой голосовой чат (LiveKit SFU)

Одно native Android-приложение (`truckerload/`, Kotlin + Compose). Продуктовые имена
Truck Passe / Truck Logi — тот же модуль `:app`. Отдельного Flutter / React Native
клиента нет.

## Почему LiveKit, а не mesh и не Agora

Текущие аудиогруппы после mesh-PR держат **по одному PeerConnection на каждого
соседа**. Uplink растёт как O(N) и на трассе легко выходит за 15–25 kbps.

| Подход | Uplink спикера | Секрет в APK | Self-host |
| --- | --- | --- | --- |
| Mesh (сейчас, запасной путь) | N−1 копии Opus | нет | не нужен SFU |
| **LiveKit SFU** | **один** поток 12–20 kbps | нет (токен с Ktor) | свой сервер (OSS) |
| Agora Voice SDK | один поток | App ID / сертификат | нет (их облако) |

Ktor depends on `io.livekit:livekit-server` (`AccessToken` in
`LiveKitAccessToken`). The API secret stays in `LIVEKIT_API_SECRET`.

Mesh остаётся fallback, если `SYNC_BACKEND_URL` пустой, `LOCAL_ONLY_MODE=true`,
или `POST /v1/voice/token` отвечает 503 (LiveKit не настроен).

## Бюджет трафика

Клиент ограничивает publish **12 / 16 / 20 kbps** (`VoiceAudioBudget`) — Opus speech,
DTX, in-band FEC. Это ≤150 КБ/мин исходящего у спикера (< 1 МБ/мин). Слушатель не
публикует микрофон (`canPublish=false`), uplink — только сигналинг.

AEC / ANS / AGC / high-pass включены в `LocalAudioTrackOptions` (LiveKit) и в
`MediaConstraints` (mesh).

## Пошаговое внедрение

1. Поднять **свой** LiveKit OSS рядом с Ktor (`LIVEKIT_URL=wss://…`).
   Облачные подписки (Agora / Zego / LiveKit Cloud) не используются.
   Локально: `docker compose up livekit` или `sh ./scripts/run-livekit.sh`
   (`ws://localhost:7880`, ключи `--dev`: `devkey` / `secret`).
2. Выдать API Key + Secret, положить **только** в env бэкенда:
   `LIVEKIT_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`.
3. Задеплоить Ktor. Проверка: авторизованный
   `POST /v1/voice/token` с `{"roomId":"…","displayName":"…","role":"speaker"}`
   возвращает `{url, token, roomName, identity, role, audioBitrate}`.
4. В `local.properties` клиента: `SYNC_BACKEND_URL=https://api…` и
   `LOCAL_ONLY_MODE=false`. URL LiveKit в APK не кладётся.
5. Открыть голосовую комнату. Строка битрейта показывает **SFU**. Toggle
   Спикер / Слушатель перевыпускает токен (`canPublish`).
6. Если токен недоступен, комната тихо идёт в mesh (**P2P** на том же экране).

## Готовые контракты

Клиент:

```http
POST /v1/voice/token
Authorization: Bearer <Supabase JWT>
Content-Type: application/json

{"roomId":"<uuid>","displayName":"Alex","role":"speaker"}
```

`role`: `speaker` | `listener`. Секрет LiveKit на устройство не попадает.

Подключение комнаты (упрощённо, см. `LiveKitVoiceSession`):

```kotlin
val room = LiveKit.create(
    appContext,
    RoomOptions(
        audioTrackCaptureDefaults = LocalAudioTrackOptions(
            noiseSuppression = true,
            echoCancellation = true,
            autoGainControl = true,
            highPassFilter = true,
        ),
        audioTrackPublishDefaults = AudioTrackPublishDefaults(
            audioBitrate = 16_000,
            dtx = true,
            red = true,
        ),
    ),
)
room.connect(response.url, response.token)
room.localParticipant.setMicrophoneEnabled(role == VoiceRoomRole.SPEAKER && !muted)
```

`joinRoom` / `leaveRoom` / mute / role — `VoiceRepository`. Presence по-прежнему
Supabase (`CommunityVoiceRemote`); медиа — LiveKit или mesh.

## Экран комнаты

- Кольцо вокруг аватара = говорит (LiveKit `ActiveSpeakersChanged` или WebRTC stats).
- **Спикер / Слушатель** над кнопками mute / deafen / выход.
- Слушатель: микрофон выключен, исходящего Opus нет.

## 1:1 звонки (P2P WebRTC)

`WebRtcCallManager` + `IceServers`: бесплатные STUN (`stun.l.google.com`,
Cloudflare). SDP Opus режется `OpusAudioTune` (DTX, FEC, maxaveragebitrate ≤20 kbps).
AEC/ANS/AGC/high-pass — `MediaConstraints`. Сигналинг — существующая таблица
`community_voice_signals` (опрос REST); медиа LiveKit идёт по WebSocket к вашему SFU.

Микрофон: `rememberMicPermission()` на экранах комнаты и звонка
(`RECORD_AUDIO` в манифесте). iOS-клиента в этом репозитории нет (KMP roadmap).

## Env

Прод SFU (Droplet `Truck-log`): `LIVEKIT_URL=wss://lk.107.170.0.183.sslip.io`.
Ktor: `https://107.170.0.183.sslip.io`.
Ключи только в `/etc/livekit/livekit.yaml` на машине и в env Ktor, не в APK и не в git.

```
LIVEKIT_URL=wss://lk.107.170.0.183.sslip.io
LIVEKIT_API_KEY=<from /etc/livekit/livekit.yaml>
LIVEKIT_API_SECRET=<from /etc/livekit/livekit.yaml>
SYNC_BACKEND_URL=https://107.170.0.183.sslip.io
LOCAL_ONLY_MODE=false
```

Локально: `ws://localhost:7880` + `devkey` / `secret` (только `--dev`).
Self-host: https://docs.livekit.io/home/self-hosting/local/
