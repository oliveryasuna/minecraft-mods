plugins {
    id("oy-mod")
    id("fabric-loom") version "1.17.14"
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
    //       COAL mod. Here we need them for compile + dev runtime.
    //
    // coal-api is MC-free -> plain api(...).
    // coal-api-gui-fabric is a Loom project whose published (maven) variant is
    // intermediary-remapped. Consuming it via `implementation(project(...))`
    // would layer that intermediary bytecode onto the runtimeClasspath —
    // sibling with the properly-remapped copy Loom exposes via `mods.register`
    // below — and the JVM loads whichever wins first, crashing testmod with
    // NoSuchMethodError on GuiRegistry.open.
    //
    // Instead we depend on the raw sourceSet outputs directly: same Mojmap
    // classes Loom compiled, no maven-variant remap. Loom's `mods.register`
    // block below folds them into this mod's dev classloader.
    api(projects.libraries.coal.coalApi)

    // MC-free loader-independent adapter code (schema reader, IO, config
    // manager, validators, provider + factory, event bus, YaclScreenSupport
    // helpers). Loader modules keep only the MC/YACL-typed classes.
    api(projects.libraries.coal.coalAdapterCommon)
    include(projects.libraries.coal.coalAdapterCommon)

    // Explicit `namedElements` variant: Loom exposes this as the
    // Mojmap-compiled jar (from the raw `jar` task, NOT `remapJar`). The
    // default variant would resolve to the vanniktech maven-publish view,
    // which is intermediary-remapped. namedElements is what other Loom
    // projects should consume for dev-time classpath.
    implementation(
        project(
            mapOf(
                "path" to ":libraries:coal:coal-api-gui-fabric",
                "configuration" to "namedElements",
            ),
        ),
    )

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
    // Not a mod -> use runtimeOnly, not modLocalRuntime.
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
        // Register OUR mod's dev classloader with the coal-api-gui-fabric
        // sourceSet folded in. Loom then remaps those classes to Mojmap for dev
        // runtime and exposes them via our mod's classloader so
        // YaclAdapterFabricClientMod can load ScreenProvider without hitting
        // Fabric's per-mod class isolation.
        register("coal_yacl_adapter") {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":libraries:coal:coal-api-gui-fabric").sourceSets.main.get())
        }
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
