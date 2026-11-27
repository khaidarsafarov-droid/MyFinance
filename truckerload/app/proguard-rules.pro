# TruckerLoad ProGuard / R8

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# SQLCipher. The JNI entry points are looked up by name.
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclasseswithmembernames class net.zetetic.database.sqlcipher.** {
    native <methods>;
}
-dontwarn net.zetetic.database.**

# Tink, pulled in by EncryptedSharedPreferences. Protobuf lite reads fields
# by reflection, and the key-type registry looks managers up by name.
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn com.google.crypto.tink.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson / BackupData
-keepclassmembers class com.truckerload.data.backup.** { <fields>; }
-keepclassmembers class com.truckerload.domain.model.** { <fields>; }

# Compose / Kotlin metadata
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }

# OkHttp / Telegram
-dontwarn okhttp3.**
-dontwarn okio.**

# Google Sign-In / Play services
-dontwarn com.google.android.gms.**

# Firebase components / ML Kit. ComponentDiscovery reads registrar class names
# from manifest metadata and calls the no-arg constructor by reflection.
# firebase-components 16.x only keeps the class name, so R8 full mode drops
# <init>() and ML Kit never registers. TextRecognition.getClient() then NPEs,
# which crashed the Maintenance (ТО) and Scanner screens in 1.6.2 release.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}

# SLF4J binder (not shipped on Android)
-dontwarn org.slf4j.impl.StaticLoggerBinder

# PdfBox-Android: optional JPEG2000 codec (JP2Decoder/JP2Encoder) is not bundled
-dontwarn com.gemalto.jp2.**

# Crashlytics references newer Android Profiling APIs (API 35+) not in compileSdk 34
-dontwarn android.os.ProfilingManager
-dontwarn android.os.ProfilingResult
-dontwarn android.os.ProfilingTrigger
-dontwarn android.os.ProfilingTrigger$Builder
