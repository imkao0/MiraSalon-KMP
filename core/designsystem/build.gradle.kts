import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("mirasalon.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "iz.mkao.mirasalon.core.designsystem"
    generateResClass = always
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

kotlin {
    if (!skipAndroid) {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "iz.mkao.mirasalon.core.designsystem"
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.cmptoast)
            implementation(libs.coil.compose)
        }
        sourceSets.findByName("androidMain")?.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        sourceSets.findByName("androidHostTest")?.dependencies {
            implementation(libs.robolectric)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.androidx.test.ext.junit)
            // runtimeOnly(libs.compose.ui.test.manifest) // manifest is usually Android-only
        }
    }
}
