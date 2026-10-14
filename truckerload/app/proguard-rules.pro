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

# iText optional crypto factories + SLF4J binder (not shipped on Android)
-dontwarn com.itextpdf.bouncycastle.BouncyCastleFactory
-dontwarn com.itextpdf.bouncycastlefips.BouncyCastleFipsFactory
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Crashlytics references newer Android Profiling APIs (API 35+) not in compileSdk 34
-dontwarn android.os.ProfilingManager
-dontwarn android.os.ProfilingResult
-dontwarn android.os.ProfilingTrigger
-dontwarn android.os.ProfilingTrigger$Builder
