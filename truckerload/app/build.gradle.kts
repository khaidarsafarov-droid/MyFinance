import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val firebaseConfigured = file("google-services.json").isFile
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.truckerload"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.truckerload"
        minSdk = 24
        targetSdk = 34
        versionCode = 11
        versionName = "1.5.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProps = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
            localProps.load(stream)
        }
        buildConfigField("String", "CEREBRAS_API_KEY", "\"${localProps.getProperty("CEREBRAS_API_KEY", "")}\"")
        buildConfigField("String", "CEREBRAS_MODEL", "\"${localProps.getProperty("CEREBRAS_MODEL", "llama3.1-8b")}\"")
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${localProps.getProperty("TELEGRAM_BOT_TOKEN", "")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
        val syncBackendUrl = localProps.getProperty("SYNC_BACKEND_URL", "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "SYNC_BACKEND_URL", "\"$syncBackendUrl\"")
        val telegramSyncMode = localProps.getProperty("TELEGRAM_SYNC_MODE", "device")
            .trim()
            .lowercase()
            .takeIf { it == "device" || it == "server" }
            ?: "device"
        buildConfigField("String", "TELEGRAM_SYNC_MODE", "\"$telegramSyncMode\"")
        val telegramServerBotUsername = localProps.getProperty("TELEGRAM_SERVER_BOT_USERNAME", "")
            .trim()
            .removePrefix("@")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "TELEGRAM_SERVER_BOT_USERNAME", "\"$telegramServerBotUsername\"")
        // Default false: app entry requires Google Auth (no silent local_dev login).
        val localOnly = localProps.getProperty("LOCAL_ONLY_MODE", "false").equals("true", ignoreCase = true)
        buildConfigField("boolean", "LOCAL_ONLY_MODE", if (localOnly) "true" else "false")
        val cloudMediaEnabled = localProps.getProperty("CLOUD_MEDIA_ENABLED", "false")
            .equals("true", ignoreCase = true)
        buildConfigField("boolean", "CLOUD_MEDIA_ENABLED", cloudMediaEnabled.toString())
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = localProps.getProperty("GOOGLE_MAPS_API_KEY", "")
        // Phone APKs: drop x86/x86_64 emulator ABIs (halves APK size for friends share).
        // Pass -PfriendsPhoneApk=true or -PabiFilters=arm64-v8a,armeabi-v7a
        val abiFiltersProp = (project.findProperty("abiFilters") as? String)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val friendsPhone = project.hasProperty("friendsPhoneApk")
        val selectedAbis = when {
            abiFiltersProp.isNotEmpty() -> abiFiltersProp
            friendsPhone -> listOf("arm64-v8a", "armeabi-v7a")
            else -> emptyList()
        }
        if (selectedAbis.isNotEmpty()) {
            ndk {
                abiFilters.clear()
                abiFilters.addAll(selectedAbis)
            }
        }
    }

    // Optional friends/production signing. Create keystore.properties (gitignored) —
    // see docs/FRIENDS_SHARE.md. Without it, release stays unsigned (CI can still compile).
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val storePath = keystoreProperties.getProperty("storeFile")
                    ?: error("keystore.properties missing storeFile")
                storeFile = rootProject.file(storePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: error("keystore.properties missing storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: error("keystore.properties missing keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: error("keystore.properties missing keyPassword")
            }
        }
    }
    buildTypes {
        debug {
            // Dev-only secrets may come from local.properties via defaultConfig.
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Never bake server/bot secrets into release APKs.
            // Public client IDs (Supabase anon, Google Web client) stay in defaultConfig.
            buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"\"")
            buildConfigField("String", "CEREBRAS_API_KEY", "\"\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
        baseline = file("lint-baseline.xml")
        checkDependencies = false
    }
}

dependencies {
    implementation(project(":shared:contract"))
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt (KSP) + WorkManager integration
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Firebase is inert without google-services.json; cloud builds still compile.
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics")

    // DataStore + Encrypted
    implementation(libs.androidx.datastore.preferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Biometric unlock (email accounts)
    implementation("androidx.biometric:biometric:1.1.0")

    // Google Sign-In (legacy fallback)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Credential Manager (One Tap replacement, modern Sign-in with Google)
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // CameraX (1.4.2+ required for 16 KB page-size aligned native libs)
    val cameraXVersion = "1.4.2"
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // ML Kit Document Scanner + OCR
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    // ML Kit on-device Latin text recognition (model bundled with the dependency).
    // Artifact name is text-recognition (not *-bundled); used by OCRService for receipt OCR.
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // PDF generation (fallback when scanner returns JPEG pages only)
    implementation("com.itextpdf:kernel:8.0.5")
    implementation("com.itextpdf:io:8.0.5")
    implementation("com.itextpdf:layout:8.0.5")

    // Tesseract OCR (Russian + English, offline) — 4.8+ ships 16 KB-aligned arm64 libs
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")

    implementation("androidx.compose.foundation:foundation")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // WebRTC audio — 1.3.10+ ships 16 KB-aligned libjingle_peerconnection_so.so (arm64)
    implementation("io.getstream:stream-webrtc-android:1.3.10")

    // Media session for call notifications
    implementation("androidx.media:media:1.7.0")
    implementation("com.patrykandpatrick.vico:compose:1.15.0")
    implementation("com.patrykandpatrick.vico:compose-m3:1.15.0")

    // HTML parsing (Ksoup — Kotlin port of Jsoup)
    implementation("com.fleeksoft.ksoup:ksoup:0.2.4")

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

/**
 * Phase 0: fail the build if release BuildConfig still embeds bot/API secrets.
 * Run: ./gradlew :app:verifyReleaseSecretsEmpty
 */
tasks.register("verifyReleaseSecretsEmpty") {
    group = "verification"
    description = "Ensures TELEGRAM_BOT_TOKEN and CEREBRAS_API_KEY are empty in release BuildConfig"
    dependsOn("generateReleaseBuildConfig")
    doLast {
        val buildConfig = layout.buildDirectory.file(
            "generated/source/buildConfig/release/com/truckerload/BuildConfig.java",
        ).get().asFile
        require(buildConfig.exists()) {
            "Release BuildConfig not found at ${buildConfig.path}"
        }
        val text = buildConfig.readText()
        fun fieldValue(name: String): String {
            val regex = Regex("""String\s+$name\s*=\s*"([^"]*)";""")
            return regex.find(text)?.groupValues?.get(1)
                ?: error("BuildConfig field $name not found")
        }
        val telegram = fieldValue("TELEGRAM_BOT_TOKEN")
        val cerebras = fieldValue("CEREBRAS_API_KEY")
        check(telegram.isEmpty()) {
            "Release BuildConfig must not embed TELEGRAM_BOT_TOKEN (found non-empty value)"
        }
        check(cerebras.isEmpty()) {
            "Release BuildConfig must not embed CEREBRAS_API_KEY (found non-empty value)"
        }
        logger.lifecycle("verifyReleaseSecretsEmpty: OK (telegram/cerebras empty in release)")
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn("verifyReleaseSecretsEmpty")
}
