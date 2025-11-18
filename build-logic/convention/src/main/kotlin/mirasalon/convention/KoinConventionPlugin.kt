package mirasalon.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.gradle.kotlin.dsl.configure

class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = target.libs

            if (pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                extensions.configure<KotlinMultiplatformExtension> {
                    sourceSets.getByName("commonMain").dependencies {
                        implementation(libs.findLibrary("koin-core").get())
                        implementation(libs.findLibrary("koin-compose").get())
                    }
                }
            } else {
                dependencies {
                    add("implementation", libs.findLibrary("koin-core").get())
                }
            }
        }
    }
}
