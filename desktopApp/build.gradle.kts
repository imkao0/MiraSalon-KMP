import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                // Shared KMP libraries only — never the customer app module
                // (:composeApp) and never platform (Android/iOS) modules.
                implementation(projects.core.common)
                implementation(projects.core.domain)
                implementation(projects.core.network)
                implementation(projects.core.navigation)
                implementation(projects.core.designsystem)
                implementation(projects.core.realtime)
                implementation(projects.core.database)
                implementation(projects.feature.featureAuth)
                implementation(projects.feature.featureProducts)
                implementation(projects.feature.featureSpecialists)
                implementation(projects.feature.featureSalon)
                implementation(projects.feature.featureChat)
                implementation(projects.feature.featureAppointments)
                implementation(projects.feature.featureProfile)
                implementation(projects.feature.featureNotifications)

                // Compose Desktop (JVM) UI toolkit.
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(libs.compose.materialIconsExtended)

                // Coroutines (JVM).
                implementation(libs.kotlinx.coroutines.swing)

                // Slack Circuit (presenter + ui runtime).
                implementation(libs.circuit.foundation)
                implementation(libs.circuit.runtime)
                implementation(libs.circuit.runtime.presenter)
                implementation(libs.circuit.runtime.ui)

                // Dependency injection.
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                // Stream SDK (JVM).
                implementation(libs.stream.sdk.java)

                // Network + serialization + datetime (JVM client engine).
                implementation(libs.bundles.ktor.client)
                implementation(libs.ktor.client.java)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Image loading (JVM).
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                implementation(libs.coil.svg)

                // Settings-backed secure token storage + logging + calendar.
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.coroutines)
                implementation(libs.napier)
                implementation(libs.kizitonwose.calendar)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "iz.mkao.mirasalon.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb,
            )
            packageName = "iz.mkao.mirasalon"
            packageVersion = "1.0.0"

            // JVM tuning for the desktop admin app (not Android defaults).
            jvmArgs(
                "-Xmx2g",
                "-XX:+UseG1GC",
            )
        }
    }
}
