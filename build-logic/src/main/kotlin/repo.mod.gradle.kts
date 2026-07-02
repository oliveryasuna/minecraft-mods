import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("repo.base-conventions")
    id("repo.licensed-library")
}

val libs = the<LibrariesForLibs>()
val javaVersion = libs.versions.java.get()

//==================================================
// Java
//==================================================

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion.toInt())
    withSourcesJar()
    withJavadocJar()
}

// More lenient than `repo.java-library-conventions` — no -Werror.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion.toInt()
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-deprecation",
            "-Xlint:-processing",
            "-parameters",
        ),
    )
}

//==================================================
// Mod extension
//==================================================

abstract class ModExtension @Inject constructor(objects: ObjectFactory) {
    abstract val version: Property<String>
    abstract val minecraftVersion: Property<String>
    abstract val fabricLoaderVersion: Property<String>

    val variants: NamedDomainObjectContainer<ModdedVariantSpec> =
        objects.domainObjectContainer(ModdedVariantSpec::class.java) { name ->
            objects.newInstance(ModdedVariantSpec::class.java, name)
        }
}

val modExt = extensions.create<ModExtension>("mod")
val variants = modExt.variants

//==================================================
// Mod metadata templating
//==================================================

tasks.withType<ProcessResources>().configureEach {
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

//==================================================
// Deferred wiring
//==================================================

afterEvaluate {
    project.version = "${modExt.minecraftVersion.get()}-${modExt.version.get()}"

    variants.forEach { variant ->
        val config = createVariantConfiguration(variant)
        val syncTask = registerVariantSyncTask(variant, config)
        plugins.withId("fabric-loom") { cloneLoomRuns(variant, syncTask) }
        plugins.withId("net.neoforged.moddev") { cloneModDevRuns(variant, syncTask) }
    }
}

//==================================================
// Helpers
//==================================================

/**
 * Zeroes the last dotted component of a version string:
 * `"0.16.14"` -> `"0.16.0"`.
 * If there is no `.` the input is returned unchanged.
 */
fun zeroLastComponent(version: String): String =
    if(!version.contains('.')) version
    else version.substringBeforeLast('.') + ".0"

/**
 * Dedicated resolvable-only configuration that holds the variant's mod jars.
 * Non-transitive so pulling in published transitives (fabric-api, kotlin, etc.)
 * doesn't duplicate jars already on the dev classpath.
 */
fun Project.createVariantConfiguration(variant: ModdedVariantSpec): Configuration {
    val configName = "${variant.name}DevRuntime"
    val config = configurations.create(configName) {
        isCanBeResolved = true
        isCanBeConsumed = false
        isTransitive = false
        description = "Mod jars staged into ${variant.gameDir}/mods/ for the ${variant.name} run variant."
    }
    variant.getModCoordinates().forEach { coord ->
        dependencies.add(configName, coord)
    }
    return config
}

/**
 * Registers a `sync<Name>ToRun` task that materializes the variant's mod jars
 * into `<gameDir>/mods/` before the run launches.
 */
fun Project.registerVariantSyncTask(
    variant: ModdedVariantSpec,
    config: Configuration,
): TaskProvider<Sync> {
    val capitalized = variant.name.replaceFirstChar { it.uppercaseChar() }
    val syncTaskName = "sync${capitalized}ToRun"
    return tasks.register<Sync>(syncTaskName) {
        group = "modded variants"
        description = "Syncs the ${variant.name} modded variant's mods into ${variant.gameDir}/mods/."
        from(config)
        into(layout.projectDirectory.dir(variant.gameDir).dir("mods"))
    }
}

/**
 * Fabric Loom cloner. `RunConfigSettings` exposes `inherit(other)`, so cloning
 * each base run under the variant's `gameDir` is a one-liner.
 */
fun Project.cloneLoomRuns(variant: ModdedVariantSpec, syncTask: TaskProvider<Sync>) {
    val baseRunNames = variant.getBaseRunNames()
    if(baseRunNames.isEmpty()) return

    val loom = extensions.getByType(LoomGradleExtensionAPI::class.java)
    val capitalized = variant.name.replaceFirstChar { it.uppercaseChar() }
    baseRunNames.forEach { baseRunName ->
        val variantRunName = "${baseRunName}${capitalized}"
        loom.runs.register(variantRunName) {
            inherit(loom.runs.getByName(baseRunName))
            runDir(variant.gameDir)
        }
        wireRunTaskDependency(variantRunName, syncTask.name)
    }
}

/**
 * ModDevGradle cloner. `RunModel` has no `inherit`, so we copy the essentials
 * (client/server mode + `loadedMods`) and override the game directory.
 */
fun Project.cloneModDevRuns(variant: ModdedVariantSpec, syncTask: TaskProvider<Sync>) {
    val baseRunNames = variant.getBaseRunNames()
    if(baseRunNames.isEmpty()) return

    val mdg = extensions.getByType(NeoForgeExtension::class.java)
    val capitalized = variant.name.replaceFirstChar { it.uppercaseChar() }
    baseRunNames.forEach { baseRunName ->
        val variantRunName = "${baseRunName}${capitalized}"
        val baseRun = mdg.runs.getByName(baseRunName)
        mdg.runs.register(variantRunName) {
            when(baseRun.type.orNull) {
                "client" -> client()
                "server" -> server()
            }
            gameDirectory.set(layout.projectDirectory.dir(variant.gameDir))
            loadedMods.addAll(baseRun.loadedMods)
        }
        wireRunTaskDependency(variantRunName, syncTask.name)
    }
}

/**
 * The actual run *task* is `run<RunConfigName>` (Loom prefixes with `run`; MDG
 * does the same). Wire its dependency on the staging task by name.
 */
fun Project.wireRunTaskDependency(variantRunName: String, syncTaskName: String) {
    val taskName = "run${variantRunName.replaceFirstChar { it.uppercaseChar() }}"
    tasks.named(taskName) {
        dependsOn(syncTaskName)
    }
}
