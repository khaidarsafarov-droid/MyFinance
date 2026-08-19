import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.truckerload"
        minSdk = 24
        targetSdk = 35
        versionCode = 11
        versionName = "1.5.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProps = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
            localProps.load(stream)
        }
        // Stage3: never bake bot/AI secrets into APKs by default (including debug/friends).
        // Opt-in for local device debugging only: ./gradlew … -PallowDebugSecrets=true
        val allowDebugSecrets = project.hasProperty("allowDebugSecrets")
        val cerebrasKey = if (allowDebugSecrets) {
            localProps.getProperty("CEREBRAS_API_KEY", "")
        } else {
            ""
        }
        val telegramToken = if (allowDebugSecrets) {
            localProps.getProperty("TELEGRAM_BOT_TOKEN", "")
        } else {
            ""
        }
        buildConfigField("String", "CEREBRAS_API_KEY", "\"$cerebrasKey\"")
        buildConfigField("String", "CEREBRAS_MODEL", "\"${localProps.getProperty("CEREBRAS_MODEL", "llama3.1-8b")}\"")
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"$telegramToken\"")
        // Non-secret Web OAuth client — required for Google ID tokens. Fall back to the
        // project default so friends/CI builds still get Sign-In when local.properties omits it.
        val defaultGoogleWebClientId =
            "842861516910-gkhu4dh9tu5rc8re40rpe4583hvs4uhv.apps.googleusercontent.com"
        val googleWebClientId = localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")
            .trim()
            .ifBlank { defaultGoogleWebClientId }
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
        val syncBackendUrl = localProps.getProperty("SYNC_BACKEND_URL", "")
            .trim()
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
        val googleMapsApiKey = localProps.getProperty("GOOGLE_MAPS_API_KEY", "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$googleMapsApiKey\"")
        // Optional separate key for Directions REST (Android-restricted Maps keys often
        // work for tiles but deny HTTPS Directions). Falls back to GOOGLE_MAPS_API_KEY.
        val googleDirectionsApiKey = localProps.getProperty("GOOGLE_DIRECTIONS_API_KEY", "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "GOOGLE_DIRECTIONS_API_KEY", "\"$googleDirectionsApiKey\"")
        val turnUri = localProps.getProperty("TURN_URI", "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        val turnUsername = localProps.getProperty("TURN_USERNAME", "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        val turnCredential = localProps.getProperty("TURN_CREDENTIAL", "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "TURN_URI", "\"$turnUri\"")
        buildConfigField("String", "TURN_USERNAME", "\"$turnUsername\"")
        buildConfigField("String", "TURN_CREDENTIAL", "\"$turnCredential\"")
        val maxGroupCall = localProps.getProperty("MAX_GROUP_CALL_PARTICIPANTS", "8").toIntOrNull() ?: 8
        buildConfigField("int", "MAX_GROUP_CALL_PARTICIPANTS", maxGroupCall.coerceIn(2, 50).toString())
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
    // Optional permanent debug keystore: debug-keystore.properties (also gitignored).
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }
    val debugKeystorePropertiesFile = rootProject.file("debug-keystore.properties")
    val debugKeystoreProperties = Properties()
    if (debugKeystorePropertiesFile.exists()) {
        debugKeystorePropertiesFile.inputStream().use { debugKeystoreProperties.load(it) }
    }
    fun com.android.build.api.dsl.ApkSigningConfig.applyKeystore(props: Properties) {
        val storePath = props.getProperty("storeFile")
            ?: error("signing properties missing storeFile")
        storeFile = rootProject.file(storePath)
        storePassword = props.getProperty("storePassword")
            ?: error("signing properties missing storePassword")
        keyAlias = props.getProperty("keyAlias")
            ?: error("signing properties missing keyAlias")
        keyPassword = props.getProperty("keyPassword")
            ?: error("signing properties missing keyPassword")
    }
    signingConfigs {
        getByName("debug") {
            when {
                debugKeystorePropertiesFile.exists() -> applyKeystore(debugKeystoreProperties)
                // Same SHA-1 as friends/release so Google Sign-In matches Track Load.
                keystorePropertiesFile.exists() -> applyKeystore(keystoreProperties)
            }
        }
        create("release") {
            if (keystorePropertiesFile.exists()) {
                applyKeystore(keystoreProperties)
            }
        }
    }
    buildTypes {
        debug {
            // Dev-only secrets may come from local.properties via defaultConfig.
            // Uses friends/release keystore when debug-keystore.properties is absent.
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
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":shared:contract"))
    implementation(project(":shared:domain"))
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
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
    implementation(libs.androidx.lifecycle.runtime.compose)

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
    androidTestImplementation("androidx.room:room-testing:2.7.0")

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Retrofit + OkHttp (legacy remote clients)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Ktor HTTP client (CIO engine — avoids OkHttp 4/5 clash with existing clients)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

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

    // Google Maps (free-tier Maps SDK; friends map routes use OSRM, not Directions API)
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
    // LiveKit SFU for group rooms. If Gradle reports duplicate org.webrtc classes,
    // drop stream-webrtc-android and keep LiveKit's bundled WebRTC for mesh/1:1 too.
    implementation("io.livekit:livekit-android:2.23.0")

    // Media session for call notifications
    implementation("androidx.media:media:1.7.0")
    implementation("com.patrykandpatrick.vico:compose:1.15.0")
    implementation("com.patrykandpatrick.vico:compose-m3:1.15.0")

    // Glance App Widgets — One UI 2×2 / 4×2 with Material You colors
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

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
 * Phase 3: fail when Kotlin production sources exceed the 600-line soft limit
 * (or grow past a frozen baseline cap). Ideal target is 350 (warnings only).
 * Run: ./gradlew :app:checkKotlinFileSize
 */
tasks.register<Exec>("checkKotlinFileSize") {
    group = "verification"
    description = "Fails if Kotlin sources exceed the 600-line soft limit (Phase 3 file-size gate)"
    workingDir = rootProject.projectDir
    commandLine(
        "python3",
        "scripts/check_kotlin_file_size.py",
        "--app-dir",
        project.projectDir.absolutePath,
        "--baseline",
        rootProject.file("config/kotlin-file-size-baseline.txt").absolutePath,
    )
}

tasks.named("check").configure {
    dependsOn("checkKotlinFileSize")
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
