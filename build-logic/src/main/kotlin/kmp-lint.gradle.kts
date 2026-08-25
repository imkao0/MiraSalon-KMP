import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

afterEvaluate {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    detekt {
        toolVersion = libs.findVersion("detekt").get().toString()
        config.setFrom(files("${project.rootDir}/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
    }

    ktlint {
        version.set(libs.findVersion("ktlint-cli").get().toString())
        android.set(true)
        ignoreFailures.set(true)
        filter {
            exclude { it.file.path.contains("build/generated") }
            exclude { it.file.path.contains("resourceGenerator") }
        }
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
    }
}
