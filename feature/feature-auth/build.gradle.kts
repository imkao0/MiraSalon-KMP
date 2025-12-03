import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("mirasalon.kmp.feature")
    id("mirasalon.circuit")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

kotlin {
    if (!skipAndroid) {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "iz.mkao.mirasalon.feature.auth"
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:domain"))
            implementation(project(":core:navigation"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:network"))
            implementation(project(":core:realtime"))
            implementation(project(":core:database"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.materialIconsExtended)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.circuit.foundation)
            implementation(libs.circuit.runtime)
            implementation(libs.circuit.runtime.presenter)
            implementation(libs.circuit.runtime.ui)
            implementation(libs.circuit.codegen.annotations)

            implementation(libs.napier)
            implementation(libs.bundles.ktor.client)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            implementation(libs.jetbrains.navigation3.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.circuit.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
