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
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

abstract class ModPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // 1. Apply dependent plugins.
        //    Their extensions and tasks must exist before we can configure or
        //    query them.
        project.applyDependentPlugins()

        // 2. Register this plugin's extension.
        //    Consumers configure `mod { ... }` *after* apply() returns, so the
        //    object must exist by then. Capture the reference for later steps.
        val modExt = project.extensions.create<ModExtension>("mod")

        // 3. Read inputs that don't depend on user configuration.
        val javaVersion = project.readJavaVersion()

        // 4. Eager configuration — inputs available now; nothing here reads
        //    from the extension.
        project.configureJava(javaVersion)

        // 5. Lazy configuration — reads from the extension via
        //    Providers / configureEach so values resolve at task-configuration
        //    time, after the consumer's `mod { }` block has run.
        project.configureProcessResources(modExt, javaVersion)

        // 6. Deferred wiring — anything that must eagerly read user-set values
        //    or iterate a container the consumer populates. Prefer Providers
        //    (step 5) when possible; fall back here when the Provider API can't
        //    express what you need.
        project.configureDeferred(modExt)
    }

    private fun Project.applyDependentPlugins() {
        pluginManager.apply("repo.base-conventions")
        pluginManager.apply("repo.licensed-library")
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
                "java_version" to javaVersion,
                "minecraft_version" to modExt.minecraftVersion.get(),
                "fabric_loader_version" to modExt.fabricLoaderVersion.map(::zeroLastComponent).get(),
            )
            inputs.properties(props)
            filesMatching(
                listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml"),
            ) {
                expand(props)
            }
        }
    }

    private fun Project.configureDeferred(modExt: ModExtension) {
        afterEvaluate {
            // `Project.version` is `Any` (not a `Property`), so it can't be wired via a
            // Provider. afterEvaluate is the right place to set it once user config is known.
            version = "${modExt.minecraftVersion.get()}-${modExt.version.get()}"

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
