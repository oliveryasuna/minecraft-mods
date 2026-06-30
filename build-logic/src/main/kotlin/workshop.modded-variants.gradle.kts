import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import workshop.modding.ModdedVariantSpec

val variants = objects.domainObjectContainer(ModdedVariantSpec::class.java) { name ->
    objects.newInstance(ModdedVariantSpec::class.java, name)
}

extensions.add("moddedVariants", variants)

afterEvaluate {
    variants.forEach { variant ->
        val capitalized = variant.name.replaceFirstChar { it.uppercaseChar() }
        val gameDirRelative = variant.gameDir
        val gameDirPath = layout.projectDirectory.dir(gameDirRelative)

        // 1. Dedicated config for the variant's mods. Resolved at sync time,
        //    never published. Non-transitive: the variant declares exactly
        //    which mods land in `<gameDir>/mods/`; pulling in published
        //    transitives (fabric-api, kotlin, etc.) would duplicate jars
        //    already on the dev classpath and risk double-loading (e.g.,
        //    if there are different versions of the same library).
        val configName = "${variant.name}DevRuntime"
        val variantConfig = configurations.create(configName) {
            isCanBeResolved = true
            isCanBeConsumed = false
            isTransitive = false
            description = "Mod jars staged into ${gameDirRelative}/mods/ for the ${variant.name} run variant."
        }
        variant.getModCoordinates().forEach { coord ->
            dependencies.add(configName, coord)
        }

        // 2. Copy task that materializes the config's jars into
        //    `<gameDir>/mods/` before the run launches.
        val syncTaskName = "sync${capitalized}ToRun"
        val syncTask = tasks.register(syncTaskName, Sync::class.java) {
            group = "modded variants"
            description = "Syncs the ${variant.name} modded variant's mods into ${gameDirRelative}/mods/."
            from(variantConfig)
            into(gameDirPath.dir("mods"))
        }

        val baseRunNames = variant.getBaseRunNames()
        if(baseRunNames.isEmpty()) {
            // No base runs to clone — the staging task is registered, and the
            // consumer is expected to wire it manually.
            return@forEach
        }

        // 3a. Fabric Loom path. `RunConfigSettings` has `inherit(other)`, so
        //     cloning is a one-liner.
        plugins.withId("fabric-loom") {
            val loom = extensions.getByType(LoomGradleExtensionAPI::class.java)
            baseRunNames.forEach { baseRunName ->
                val variantRunName = "${baseRunName}${capitalized}"
                loom.runs.register(variantRunName) {
                    inherit(loom.runs.getByName(baseRunName))
                    runDir(gameDirRelative)
                }
                wireRunTaskDependency(variantRunName, syncTask.name)
            }
        }

        // 3b. ModDevGradle path. `RunModel` has no `inherit`, so we copy the
        //     essentials (client/server mode and loadedMods) and override the
        //     game directory.
        plugins.withId("net.neoforged.moddev") {
            val mdg = extensions.getByType(NeoForgeExtension::class.java)
            baseRunNames.forEach { baseRunName ->
                val variantRunName = "${baseRunName}${capitalized}"
                val baseRun = mdg.runs.getByName(baseRunName)
                mdg.runs.register(variantRunName) {
                    when(baseRun.type.orNull) {
                        "client" -> client()
                        "server" -> server()
                    }
                    gameDirectory.set(gameDirPath)
                    loadedMods.addAll(baseRun.loadedMods)
                }
                wireRunTaskDependency(variantRunName, syncTask.name)
            }
        }
    }
}

/**
 * The actual run *task* is `run<RunConfigName>` (Loom prefixes with `run`; MDG
 * does the same). Wire it's dependencies on the staging task by name.
 */
fun Project.wireRunTaskDependency(variantRunName: String, syncTaskName: String) {
    val taskName = "run${variantRunName.replaceFirstChar { it.uppercaseChar() }}"
    tasks.named(taskName) {
        dependsOn(syncTaskName)
    }
}
