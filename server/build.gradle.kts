plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("iz.mkao.mirasalon.server.ApplicationKt")
}

tasks.withType<JavaExec> {
    // Non-sensitive local-development defaults only.
    // Secrets (DATABASE_PASSWORD, STREAM_API_KEY/SECRET/APP_ID, JWT_SECRET,
    // ADMIN_PASSWORD, METRICS_PASSWORD) must be injected via real environment
    // variables or CI secrets. The server fails fast at startup when they are absent.
    environment("DATABASE_URL", "jdbc:postgresql://localhost:5432/mirasalon")
    environment("DATABASE_DRIVER", "org.postgresql.Driver")
    environment("DATABASE_USER", "mirasalon")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:domain"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultheaders)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ktor.server.call.id)

    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.json)

    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.hikaricp)

    implementation(libs.bcrypt)

    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    implementation(libs.koin.core)
    implementation(libs.koin.ktor3)
    implementation(libs.napier)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.logback.classic)

    implementation(libs.stream.sdk.java)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.ktor:ktor-server-test-host:${libs.versions.ktor.get()}")
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotlinx.coroutines.test)
}
