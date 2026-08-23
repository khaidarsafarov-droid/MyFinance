import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(project(":shared:contract"))
    implementation("io.ktor:ktor-server-core-jvm:3.5.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.5.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.5.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.5.1")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.5.1")
    implementation("io.ktor:ktor-server-call-id-jvm:3.5.1")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.5.1")
    implementation("io.ktor:ktor-server-auth-jvm:3.5.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.17.0")
    implementation("com.auth0:java-jwt:4.6.0")
    implementation("com.auth0:jwks-rsa:0.22.2")

    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("org.flywaydb:flyway-core:12.11.0")
    implementation("org.flywaydb:flyway-database-postgresql:12.11.0")
    implementation("com.google.firebase:firebase-admin:9.10.0")

    implementation(platform("software.amazon.awssdk:bom:2.49.3"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:auth")

    runtimeOnly("ch.qos.logback:logback-classic:1.5.20")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:9.0")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.5.1")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:3.5.1")
}

tasks.test {
    useJUnitPlatform()
}
