import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("mirasalon.kmp.library")
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

kotlin {
    if (!skipAndroid) {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "iz.mkao.mirasalon.core.common"
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            api(libs.multiplatform.settings)
            api(libs.napier)
        }
    }
}
