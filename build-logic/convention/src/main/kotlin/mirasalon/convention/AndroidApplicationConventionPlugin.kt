package mirasalon.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val skipAndroid = providers.systemProperty("skipAndroid").getOrElse("false") == "true"
            if (skipAndroid) return

            with(pluginManager) {
                apply("com.android.application")
            }

            extensions.configure<ApplicationExtension> {
                configureAndroid(this@with)
                defaultConfig.targetSdk = libs.findVersion("android-targetSdk").get().toString().toInt()
            }
        }
    }
}
