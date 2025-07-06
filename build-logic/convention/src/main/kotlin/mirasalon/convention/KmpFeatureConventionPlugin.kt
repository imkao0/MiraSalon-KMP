package mirasalon.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("mirasalon.kmp.library")
                apply("mirasalon.koin")
                apply("mirasalon.detekt")
                apply("mirasalon.circuit")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":core:common"))
                    implementation(project(":core:domain"))
                    implementation(project(":core:navigation"))
                    implementation(project(":core:designsystem"))
                    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                    implementation(libs.findLibrary("kotlinx-datetime").get())
                    implementation(libs.findLibrary("androidx-lifecycle-viewmodel").get())
                    implementation(libs.findLibrary("multiplatform-settings").get())
                }

                pluginManager.withPlugin("org.jetbrains.compose") {
                    sourceSets.getByName("commonMain").dependencies {
                        implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                        implementation(libs.findLibrary("koin-compose-viewmodel").get())
                        implementation(libs.findLibrary("compose-runtime").get())
                        implementation(libs.findLibrary("compose-foundation").get())
                        implementation(libs.findLibrary("material3").get())
                        implementation(libs.findLibrary("compose-ui").get())
                        implementation(libs.findLibrary("compose-components-resources").get())
                        implementation(libs.findLibrary("compose-materialIconsExtended").get())
                    }
                }
                
                sourceSets.getByName("commonTest").dependencies {
                    implementation(libs.findBundle("testing-common").get())
                }

                try {
                    libs.findLibrary("mockative-processor").get().let { mockativeProcessor ->
                        targets.configureEach {
                            val kspConfigurationName = if (name == "metadata") "kspCommonMainMetadata" else "ksp${name.replaceFirstChar { it.uppercase() }}"
                            configurations.findByName(kspConfigurationName)?.let {
                                dependencies.add(kspConfigurationName, mockativeProcessor)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // mockative-processor not found in version catalog, skip KSP configuration
                }
            }
        }
    }
}
