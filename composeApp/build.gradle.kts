import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.plugins.ExtensionAware

plugins {
    id("mirasalon.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.skie)
}

compose.resources {
    packageOfResClass = "iz.mkao.mirasalon.shared"
    generateResClass = always
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

kotlin {
    if (!skipAndroid) {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "iz.mkao.mirasalon.shared"
        }
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            
            export(projects.core.common)
            export(projects.core.database)
            export(projects.core.network)
            export(projects.core.realtime)
            export(projects.core.designsystem)
            export(projects.core.navigation)
            export(projects.core.domain)
            export(projects.feature.featureSalon)
            export(projects.feature.featureProducts)
            export(projects.feature.featureSpecialists)
            export(projects.feature.featureFavourites)
            export(projects.feature.featureCart)
            export(projects.feature.featureChat)
            export(projects.feature.featureBooking)
            export(projects.feature.featureAppointments)
            export(projects.feature.featureAuth)
            export(projects.feature.featureProfile)
            export(projects.feature.featureNotifications)
            
            export(libs.circuit.runtime)
            export(libs.circuit.foundation)
        }
    }
    
    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigation3.ui)
            
            api(projects.core.common)
            api(projects.core.database)
            api(projects.core.network)
            api(projects.core.realtime)
            api(projects.core.designsystem)
            api(projects.core.navigation)
            api(projects.core.domain)
            api(projects.feature.featureSalon)
            api(projects.feature.featureProducts)
            api(projects.feature.featureSpecialists)
            api(projects.feature.featureFavourites)
            api(projects.feature.featureCart)
            api(projects.feature.featureChat)
            api(projects.feature.featureBooking)
            api(projects.feature.featureAppointments)
            api(projects.feature.featureAuth)
            api(projects.feature.featureProfile)
            api(projects.feature.featureNotifications)
            
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.coil.svg)
            implementation(libs.cmptoast)
            implementation(libs.napier)

            api(libs.circuit.foundation)
            api(libs.circuit.runtime)
            implementation(libs.circuit.runtime.presenter)
            implementation(libs.circuit.runtime.ui)
            implementation(libs.circuitx.gesture.navigation)
            implementation(libs.molecule.runtime)
        }
        sourceSets.findByName("androidMain")?.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.multiplatform.settings)
            implementation(libs.ktor.client.okhttp)
        }
    }
}
