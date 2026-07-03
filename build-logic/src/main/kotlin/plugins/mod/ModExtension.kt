package plugins.mod

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ModExtension @Inject constructor(
    objects: ObjectFactory
) : ExtensionAware {

    abstract val id: Property<String>
    abstract val version: Property<String>
    abstract val name: Property<String>
    abstract val description: Property<String>

    // TODO: Add support for authors list, but for now just use a single author.
    abstract val author: Property<String>

    abstract val minecraftVersion: Property<String>
    abstract val fabricLoaderVersion: Property<String>
    abstract val neoforgeVersion: Property<String>

    // The factory needs a reference to the container so each variant can look
    // up its siblings by name (for `extend("...")`). The `lateinit` local
    // closes the cycle: we build the container, then thread it through the
    // factory closure — which only fires when `register(...)` is called, well
    // after the container is assigned.
    val variants: NamedDomainObjectContainer<DevRuntimeVariant> = run {
        lateinit var container: NamedDomainObjectContainer<DevRuntimeVariant>
        container = objects.domainObjectContainer(DevRuntimeVariant::class.java) { name ->
            objects.newInstance(DevRuntimeVariant::class.java, name, container)
        }
        container
    }

    init {
        extensions.add("variants", variants)
    }
}
