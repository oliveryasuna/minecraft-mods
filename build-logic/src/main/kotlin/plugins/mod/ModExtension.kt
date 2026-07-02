package plugins.mod

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ModExtension @Inject constructor(
    objects: ObjectFactory
) : ExtensionAware {

    abstract val version: Property<String>
    abstract val minecraftVersion: Property<String>
    abstract val fabricLoaderVersion: Property<String>

    val variants: NamedDomainObjectContainer<DevRuntimeVariant> =
        objects.domainObjectContainer(DevRuntimeVariant::class.java) { name ->
            objects.newInstance(DevRuntimeVariant::class.java, name)
        }

    init {
        extensions.add("variants", variants)
    }
}
