plugins {
    id("oy-mod")
    id("fabric-loom") version "1.17.13"
}

val mcVersion = "1.21.8"
val fabricLoaderVer = "0.19.3"
val fabricApiVersion = "0.136.1+1.21.8"

mod {
    id = "coal"
    version = providers.gradleProperty("coal.version").get()
    name = "COAL"
    description = "Config Options Abstraction Layer — SLF4J-style config-library abstraction for Minecraft mods."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    fabricLoaderVersion = fabricLoaderVer
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVer")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // api() — consumers need to compile against the types.
    api(projects.libraries.coal.coalApi)
    api(projects.libraries.coal.coalApiGuiFabric)
    api(projects.libraries.coal.coalApiSync)
    api(projects.libraries.coal.coalNoop)

    // include() — JiJ-bundle into the shipped coal.jar.
    include(projects.libraries.coal.coalApi)
    include(projects.libraries.coal.coalApiGuiFabric)
    include(projects.libraries.coal.coalApiSync)
    include(projects.libraries.coal.coalNoop)
    include(libs.oliveryasuna.commonsLanguage)

    implementation(libs.oliveryasuna.commonsLanguage)
}
