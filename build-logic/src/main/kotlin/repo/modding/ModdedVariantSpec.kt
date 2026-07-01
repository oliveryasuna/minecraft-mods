package repo.modding

import org.gradle.api.Named
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Declares one dev-runtime "variant" — a parallel set of run tasks that launch
 * with a different game directory and have a defined set of mod jars staged
 * into `<gameDir>/mods/` before launch.
 */
abstract class ModdedVariantSpec @Inject constructor(
    private val variantName: String
) : Named {

    //==================================================
    // Fields
    //==================================================

    /**
     * Game directory, relative to the project root.
     */
    var gameDir: String = "run-${variantName}"

    private val mutableMods = mutableListOf<String>()
    private val mutableApplyTo = mutableListOf<String>()

    //==================================================
    // Methods
    //==================================================

    /**
     * Mod-jar coordinate strings staged into the variant's game directory
     * before launch.
     */
    fun mods(vararg coordinates: String) {
        mutableMods.addAll(coordinates)
    }

    /**
     * Version-catalog overload of [mods]; resolves each [deps] entry to a
     * coordinate string and forwards to the `vararg String` overload.
     *
     * @see mods
     */
    fun mods(vararg deps: Provider<MinimalExternalModuleDependency>) {
        deps.forEach { provider ->
            val dep = provider.get()
            val coordinate = "${dep.module.group}:${dep.module.name}:${dep.versionConstraint.requiredVersion}"
            mods(coordinate)
        }
    }

    /**
     * Names of base run configs the variant should mirror.
     *
     * For each base run, a clone is registered with the variant suffix and the
     * variant's game directory. Example: `applyTo("client", "testmodClient")`
     * produces `runClient<Name>` and `runTestmodClient<Name>`.
     *
     * If omitted, only the staging task is registered — the consuming build
     * script wires the runs by hand. Useful when none of the existing runs is
     * the right starting point.
     */
    fun applyTo(vararg baseRunNames: String) {
        mutableApplyTo.addAll(baseRunNames)
    }

    override fun getName(): String = variantName

    /** Plugin internal accessor. */
    fun getModCoordinates(): List<String> = mutableMods.toList()

    /** Plugin internal accessor. */
    fun getBaseRunNames(): List<String> = mutableApplyTo.toList()

}
