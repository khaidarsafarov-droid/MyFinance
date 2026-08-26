# iOS client (TruckoRig)

SwiftUI shell that consumes the existing Kotlin Multiplatform business logic
(`:shared:contract` + `:shared:domain`) through one umbrella framework:

**`TruckerLoadShared`** — Gradle module `:shared`.

Android remains the production client. This app is the iOS entry point so weekly-goal
math and API contracts stay in one Kotlin tree. Journal/Room/camera stay on Android
until they are moved into `:shared:domain`.

## Requirements

- macOS with **Xcode 15+** and JDK 21
- Open `ios/TruckoRig.xcodeproj` (working directory is `truckerload/`)

Xcode runs `ios/embed-shared-framework.sh` before compiling Swift. That script calls:

```bash
sh ./gradlew :shared:embedAndSignAppleFrameworkForXcode -Ptruckerload.enableIos=true
```

Swift then `import TruckerLoadShared` and calls `SharedBusinessLogic.shared`.

Linux/CI never links the iOS framework (no Xcode). They still compile `:shared` for JVM
and check that this project is wired to the umbrella module.

## Layout

```
ios/
  TruckoRig.xcodeproj
  TruckoRig/                 SwiftUI sources
  embed-shared-framework.sh  Gradle → TruckerLoadShared.framework
```

Sign in with Apple and APNs are intentionally not included.
