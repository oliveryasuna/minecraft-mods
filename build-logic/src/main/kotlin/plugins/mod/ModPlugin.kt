package plugins.mod

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

abstract class ModPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.applyDependentPlugins()

        val modExt = project.extensions.create<ModExtension>("mod")
        val javaVersion = project.readJavaVersion()

        project.configureJava(javaVersion)

        project.configureProcessResources(modExt, javaVersion)
        project.configureJarManifest(modExt, javaVersion)

        project.configureDeferred(modExt)
    }

    private fun Project.applyDependentPlugins() {
        pluginManager.apply("oy-base-conventions")
        pluginManager.apply("oy-licensed-library")
    }

    private fun Project.readJavaVersion(): String =
        extensions.getByType<VersionCatalogsExtension>()
            .named("libs")
            .findVersion("java")
            .get()
            .requiredVersion

    private fun Project.configureJava(javaVersion: String) {
        val target = javaVersion.toInt()

        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(target))
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(target)
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

    private fun Project.configureProcessResources(
        modExt: ModExtension,
        javaVersion: String
    ) {
        tasks.withType<ProcessResources>().configureEach {
            val props = mapOf(
                "mod_id" to modExt.id.get(),
                "mod_version" to project.version.toString(),
                "mod_name" to modExt.name.get(),
                "mod_desc" to modExt.description.orNull,
                "mod_author" to modExt.author.get(),
                "java_version" to javaVersion,
                "minecraft_version" to modExt.minecraftVersion.get(),
                "fabric_loader_version" to modExt.fabricLoaderVersion.map(::zeroLastComponent).getOrElse(""),
                "neoforge_version" to modExt.neoforgeVersion.getOrElse(""),
            )
            inputs.properties(props)
            filesMatching(
                listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml"),
            ) {
                expand(props)
            }
        }
    }

    private fun Project.configureJarManifest(
        modExt: ModExtension,
        javaVersion: String
    ) {
        tasks.withType<Jar>().configureEach {
            manifest {
                attributes(
                    mapOf(
                        "Specification-Title" to modExt.name,
                        "Specification-Version" to modExt.version,
                        "Specification-Vendor" to modExt.author,
                        "Implementation-Title" to modExt.name,
                        "Implementation-Version" to project.provider { project.version.toString() },
                        "Implementation-Vendor" to modExt.author,
                        "Built-JDK" to javaVersion,
                    )
                )
            }
        }
    }

    private fun Project.configureDeferred(modExt: ModExtension) {
        afterEvaluate {
            // `Project.version` is `Any` (not a `Property`), so it can't be wired via a
            // Provider. afterEvaluate is the right place to set it once user config is known.
            version = "${modExt.version.get()}+${modExt.minecraftVersion.get()}"

            // Iterate variants once the consumer's `mod { variants { ... } }` block has
            // fully populated each variant.
            modExt.variants.forEach { variant ->
                val config = createVariantConfiguration(variant)
                val syncTask = registerVariantSyncTask(variant, config)
                plugins.withId("fabric-loom") { cloneLoomRuns(variant, syncTask) }
                plugins.withId("net.neoforged.moddev") { cloneModDevRuns(variant, syncTask) }
            }
        }
    }

    private fun Project.createVariantConfiguration(variant: DevRuntimeVariant): Configuration =
        configurations.create("${variant.name}DevRuntime") {
            isCanBeResolved = true
            isCanBeConsumed = false
            isTransitive = false
            description = "Mod jars staged into ${variant.gameDir.get()}/mods/ for the ${variant.name} run variant."
            variant.modCoordinates.get().forEach { coord ->
                dependencies.add(project.dependencies.create(coord))
            }
        }

    private fun Project.registerVariantSyncTask(
        variant: DevRuntimeVariant,
        config: Configuration,
    ): TaskProvider<Sync> {
        val capitalized = variant.name.replaceFirstChar { it.uppercaseChar() }

        return tasks.register<Sync>("sync${capitalized}ToRun") {
            group = "modded variants"
            description = "Syncs the ${variant.name} modded variant's mods into ${variant.gameDir.get()}/mods/."
            from(config)
            into(variant.gameDir.map { layout.projectDirectory.dir(it).dir("mods") })
        }
    }

    private fun Project.cloneLoomRuns(
        variant: DevRuntimeVariant,
        syncTask: TaskProvider<Sync>
    ) {
        val baseRunNames = variant.baseRunNames.get()
        if(baseRunNames.isEmpty()) return

        val loom = extensions.getByType<LoomGradleExtensionAPI>()
        val capitalized = variant.name.replaceFirstChar { it.uppercaseChar() }
        baseRunNames.forEach { baseRunName ->
            val variantRunName = "$baseRunName$capitalized"
            loom.runs.register(variantRunName) {
                inherit(loom.runs.getByName(baseRunName))
                runDir(variant.gameDir.get())
            }
            wireRunTaskDependency(variantRunName, syncTask.name)
        }
    }

    private fun Project.cloneModDevRuns(
        variant: DevRuntimeVariant,
        syncTask: TaskProvider<Sync>
    ) {
        val baseRunNames = variant.baseRunNames.get()
        if(baseRunNames.isEmpty()) return

        val mdg = extensions.getByType<NeoForgeExtension>()
        val capitalized = variant.name.replaceFirstChar { it.uppercaseChar() }
        baseRunNames.forEach { baseRunName ->
            val variantRunName = "$baseRunName$capitalized"
            val baseRun = mdg.runs.getByName(baseRunName)
            mdg.runs.register(variantRunName) {
                when(baseRun.type.orNull) {
                    "client" -> client()
                    "server" -> server()
                }
                gameDirectory.set(layout.projectDirectory.dir(variant.gameDir.get()))
                loadedMods.addAll(baseRun.loadedMods)
            }
            wireRunTaskDependency(variantRunName, syncTask.name)
        }
    }

    private fun Project.wireRunTaskDependency(
        variantRunName: String,
        syncTaskName: String
    ) {
        val taskName = "run${variantRunName.replaceFirstChar { it.uppercaseChar() }}"
        tasks.named(taskName) {
            dependsOn(syncTaskName)
        }
    }

}

/**
 * Zeroes the last dotted component of a version string:
 * `"0.16.14"` -> `"0.16.0"`. If there is no `.` the input is returned
 * unchanged.
 */
private fun zeroLastComponent(version: String): String =
    if(!version.contains('.')) version
    else version.substringBeforeLast('.') + ".0"
