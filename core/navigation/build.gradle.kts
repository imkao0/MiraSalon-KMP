import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("mirasalon.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

if (!skipAndroid) {
    apply(plugin = "kotlin-parcelize")
}

kotlin {
    if (!skipAndroid) {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "iz.mkao.mirasalon.core.navigation"
        }
    }
    sourceSets {
        val nativeJvmMain by creating {
            dependsOn(commonMain.get())
        }
        jvmMain.get().dependsOn(nativeJvmMain)
        iosMain.get().dependsOn(nativeJvmMain)

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.materialIconsExtended)
            api(libs.jetbrains.navigation3.ui)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.circuit.runtime)
        }
    }
}
