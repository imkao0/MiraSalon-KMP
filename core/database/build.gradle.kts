import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("mirasalon.kmp.library")
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

room {
    schemaDirectory("$projectDir/schemas")
}

val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"

kotlin {
    if (!skipAndroid) {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "iz.mkao.mirasalon.core.database"
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.datetime)
        }
        sourceSets.findByName("androidMain")?.dependencies {
            implementation(libs.koin.android)
        }
    }
}

dependencies {
    if (!skipAndroid) {
        add("kspAndroid", libs.androidx.room.compiler)
    }
    add("kspJvm", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
