plugins {
    id("oy-mod")
    id("fabric-loom") version "1.17.13"
}

repositories {
    maven("https://maven.isxander.dev/releases") {
        name = "IsxanderReleases"
        content { includeGroup("dev.isxander") }
    }
    maven("https://maven.quiltmc.org/repository/release/") {
        name = "QuiltReleases"
        content { includeGroup("org.quiltmc.parsers") }
    }
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
}

val mcVersion = "1.21.8"
val fabricLoaderVer = "0.16.14"
val fabricApiVersion = "0.136.1+1.21.8"

val yaclVersion = "3.7.0+1.21.6-fabric"
val modMenuVersion = "15.0.2"

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

    //==================================================
    // COAL
    //==================================================

    compileOnly(projects.libraries.coal.coalApi)

    implementation(
        project(
            mapOf(
                "path" to ":libraries:coal:coal-api-gui-fabric",
                "configuration" to "namedElements",
            ),
        ),
    )

    modImplementation("com.terraformersmc:modmenu:$modMenuVersion")

    //==================================================
    // Dev runtime — the COAL provider stack + GUI
    //==================================================

    modLocalRuntime(projects.libraries.coal.coalFabric) { isTransitive = false }
    modLocalRuntime(projects.libraries.coal.coalYaclAdapterFabric) { isTransitive = false }

    runtimeOnly(projects.libraries.coal.coalAdapterCommon)

    modLocalRuntime("dev.isxander:yet-another-config-lib:$yaclVersion")

    runtimeOnly(projects.libraries.coal.coalNoop)
    runtimeOnly(projects.libraries.coal.coalApiSync)
}
