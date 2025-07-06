package mirasalon.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"
            with(pluginManager) {
                if (!skipAndroid) {
                    apply("com.android.kotlin.multiplatform.library")
                }
                apply("org.jetbrains.kotlin.multiplatform")
                // apply("org.jetbrains.kotlinx.kover")
                apply("org.jetbrains.kotlin.plugin.serialization")
                if (!skipAndroid) {
                    apply("kotlin-parcelize")
                }
            }

            extensions.configure<KotlinMultiplatformExtension> {
                applyDefaultHierarchyTemplate()
                jvmToolchain(17)

                // Modern KMP Android Library configuration
                if (!skipAndroid) {
                    (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                        configureAndroid(this@with)
                        androidResources.enable = true
                        withHostTest {
                            isIncludeAndroidResources = true
                        }
                    }
                }

                jvm()
                iosArm64()
                iosSimulatorArm64()

                sourceSets.getByName("commonMain").dependencies {
                    implementation(libs.findLibrary("napier").get())
                }

                // Remove expect-actual warning
                compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
            }

            if (!skipAndroid) {
                tasks.withType<KotlinCompilationTask<*>>().configureEach {
                    compilerOptions.freeCompilerArgs.addAll(
                        "-P", "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=iz.mkao.mirasalon.core.navigation.CommonParcelize",
                        "-P", "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=iz.mkao.mirasalon.feature.cart.presentation.circuit.CommonParcelize",
                        "-P", "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=iz.mkao.mirasalon.feature.favourites.presentation.circuit.CommonParcelize",
                        "-P", "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=iz.mkao.mirasalon.feature.admin.presentation.circuit.CommonParcelize"
                    )
                }
            }
        }
    }
}
