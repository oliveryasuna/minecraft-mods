package plugins.mod

import org.gradle.api.provider.Property

abstract class ModExtension {

    abstract val version: Property<String>
    abstract val minecraftVersion: Property<String>
    abstract val fabricLoaderVersion: Property<String>

}
