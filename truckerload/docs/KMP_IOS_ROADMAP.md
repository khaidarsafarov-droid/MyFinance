# KMP → iOS roadmap

Подготовка почвы, не порт iPhone. Android остаётся основным клиентом. Sign in with
Apple и публикация в App Store — **последний этап**, когда будет Apple Developer
Program ($99/год) и Mac с Xcode.

## Что уже сделано

| Модуль | Зачем |
|---|---|
| `:shared:contract` | KMP. JSON-контракт API (snapshots, media, Telegram inbox, push tokens). |
| `:shared:domain` | KMP. `GoalMoneyMath`, типы weekly goal, `PlatformTime`, зарезервированный `AuthProvider.APPLE`. |
| API `platform=ios` | Можно сохранить iOS push-token. FCM их **не** шлёт — APNs ещё нет. |

iOS-таргеты Gradle включаются **только на macOS** (нужен Xcode). Linux/CI собирают JVM.
Принудительно: `-Ptruckerload.enableIos=true` или `false`.

Android продолжает жить в `:app`. Новые фичи пишите там как обычно. В `:shared:domain`
переносите только чистый Kotlin без `android.*`, `java.time`, `org.json`.

## Что не трогаем сейчас

- Sign in with Apple / AuthenticationServices / Apple JWT на backend
- APNs ключи и sender
- Xcode-проект, SwiftUI, Compose Multiplatform UI
- Foreground Telegram на iPhone (на iOS только `TELEGRAM_SYNC_MODE=server`)

`AuthProvider.APPLE` — имя в общем модуле, без UI и без вызовов Apple.

## Как развивать Android, не ломая KMP

1. UI, Room, WorkManager, камера, ML Kit, виджеты, Google Sign-In — только `:app`.
2. Парсеры Relay, goal-математика, CSV — кандидаты в `:shared:domain`, когда из них
   уйдут `Calendar` / `Locale` / `java.time` (через `PlatformTime` + kotlinx-datetime).
3. Не подключайте Apple SDK «на будущее» в Android-логин.

## Когда дойдёте до iPhone

1. Mac + Xcode; `sh ./gradlew :shared:contract:linkDebugFrameworkIosSimulatorArm64`
   (и domain). Позже — один umbrella-framework `TruckerLoadShared`.
2. iOS UI (SwiftUI или Compose Multiplatform) поверх shared.
3. Telegram **только server webhook**, не long-poll на устройстве.
4. В конце: Apple Developer, Sign in with Apple (обязателен, если остаётся Google),
   APNs, TestFlight, App Store.

Подробности по облаку: [TARGET_ARCHITECTURE.md](TARGET_ARCHITECTURE.md).
