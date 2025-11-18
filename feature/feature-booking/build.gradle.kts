import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("mirasalon.kmp.feature")
    id("mirasalon.circuit")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "iz.mkao.mirasalon.feature.booking"
    generateResClass = always
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

if (!skipAndroid) {
    apply(plugin = "org.jetbrains.kotlin.plugin.parcelize")
}

kotlin {
    if (!skipAndroid) {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "iz.mkao.mirasalon.feature.booking"
        }
    }
    sourceSets {
        // Intermediate source set shared by the jvm() and iOS targets so the
        // non-Android actual of CommonParcelize can live in exactly one place
        // (both are children of the default-hierarchy 'native'/'nonAndroid'
        // groupings, which we name explicitly here).
        val nativeJvmMain by creating {
            dependsOn(commonMain.get())
        }
        jvmMain.get().dependsOn(nativeJvmMain)
        iosMain.get().dependsOn(nativeJvmMain)

        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:domain"))
            implementation(project(":core:navigation"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:network"))
            implementation(project(":core:realtime"))
            implementation(project(":core:database"))
            implementation(project(":feature:feature-appointments"))
            implementation(project(":feature:feature-profile"))

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
            implementation(libs.qrose)
            implementation(libs.ksafe)
            implementation(libs.ksafe.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.circuit.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
