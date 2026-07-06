plugins {
    id("oy-mod")
    id("fabric-loom") version "1.17.13"
}

repositories {
    maven("https://maven.shedaniel.me/") {
        name = "Shedaniel"
        content { includeGroup("me.shedaniel.cloth") }
    }
}

val mcVersion = "1.21.8"
val fabricLoaderVer = "0.19.3"
val fabricApiVersion = "0.136.1+1.21.8"

val clothVersion = "19.0.147"

mod {
    id = "coal_cloth_adapter"
    version = providers.gradleProperty("coal.version").get()
    name = "COAL — Cloth Config adapter"
    description = "COAL provider adapter backed by Cloth Config (GUI + gson JSON persistence)."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    fabricLoaderVersion = fabricLoaderVer
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVer")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Cloth Config — hard runtime dep; the whole point of this adapter.
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:$clothVersion") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    // MC-free loader-independent adapter core (shared with the YACL adapter):
    // schema reader, JSON I/O, ConfigProvider, validators, event bus, config
    // manager, AdapterScreenSupport helpers.
    api(projects.libraries.coal.coalApi)
    api(projects.libraries.coal.coalAdapterCommon)
    include(projects.libraries.coal.coalAdapterCommon)

    // coal-api-gui-fabric: see coal-yacl-adapter-fabric for the rationale on
    // consuming its `namedElements` variant explicitly.
    implementation(
        project(
            mapOf(
                "path" to ":libraries:coal:coal-api-gui-fabric",
                "configuration" to "namedElements",
            ),
        ),
    )

    implementation(libs.gson)
    implementation(libs.oliveryasuna.commonsLanguage)
    include(libs.oliveryasuna.commonsLanguage)
    include(libs.gson)

    // Dev-runtime only: coal-fabric ships FabricPlatform via META-INF/services.
    modLocalRuntime(projects.libraries.coal.coalFabric) { isTransitive = false }

    runtimeOnly(projects.libraries.coal.coalNoop)
    runtimeOnly(projects.libraries.coal.coalApiSync)
}

// ==================================================
// Testmod source set
// ==================================================

val testmod by sourceSets.creating {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

dependencies {
    "testmodImplementation"(sourceSets.main.get().output)
}

loom {
    mods {
        register("coal_cloth_adapter") {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":libraries:coal:coal-api-gui-fabric").sourceSets.main.get())
        }
        register("coal_cloth_adapter_testmod") {
            sourceSet(testmod)
        }
    }
    runs {
        register("testmodClient") {
            client()
            source(testmod)
            runDir = "run-testmod-client"
        }
    }
}
