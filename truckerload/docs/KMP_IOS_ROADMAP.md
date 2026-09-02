# KMP → iOS roadmap

Android remains the production client. Portable contracts and weekly-goal math live in
Kotlin Multiplatform so the iOS app in `ios/` can call the same code.

## What already exists

| Piece | Role |
|---|---|
| `:shared:contract` | KMP identifiers shared with iOS (push platform labels today). |
| `:shared:domain` | KMP `GoalMoneyMath`, weekly-goal types, `PlatformTime`, reserved `AuthProvider.APPLE`. |
| `:shared` | Umbrella module. On macOS it builds **one** static framework `TruckerLoadShared` that exports contract + domain. |
| `ios/` | SwiftUI client. Xcode embeds `TruckerLoadShared` via `ios/embed-shared-framework.sh`. |

iOS Gradle targets are **on by default only on macOS** (Xcode required). Linux/CI compile JVM.
Force: `-Ptruckerload.enableIos=true` or `false`.

Android stays in `:app`. New Android features go there as usual. Move into `:shared:domain`
only pure Kotlin (no `android.*`, `java.time`, `org.json`).

## What is still out of scope

- Sign in with Apple / AuthenticationServices
- APNs keys and a push sender
- Compose Multiplatform UI (the iOS UI is SwiftUI)
- Foreground Telegram on iPhone

`AuthProvider.APPLE` is a reserved name in shared code, with no Apple SDK and no login UI.

## How to develop Android without breaking KMP

1. UI, Room, WorkManager, camera, ML Kit, widgets, Drive backup — only `:app`.
2. Relay parsers, goal math, CSV — candidates for `:shared:domain` after `Calendar` /
   `Locale` / `java.time` are replaced (`PlatformTime` + kotlinx-datetime).
3. Do not add Apple SDKs to the Android first-run flow “for later”.

## Build the iPhone app (Mac)

1. Open `ios/TruckoRig.xcodeproj` (repo path `truckerload/ios`).
2. Xcode runs `sh ./gradlew :shared:embedAndSignAppleFrameworkForXcode` (and domain/contract
   transitively) before Swift compile.
3. Swift: `import TruckerLoadShared` then `SharedBusinessLogic.shared`.
4. Later: Apple Developer Program, Sign in with Apple if App Store requires it,
   APNs, TestFlight, App Store.

Details: [ios/README.md](../ios/README.md), [TARGET_ARCHITECTURE.md](TARGET_ARCHITECTURE.md).

