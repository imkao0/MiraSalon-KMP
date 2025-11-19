package mirasalon.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun CommonExtension.configureAndroid(project: Project) {
    val libs = project.libs

    compileSdk = libs.findVersion("android-compileSdk").get().toString().toInt()

    when (this) {
        is LibraryExtension -> {
            defaultConfig.minSdk = libs.findVersion("android-minSdk").get().toString().toInt()
        }
        is ApplicationExtension -> {
            defaultConfig.minSdk = libs.findVersion("android-minSdk").get().toString().toInt()
            defaultConfig.targetSdk = libs.findVersion("android-targetSdk").get().toString().toInt()
        }
    }

    compileOptions.apply {
        sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
        targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
    }
}

internal fun KotlinMultiplatformAndroidLibraryTarget.configureAndroid(project: Project) {
    val libs = project.libs

    compileSdk = libs.findVersion("android-compileSdk").get().toString().toInt()
    minSdk = libs.findVersion("android-minSdk").get().toString().toInt()
}
