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
    // Mod Menu artifacts.
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
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

    variants {
        register("modMenu") {
            mods("com.terraformersmc:modmenu:15.0.2")
            applyTo("client", "testmodClient")
        }
    }
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

    // Dev-runtime only: the COAL mod ships FabricPlatform via
    // META-INF/services. Without it Coal.bootstrap() throws
    // ProviderNotFoundException. NOT bundled in the published adapter
    // jar — end users install coal.jar themselves.
    //
    // isTransitive = false: coal-fabric's published POM references
    // coal-noop / coal-api-sync as Maven GAVs (they're api(...) in its build),
    // so Gradle would try to resolve them from Maven Central. Turning off
    // transitives + adding the plain-library jars via runtimeOnly below keeps
    // resolution project-local.
    modLocalRuntime(projects.libraries.coal.coalFabric) { isTransitive = false }

    // coal-noop is a plain library jar (no fabric.mod.json) that COAL's
    // ServiceLoader needs on classpath to discover the last-resort provider.
    // Not a mod → use runtimeOnly, not modLocalRuntime.
    runtimeOnly(projects.libraries.coal.coalNoop)
    // coal-api-sync is transitively required by coal-fabric's compiled code.
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
        register("coal_yacl_adapter_testmod") {
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
