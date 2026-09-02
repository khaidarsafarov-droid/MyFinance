import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    truckerloadIosTargets("TruckerLoadShared") {
        // One framework for the Swift client: domain math + shared identifiers.
        export(project(":shared:contract"))
        export(project(":shared:domain"))
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:contract"))
            api(project(":shared:domain"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.register("test") {
    group = "verification"
    description = "Runs JVM tests (iOS targets are macOS-only)."
    dependsOn("jvmTest")
}

private fun org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension.truckerloadIosTargets(
    frameworkBaseName: String,
    configure: Framework.() -> Unit = {},
) {
    if (!truckerloadIosEnabled()) return
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = frameworkBaseName
            isStatic = true
            configure()
        }
    }
}

private fun truckerloadIosEnabled(): Boolean {
    val override = findProperty("truckerload.enableIos")?.toString()?.lowercase()
    return when (override) {
        "true" -> true
        "false" -> false
        else -> System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)
    }
}
