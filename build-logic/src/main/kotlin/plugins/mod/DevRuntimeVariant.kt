package plugins.mod

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Declares one dev-runtime "variant" — a parallel set of run tasks that launch
 * with a different game directory and stage a defined set of mod jars into
 * `<gameDir>/mods/` before launch.
 */
abstract class DevRuntimeVariant @Inject constructor(
    private val variantName: String,
    private val siblings: NamedDomainObjectContainer<DevRuntimeVariant>,
) : Named {

    abstract val gameDir: Property<String>
    abstract val modCoordinates: ListProperty<String>
    abstract val baseRunNames: ListProperty<String>

    init {
        gameDir.convention("run-${variantName}")
    }

    /**
     * Mod-jar coordinate strings staged into the variant's game directory
     * before launch.
     */
    fun mods(vararg coordinates: String) {
        modCoordinates.addAll(*coordinates)
    }

    /**
     * Version-catalog overload of [mods] — appends a lazy Provider so catalog
     * resolution is deferred until the configuration is actually resolved.
     */
    fun mods(vararg deps: Provider<MinimalExternalModuleDependency>) {
        deps.forEach { provider ->
            modCoordinates.add(provider.map { dep ->
                "${dep.module.group}:${dep.module.name}:${dep.versionConstraint.requiredVersion}"
            })
        }
    }

    /**
     * Appends each given variant's mod coordinates to this variant's — lazily,
     * so ordering of `register(...)` calls doesn't matter.
     */
    fun extends(vararg others: NamedDomainObjectProvider<DevRuntimeVariant>) {
        others.forEach { other ->
            modCoordinates.addAll(other.flatMap { it.modCoordinates })
        }
    }

    /**
     * String-name overload of [extends]. Resolution is lazy — the named
     * variants only have to exist by the time the configuration is queried, so
     * ordering of `register(...)` calls doesn't matter.
     */
    fun extends(vararg names: String) {
        names.forEach { name -> extends(siblings.named(name)) }
    }

    /**
     * Names of base run configs the variant should mirror. For each base run,
     * a clone is registered with the variant suffix and the variant's game
     * directory. If omitted, only the staging task is registered.
     */
    fun applyTo(vararg names: String) {
        baseRunNames.addAll(*names)
    }

    override fun getName(): String = variantName
}
