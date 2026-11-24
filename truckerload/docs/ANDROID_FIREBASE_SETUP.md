# Crash reporting is not part of TruckoRig

Debug and release builds do not include Firebase Crashlytics, Firebase Sessions, or the Google services Gradle plugin.

`app/build.gradle.kts` sets `BuildConfig.FIREBASE_CONFIGURED` to `false` and does not read `app/google-services.json`. Putting that file next to the module does not turn crash uploads on. `:app:verifyNoFirebaseCrashReporting` fails the build if the plugin, the Firebase BoM, or a `FirebaseCrashlytics` call is added back.

`CrashReporting` only writes a redacted line to logcat. It does not send stack traces off the device.

The Play Data safety sheet in `store/google-play/DATA_SAFETY.md` still declares crash logs, because the Maps SDK’s own disclosure lists stack traces from inside the Maps SDK. Those are not Firebase crash reports, and the developer does not receive them.

There is no FCM client and no product backend in this app.
