import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("mirasalon.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

if (!skipAndroid) {
    configure<ApplicationExtension> {
        namespace = "iz.mkao.mirasalon"
        defaultConfig {
            applicationId = "iz.mkao.mirasalon"
            versionCode = 1
            versionName = "1.0"
        }

        buildFeatures {
            buildConfig = true
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.core.network)
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.core.domain)
    implementation(projects.feature.featureSalon)
    implementation(projects.feature.featureProducts)
    implementation(projects.feature.featureChat)
    implementation(projects.feature.featureCart)
    implementation(projects.feature.featureSpecialists)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
    implementation(libs.napier)
    implementation(libs.jetbrains.navigation3.ui)
    if (!skipAndroid) {
        implementation(libs.koin.android)
        implementation(libs.androidx.activity.compose)
    }
    implementation(libs.koin.compose)
    implementation(libs.cmptoast)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.material3)
    implementation(libs.compose.materialIconsExtended)
    implementation(libs.compose.ui)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.components.uiToolingPreview)
}
