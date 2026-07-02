package plugins.testing

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

abstract class TestingConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.applyDependentPlugins()

        project.configureDependencies()
        project.configureTests()
    }

    private fun Project.applyDependentPlugins() {
        pluginManager.apply("java-library")
    }

    private fun Project.configureDependencies() {
        val libs = the<LibrariesForLibs>()
        dependencies {
            "testImplementation"(platform(libs.junit.bom))
            "testImplementation"(libs.junit.jupiter)
            "testRuntimeOnly"(libs.junit.platform.launcher)
        }
    }

    private fun Project.configureTests() {
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams = false
            }
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        }
    }

}
