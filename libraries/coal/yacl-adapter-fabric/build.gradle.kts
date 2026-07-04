plugins {
    id("oy-mod")
    id("fabric-loom") version "1.17.13"
}

repositories {
    // YACL artifacts.
    maven("https://maven.isxander.dev/releases") {
        name = "IsxanderReleases"
        content { includeGroup("dev.isxander") }
    }
    // YACL transitively depends on org.quiltmc.parsers:{json,gson}.
    maven("https://maven.quiltmc.org/repository/release/") {
        name = "QuiltReleases"
        content { includeGroup("org.quiltmc.parsers") }
    }
}

val mcVersion = "1.21.8"
val fabricLoaderVer = "0.16.14"
val fabricApiVersion = "0.136.1+1.21.8"

val yaclVersion = "3.7.0+1.21.6-fabric"

mod {
    id = "coal_yacl_adapter"
    version = providers.gradleProperty("coal.version").get()
    name = "COAL — YACL adapter"
    description = "COAL provider adapter backed by YetAnotherConfigLib (GUI + gson JSON persistence)."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    fabricLoaderVersion = fabricLoaderVer
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVer")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // YACL — hard runtime dep; the whole point of this adapter.
    modImplementation("dev.isxander:yet-another-config-lib:$yaclVersion")

    // COAL: consumers see coal-api / coal-api-gui-fabric transitively via the
    //       COAL mod. Here we need them for compile.
    api(projects.libraries.coal.coalApi)
    api(projects.libraries.coal.coalApiGuiFabric)

    // gson: JSON persistence for the ConfigIO implementation.
    implementation(libs.gson)

    implementation(libs.oliveryasuna.commonsLanguage)
    include(libs.oliveryasuna.commonsLanguage)
    include(libs.gson)
}
