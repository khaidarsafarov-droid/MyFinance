import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.TestComponent
import com.google.android.libraries.mapsplatform.secrets_gradle_plugin.SecretsPluginExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.secrets)
}

// Secrets Gradle Plugin reads local.properties (gitignored) and fills ${MAPS_API_KEY}.
// local.properties also holds sdk.dir and other BuildConfig keys; inject only MAPS_API_KEY.
// The negative lookahead is anchored so the plugin's containsMatchIn keeps this one key.
configure<SecretsPluginExtension> {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
    ignoreList.add("^(?!MAPS_API_KEY$).*")
}

/**
 * Maps SDK key for `com.google.android.geo.API_KEY`.
 * Env `MAPS_API_KEY` wins, then gitignored `local.properties`, then the committed placeholder.
 */
fun resolveMapsApiKey(): String {
    fun read(fileName: String): String {
        val file = rootProject.file(fileName)
        if (!file.isFile) return ""
        val props = Properties()
        file.inputStream().use { props.load(it) }
        return props.getProperty("MAPS_API_KEY").orEmpty().trim().removeSurrounding("\"")
    }
    val fromEnv = System.getenv("MAPS_API_KEY").orEmpty().trim()
    if (fromEnv.isNotEmpty()) return fromEnv
    val fromLocal = read("local.properties")
    if (fromLocal.isNotEmpty()) return fromLocal
    val fromDefaults = read("local.defaults.properties")
    check(fromDefaults.isNotEmpty()) {
        "MAPS_API_KEY is missing from the environment, local.properties, and local.defaults.properties"
    }
    return fromDefaults
}

fun String.escapeForBuildConfigString(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "")
        .replace("\n", "")

// Crash reporting stays off for debug and release. A google-services.json on disk
// must not apply a Google plugin or turn crash uploads back on.

android {
    namespace = "com.truckerload"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.truckorig"
        minSdk = 24
        targetSdk = 36
        versionCode = 18
        versionName = "1.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProps = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
            localProps.load(stream)
        }
        // Stage3: never bake bot secrets into APKs by default (including debug/release).
        // Opt-in for local device debugging only: ./gradlew … -PallowDebugSecrets=true
        val allowDebugSecrets = project.hasProperty("allowDebugSecrets")
        val telegramToken = if (allowDebugSecrets) {
            localProps.getProperty("TELEGRAM_BOT_TOKEN", "")
        } else {
            ""
        }
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"$telegramToken\"")
        // Default false: first launch shows the local name screen, then local_dev.
        val localOnly = localProps.getProperty("LOCAL_ONLY_MODE", "false").equals("true", ignoreCase = true)
        buildConfigField("boolean", "LOCAL_ONLY_MODE", if (localOnly) "true" else "false")
        buildConfigField("boolean", "FIREBASE_CONFIGURED", "false")
        // Also set on each variant below. defaultConfig covers AGP 9 unit-test manifest merge,
        // which does not inherit placeholders the Secrets plugin adds only on the main variant.
        manifestPlaceholders["MAPS_API_KEY"] = resolveMapsApiKey()
        val googleDriveApiKey = (
            System.getenv("GOOGLE_DRIVE_API_KEY")
                ?: localProps.getProperty("GOOGLE_DRIVE_API_KEY", "")
            )
            .trim()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "GOOGLE_DRIVE_API_KEY", "\"$googleDriveApiKey\"")
        // Optional separate key for Directions REST. The map screen does not call Directions.
        val googleDirectionsApiKey = localProps.getProperty("GOOGLE_DIRECTIONS_API_KEY", "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "GOOGLE_DIRECTIONS_API_KEY", "\"$googleDirectionsApiKey\"")
        // Optional NDK ABI filter, e.g. -PabiFilters=arm64-v8a,armeabi-v7a
        val abiFiltersProp = (project.findProperty("abiFilters") as? String)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val selectedAbis = abiFiltersProp
        if (selectedAbis.isNotEmpty()) {
            ndk {
                abiFilters.clear()
                abiFilters.addAll(selectedAbis)
            }
        }
    }

    // Optional Play Store / production signing. Create keystore.properties (gitignored).
    // Without it, release stays unsigned (CI can still compile).
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
                // Same SHA-1 as Play upload key so Google Drive OAuth matches.
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
            // Uses the Play upload keystore when debug-keystore.properties is absent.
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Play Console native-symbols warning: include JNI symbol table when NDK is present.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            // Never bake bot secrets into release APKs.
            // Public client IDs (Google Web client) stay in defaultConfig.
            buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
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

// Env MAPS_API_KEY overrides the Secrets plugin (it only reads properties files).
// Registered after the plugin so this callback runs last and wins on the main variant.
androidComponents {
    onVariants { variant ->
        val mapsApiKey = resolveMapsApiKey()
        variant.manifestPlaceholders.put("MAPS_API_KEY", mapsApiKey)
        // AGP 9 validates ${MAPS_API_KEY} in unit-test and android-test manifests too.
        // ApplicationVariant.unitTest clashes with the legacy accessor, so use nested tests.
        variant.nestedComponents.filterIsInstance<TestComponent>().forEach { testComponent ->
            testComponent.manifestPlaceholders.put("MAPS_API_KEY", mapsApiKey)
        }
        variant.buildConfigFields?.put(
            "MAPS_API_KEY",
            BuildConfigField("String", "\"${mapsApiKey.escapeForBuildConfigString()}\"", null),
        )
    }
}

// Profiles are generated only when someone runs :app:generateBaselineProfile
// on a device. Release builds keep the checked-in src/main profiles and do
// not wait on a device. Signing, applicationId, version, and R8 minify stay
// on the release build type above.
baselineProfile {
    automaticGenerationDuringBuild = false
    dexLayoutOptimization = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":shared:contract"))
    implementation(project(":shared:domain"))
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.core)
    // Camera / flash glyphs (CameraAlt, FlashOn/Off/Auto) — kept for the restored camera UX.
    implementation(libs.androidx.compose.material.icons.extended)
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
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Gson (backup codec)
    implementation(libs.gson)

    // OkHttp (Telegram Bot API + Google Drive). Stay on 4.x; OkHttp 5 is a later pass.
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.serialization.json)

    // DataStore + Encrypted
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Biometric unlock (email accounts)
    implementation(libs.androidx.biometric)

    // Google Sign-In (Drive app-data backup only; app identity is local)
    implementation(libs.play.services.auth)
    implementation(libs.billing.ktx)

    // Google Maps (free-tier Maps SDK; friends map routes use OSRM, not Directions API)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.android.maps.utils)
    implementation(libs.play.services.location)
    implementation(libs.play.services.base)
    implementation(libs.kotlinx.coroutines.play.services)

    // CameraX (1.4.2+ required for 16 KB page-size aligned native libs)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)

    // ML Kit Document Scanner + OCR
    implementation(libs.play.services.mlkit.document.scanner)
    // ML Kit on-device Latin text recognition (model bundled with the dependency).
    // Artifact name is text-recognition (not *-bundled); used by OCRService for receipt OCR.
    implementation(libs.mlkit.text.recognition)

    // PDF generation (fallback when scanner returns JPEG pages only)
    implementation(libs.itext.kernel)
    implementation(libs.itext.io)
    implementation(libs.itext.layout)

    // Tesseract OCR (Russian + English, offline) — 4.8+ ships 16 KB-aligned arm64 libs
    implementation(libs.tesseract4android)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.coil.compose)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Glance App Widgets — One UI 2×2 / 4×2 with Material You colors
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // HTML parsing (Ksoup — Kotlin port of Jsoup)
    implementation(libs.ksoup)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.androidx.glance.appwidget.testing)
    kspTest(libs.hilt.compiler)
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
 * Phase 0: fail the build if release BuildConfig still embeds bot/API secrets,
 * or if the release safety switches (minify, cleartext, backup, schema export) flip.
 * Run: ./gradlew :app:verifyReleaseSecretsEmpty
 */
tasks.register("verifyReleaseSecretsEmpty") {
    group = "verification"
    description = "Ensures release BuildConfig has no Telegram token or hardcoded Drive API key"
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
        check(telegram.isEmpty()) {
            "Release BuildConfig must not embed TELEGRAM_BOT_TOKEN (found non-empty value)"
        }
        val gradleSource = file("build.gradle.kts").readText()
        check(
            gradleSource.contains("""buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"\"")"""),
        ) {
            "Release builds must force TELEGRAM_BOT_TOKEN to an empty string"
        }
        check(gradleSource.contains("isMinifyEnabled = true") && gradleSource.contains("isShrinkResources = true")) {
            "Release builds must keep R8 minify and resource shrinking"
        }
        check(gradleSource.contains("room.schemaLocation")) {
            "Room schema export must stay enabled"
        }
        val manifest = file("src/main/AndroidManifest.xml").readText()
        check(manifest.contains("""android:allowBackup="false"""")) {
            "android:allowBackup must stay false"
        }
        val network = file("src/main/res/xml/network_security_config.xml").readText()
        check(network.contains("""cleartextTrafficPermitted="false"""")) {
            "Cleartext traffic must stay blocked"
        }
        val proguard = file("proguard-rules.pro").readText()
        check(proguard.contains("net.zetetic.database.sqlcipher")) {
            "Release keep rules must include SQLCipher"
        }
        check(proguard.contains("com.google.crypto.tink")) {
            "Release keep rules must include Tink"
        }
        check(!Regex("""AIza[0-9A-Za-z_\-]{10,}""").containsMatchIn(gradleSource)) {
            "build.gradle.kts must not hardcode a Google API key. " +
                "Set GOOGLE_DRIVE_API_KEY in local.properties or the environment."
        }
        val suppliedDriveKey = (
            System.getenv("GOOGLE_DRIVE_API_KEY")
                ?: rootProject.file("local.properties").takeIf { it.exists() }?.let { propsFile ->
                    val props = Properties()
                    propsFile.inputStream().use { props.load(it) }
                    props.getProperty("GOOGLE_DRIVE_API_KEY")
                }
            ).orEmpty().trim()
        val driveKey = fieldValue("GOOGLE_DRIVE_API_KEY")
        check(driveKey == suppliedDriveKey.replace("\\", "\\\\").replace("\"", "\\\"")) {
            "Release GOOGLE_DRIVE_API_KEY does not match local.properties or GOOGLE_DRIVE_API_KEY env"
        }
        check(suppliedDriveKey.isNotBlank() || driveKey.isEmpty()) {
            "Release BuildConfig contains a Drive API key that is not supplied outside the repo"
        }
        check(manifest.contains("android:value=\"\${MAPS_API_KEY}\"")) {
            "AndroidManifest must expose the Maps SDK key as \${MAPS_API_KEY}"
        }
        check(!manifest.contains("GOOGLE_MAPS_API_KEY")) {
            "AndroidManifest must not keep the old GOOGLE_MAPS_API_KEY placeholder"
        }
        val defaultsFile = rootProject.file("local.defaults.properties")
        check(defaultsFile.isFile) { "Commit local.defaults.properties with a Maps key placeholder" }
        val defaultsText = defaultsFile.readText()
        check(!Regex("""AIza[0-9A-Za-z_\-]{10,}""").containsMatchIn(defaultsText)) {
            "local.defaults.properties must not contain a real Google API key"
        }
        val mapsKey = fieldValue("MAPS_API_KEY")
        check(mapsKey == resolveMapsApiKey()) {
            "Release MAPS_API_KEY does not match MAPS_API_KEY env, local.properties, or local.defaults.properties"
        }
        check(mapsKey.isNotEmpty()) { "Release MAPS_API_KEY must not be empty" }
        logger.lifecycle(
            "verifyReleaseSecretsEmpty: OK (telegram empty; Drive key not hardcoded; " +
                "Maps key from env, local.properties, or local.defaults.properties; " +
                "minify, backup, cleartext, and schema export unchanged)",
        )
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn("verifyReleaseSecretsEmpty")
}

/**
 * Fails if a build can ship crash reporting.
 * The check reads Gradle files only, so a local google-services.json cannot satisfy it.
 */
tasks.register("verifyNoFirebaseCrashReporting") {
    group = "verification"
    description = "Ensures debug and release builds do not apply or depend on crash reporting"
    doLast {
        val appGradle = file("build.gradle.kts").readText()
        val rootGradle = rootProject.file("build.gradle.kts").readText()
        val servicesPlugin = "com.google.gms." + "google-services"
        val crashPlugin = "com.google.firebase." + "crashlytics"
        val firebaseLibs = "libs." + "firebase"
        check(!appGradle.contains(servicesPlugin)) {
            "Do not apply the Google services plugin from app/build.gradle.kts"
        }
        check(!appGradle.contains(crashPlugin)) {
            "Do not apply or depend on crash reporting from app/build.gradle.kts"
        }
        check(!appGradle.contains(firebaseLibs)) {
            "Do not put a Firebase library on the app classpath"
        }
        check(appGradle.contains("""buildConfigField("boolean", "FIREBASE_CONFIGURED", "false")""")) {
            "FIREBASE_CONFIGURED must stay false"
        }
        check(!rootGradle.contains("libs.plugins." + "firebase.crashlytics")) {
            "Do not add the crash reporting Gradle plugin to the root project"
        }
        check(!rootGradle.contains("libs.plugins." + "google.services")) {
            "Do not add the Google services Gradle plugin to the root project"
        }
        val crashReporting = file("src/main/java/com/truckerload/utils/CrashReporting.kt").readText()
        check(!crashReporting.contains("FirebaseCrashlytics")) {
            "CrashReporting must not call a crash-reporting SDK"
        }
        val appClass = file("src/main/java/com/truckerload/TruckerLoadApp.kt").readText()
        check(!appClass.contains("FirebaseCrashlytics")) {
            "TruckerLoadApp must not initialize a crash-reporting SDK"
        }
        logger.lifecycle("verifyNoFirebaseCrashReporting: OK")
    }
}

tasks.matching {
    it.name == "assembleDebug" || it.name == "assembleRelease" || it.name == "bundleRelease"
}.configureEach {
    dependsOn("verifyNoFirebaseCrashReporting")
}
