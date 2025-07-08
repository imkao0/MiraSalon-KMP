package mirasalon.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CircuitConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // NOTE: KSP automation for Circuit is currently disabled due to environmental KSP configuration issues
            // (NoSuchElementException: Key module-name is missing in the map).
            // Infrastructure is prepared in code, but KSP plugin application is deferred.

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(libs.findLibrary("circuit-foundation").get())
                    implementation(libs.findLibrary("circuit-runtime").get())
                    implementation(libs.findLibrary("circuit-runtime-presenter").get())
                    implementation(libs.findLibrary("circuit-runtime-ui").get())
                    implementation(libs.findLibrary("circuit-retained").get())
                    implementation(libs.findLibrary("circuit-codegen-annotations").get())
                }
            }
        }
    }
}
