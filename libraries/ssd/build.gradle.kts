plugins {
    id("oy-mod")
    id("fabric-loom") version "1.17.13"
}

val mcVersion = "1.21.8"
val fabricLoaderVer = "0.16.14"
val fabricApiVersion = "0.136.1+1.21.8"

mod {
    id = "seven-segment-display"
    version = providers.gradleProperty("coal.version").get()
    name = "Seven Segment Display"
    description = "A seven-segment display for Minecraft."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    fabricLoaderVersion = fabricLoaderVer
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVer")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    include(libs.oliveryasuna.commonsLanguage)

    implementation(libs.oliveryasuna.commonsLanguage)
}
