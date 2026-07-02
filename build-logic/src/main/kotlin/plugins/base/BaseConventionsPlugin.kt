package plugins.base

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

abstract class BaseConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.applyDependentPlugins()

        project.group = "com.oliveryasuna.mc"
        project.configureSpotless()
    }

    private fun Project.applyDependentPlugins() {
        pluginManager.apply("com.diffplug.spotless")
    }

    private fun Project.configureSpotless() {
        extensions.configure<SpotlessExtension> {
            kotlinGradle {
                target("*.gradle.kts")
                ktlint()
            }
            format("misc") {
                target("*.md", ".gitignore")
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }

}
