package plugins.javalibrary

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

abstract class JavaLibraryConventionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.applyDependentPlugins()

        val libs = project.the<LibrariesForLibs>()
        val javaVersion = libs.versions.java.get().toInt()

        project.configureJava(javaVersion)
        project.configureJavaCompile(javaVersion)
        project.configureJavadoc()
    }

    private fun Project.applyDependentPlugins() {
        pluginManager.apply("java-library")
        pluginManager.apply("oy-base-conventions")
        pluginManager.apply("oy-testing-conventions")
        pluginManager.apply("oy-licensed-library")
    }

    private fun Project.configureJava(javaVersion: Int) {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
            // Sources + Javadoc jars are configured by oy-published-library
            // (via the Vanniktech plugin's JavaLibrary(javadocJar, sourcesJar)
            // setup). Calling withSourcesJar()/withJavadocJar() here would
            // create a second, conflicting task chain that races with
            // Vanniktech's metadata generation.
            consistentResolution {
                useCompileClasspathVersions()
            }
        }
    }

    private fun Project.configureJavaCompile(javaVersion: Int) {
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(javaVersion)
            options.compilerArgs.addAll(
                listOf(
                    "-Xlint:all",
                    "-Xlint:-processing",
                    "-Werror",
                    "-parameters",
                ),
            )
        }
    }

    private fun Project.configureJavadoc() {
        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
        }
    }

}
