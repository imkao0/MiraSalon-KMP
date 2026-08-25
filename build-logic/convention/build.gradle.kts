import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

group = "mirasalon.convention"

dependencies {
    implementation(libs.androidPluginGradle)
    implementation(libs.kotlinPluginGradle)
    implementation(libs.composePluginGradle)
    implementation(libs.kspPluginGradle)
    implementation(libs.detekt.gradle)
    implementation(libs.ktlint.gradle)
    implementation(libs.kover.gradle)
    implementation(libs.dependencyAnalysis.gradle)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mirasalon.android.application"
            implementationClass = "mirasalon.convention.AndroidApplicationConventionPlugin"
        }
        register("kmpLibrary") {
            id = "mirasalon.kmp.library"
            implementationClass = "mirasalon.convention.KotlinMultiplatformConventionPlugin"
        }
        register("kmpFeature") {
            id = "mirasalon.kmp.feature"
            implementationClass = "mirasalon.convention.KmpFeatureConventionPlugin"
        }
        register("koin") {
            id = "mirasalon.koin"
            implementationClass = "mirasalon.convention.KoinConventionPlugin"
        }
        register("detekt") {
            id = "mirasalon.detekt"
            implementationClass = "mirasalon.convention.DetektConventionPlugin"
        }
        register("circuit") {
            id = "mirasalon.circuit"
            implementationClass = "mirasalon.convention.CircuitConventionPlugin"
        }
    }
}
