# AGENTS.md

## Cursor Cloud specific instructions

This repository is a single-module **native Android app** (`truckerload/`, Kotlin +
Jetpack Compose, module `:app`). There is **no backend / web / standalone bot service** —
the "Telegram bot" runs inside the app as a foreground service. General project info is in
`truckerload/README.md` and `truckerload/docs/PROJECT_OVERVIEW.md` (in Russian).

### Toolchain / environment (already provisioned in the VM snapshot)

- **JDK 21** at `/usr/lib/jvm/java-21-openjdk-amd64`; **Android SDK** at `~/android-sdk`
  (platform-34, build-tools 34.0.0, platform-tools, emulator, system image
  `android-34;google_apis;x86_64`). `JAVA_HOME`, `ANDROID_HOME`, and `PATH` are exported in
  `~/.bashrc`, so use an interactive/login shell (or `source ~/.bashrc`) before Android commands.
- **Non-obvious gotcha:** the committed `truckerload/gradle.properties` hard-codes a Windows
  path `org.gradle.java.home=C:\...\Android Studio\jbr`. This breaks Gradle on Linux, so it is
  overridden in `~/.gradle/gradle.properties` (`org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64`),
  which takes precedence. Do not "fix" it in the repo.
- `truckerload/local.properties` is gitignored and holds `sdk.dir` + `LOCAL_ONLY_MODE=true`
  (skips Supabase login; app runs fully offline on Room). The update script recreates it if missing.
- **`gradlew` is not marked executable** in this checkout — invoke it as `sh ./gradlew ...`.

### Build / test / lint (run from `truckerload/`)

- Build debug APK: `sh ./gradlew :app:assembleDebug`
- Unit tests: `sh ./gradlew :app:testDebugUnitTest` (38 tests, pass; cover the Relay/Telegram
  parsers, weekly-goal math, CSV export — this is the app's core domain logic)
- Lint: `sh ./gradlew :app:lintDebug` — **currently fails with ~663 pre-existing lint errors**
  (no `lint-baseline.xml`). This is a property of the existing code, not the environment.
- File-size gate: `sh ./gradlew :app:checkKotlinFileSize` — fails if a production `.kt` file
  exceeds **600 lines** (or grows past a cap in `truckerload/config/kotlin-file-size-baseline.txt`).
  Ideal target is **350** (warnings only). Wired into `:app:check` and CI.
- First `assembleDebug` downloads the Gradle 9.4.1 distribution + deps (~3 min); later builds are fast.

### Running the app on the emulator (important caveats)

- **No KVM / no nested virtualization** in this VM (`emulator -accel-check` fails). The emulator
  therefore runs in pure-software (TCG) mode and is **very slow**. It still boots and runs.
- Launch headless (an AVD `tl_test` may already exist):
  `emulator -avd tl_test -no-accel -no-snapshot -no-window -no-audio -memory 4096 -cores 4 -gpu swiftshader_indirect -no-boot-anim`
  Boot to `sys.boot_completed=1` takes several minutes; poll it.
- The app's animation-heavy Compose home screen keeps the main thread busy (~66% CPU idle) on the
  software GPU, which starves input and can trigger ANRs. To make it usable:
  - **AOT-compile the app once** after install: `adb shell cmd package compile -m speed -f com.truckerload`
    — this removes the startup class-verification CPU spike and keeps the process alive/responsive.
  - Disable animations: `adb shell settings put global {window,transition}_animation_scale 0` and
    `animator_duration_scale 0`.
  - WorkManager's `SystemJobService` can ANR-kill the app during a cold start under load; a warm/AOT
    start avoids it.
- **`adb shell input tap` coordinates are ACTUAL device pixels (1080×2340)**, not the scaled
  screenshot preview size — compute taps in real pixels.
- **`adb shell input text` drops characters** on the slow emulator; type char-by-char with a short
  sleep between keys (or paste on a real/KVM device). `cmd clipboard set-text` is unavailable.
- Screenshots via `adb exec-out screencap -p > file.png` work even when touch is laggy.

### Verified hello-world flow

Add-load: on "Добавить груз" paste a Relay message (needs `Total Rate` > 0 **and** a
`Pu-address:`/`Del-address:` line on its own line) → tap Save → the journal shows the parsed load
(count, miles, RPM). Example message that parses:
`Trip ID: T-116KYL6KW` / `Total Rate: 2500.00` / `Total Loaded Miles: 850 mi` / `Pu-address: SWF2, Garner, NC`.
