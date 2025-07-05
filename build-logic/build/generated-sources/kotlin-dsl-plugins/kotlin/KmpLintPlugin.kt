/**
 * Precompiled [kmp-lint.gradle.kts][Kmp_lint_gradle] script plugin.
 *
 * @see Kmp_lint_gradle
 */
public
class KmpLintPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Kmp_lint_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
