package mirasalon.convention

import app.cash.sqldelight.gradle.SqlDelightExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class SqlDelightConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.cash.sqldelight")

            extensions.configure<SqlDelightExtension> {
                databases.create("MiraSalonDatabase") {
                    packageName.set("iz.mkao.mirasalon.db")
                }
            }
        }
    }
}
