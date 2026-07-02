package plugins.kotlinlibrary

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

abstract class KotlinLibraryConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.applyDependentPlugins()

        val libs = project.the<LibrariesForLibs>()
        val javaVersion = libs.versions.java.get()

        project.configureKotlin(javaVersion)
        project.configureDependencies()
        project.configureSpotlessForKotlinSources()
    }

    private fun Project.applyDependentPlugins() {
        pluginManager.apply("oy-java-library-conventions")
        pluginManager.apply("org.jetbrains.kotlin.jvm")
    }

    private fun Project.configureKotlin(javaVersion: String) {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(javaVersion.toInt())
            explicitApi()
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(javaVersion))
                allWarningsAsErrors.set(true)
                freeCompilerArgs.addAll(
                    "-Xjvm-default=all",
                    "-Xjsr305=strict",
                )
            }
        }
    }

    private fun Project.configureDependencies() {
        val libs = the<LibrariesForLibs>()
        dependencies {
            "testImplementation"(libs.kotlin.test.junit5)
        }
    }

    private fun Project.configureSpotlessForKotlinSources() {
        extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                ktlint()
            }
        }
    }

}
