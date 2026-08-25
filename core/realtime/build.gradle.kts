import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("mirasalon.kmp.library")
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

kotlin {
    if (!skipAndroid) {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "iz.mkao.mirasalon.core.realtime"
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:network"))
            implementation(project(":core:domain"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            api(libs.ktor.client.core)
        }
        if (!skipAndroid) {
            val androidMain by getting {
                dependencies {
                    implementation(libs.stream.chat.android.client)
                    implementation(libs.stream.chat.android.offline)
                    implementation(libs.stream.chat.android.state)
                }
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.stream.sdk.java)
            }
        }
    }
}
