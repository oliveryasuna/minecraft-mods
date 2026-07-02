package plugins.mod

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources

abstract class ModPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        applyPlugins(project)

        val modExt = project.extensions.create<ModExtension>("mod")

        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        val javaVersion = libs.findVersion("java").get().requiredVersion

        configureJava(project, javaVersion)

        configureProcessResources(project, modExt, javaVersion)

        project.afterEvaluate {
            project.version = "${modExt.minecraftVersion.get()}-${modExt.version.get()}"
        }
    }

    private fun applyPlugins(project: Project) {
        project.pluginManager.apply("repo.base-conventions")
        project.pluginManager.apply("repo.licensed-library")
    }

    private fun configureJava(
        project: Project,
        javaVersion: String
    ) {
        val javaVersionInt = javaVersion.toInt()

        project.extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersionInt))
            withSourcesJar()
            withJavadocJar()
        }

        project.tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(javaVersionInt)
            options.compilerArgs.addAll(
                listOf(
                    "-Xlint:all",
                    "-Xlint:-deprecation",
                    "-Xlint:-processing",
                    "-parameters",
                ),
            )
        }
    }

    private fun configureProcessResources(
        project: Project,
        modExt: ModExtension,
        javaVersion: String
    ) {
        project.tasks.withType<ProcessResources>().configureEach {
            val templateProps = mapOf(
                "version" to project.version.toString(),
                "java_version" to javaVersion,
                "minecraft_version" to modExt.minecraftVersion.get(),
                "fabric_loader_version" to zeroLastComponent(modExt.fabricLoaderVersion.get()),
            )
            inputs.properties(templateProps)
            filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
                expand(templateProps)
            }
        }
    }

}

/**
 * Zeroes the last dotted component of a version string:
 * `"0.16.14"` -> `"0.16.0"`.
 * If there is no `.` the input is returned unchanged.
 */
private fun zeroLastComponent(version: String): String =
    if(!version.contains('.')) version
    else version.substringBeforeLast('.') + ".0"
