plugins {
    id("oy-mod")
    id("net.neoforged.moddev") version "2.0.141"
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
    // Kotlin For Forge — YACL's NG variant transitively depends on it.
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "KotlinForForge"
        content { includeGroup("thedarkcolour") }
    }
}

val mcVersion = "1.21.8"
val neoforgeVer = "21.8.53"

val yaclVersion = "3.8.2+"

// MC 1.21.8 strict transitive pins — mirror coal-neoforge.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if(requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
            useVersion("3.17.0")
            because("MC 1.21.8 strictly pins commons-lang3 to 3.17.0")
        }
        if(requested.group == "org.slf4j" && requested.name == "slf4j-api") {
            useVersion("2.0.16")
            because("MC 1.21.8 strictly pins slf4j-api to 2.0.16")
        }
    }
}

mod {
    id = "coal_yacl_adapter"
    version = providers.gradleProperty("coal.version").get()
    name = "COAL — YACL adapter (NeoForge)"
    description = "COAL provider adapter backed by YetAnotherConfigLib (GUI + gson JSON persistence)."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    neoforgeVersion = neoforgeVer
}

// coal-* subprojects the adapter references directly.
val adapterSubprojectPaths =
    listOf(
        ":libraries:coal:coal-api",
        ":libraries:coal:coal-api-gui-neoforge",
        ":libraries:coal:coal-adapter-common",
    )

// coal-* subprojects that make up the `coal` mod at dev time. Registered as a
// separate mod (below) so NG discovers NeoForgePlatform via ServiceLoader.
val coalModSubprojectPaths =
    listOf(
        ":libraries:coal:coal-neoforge",
        ":libraries:coal:coal-noop",
        ":libraries:coal:coal-api-sync",
    )

(adapterSubprojectPaths + coalModSubprojectPaths).forEach { evaluationDependsOn(it) }

// Declare the testmod source set before the neoForge{runs{...}} block references
// it — NG's DSL resolves sourceSets eagerly at configuration time.
val testmod: SourceSet =
    sourceSets.create("testmod") {
        compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
    }

neoForge {
    version = neoforgeVer

    runs {
        register("client") {
            client()
            gameDirectory = layout.projectDirectory.dir("run-client")
        }
        register("testmodClient") {
            client()
            gameDirectory = layout.projectDirectory.dir("run-testmod-client")
            sourceSet = testmod
        }
    }

    mods {
        register("coal_yacl_adapter") {
            sourceSet(sourceSets.main.get())
            // Common adapter classes live here so their packages have exactly
            // one exporting module.
            sourceSet(project(":libraries:coal:coal-adapter-common").sourceSets.main.get())
            // Do NOT fold coal-api / coal-api-gui-neoforge here — the "coal"
            // mod owns them (below). Registering the same package under two
            // mods triggers JPMS split-package resolution errors.
        }
        // Fold the COAL mod's dev-time sources into their own registered mod
        // so ServiceLoader finds NeoForgePlatform + coal-noop's factory, and
        // so coal-api / coal-api-gui-neoforge packages have exactly one
        // exporting module.
        register("coal") {
            coalModSubprojectPaths.forEach { path ->
                sourceSet(project(path).sourceSets.main.get())
            }
            sourceSet(project(":libraries:coal:coal-api").sourceSets.main.get())
            sourceSet(project(":libraries:coal:coal-api-gui-neoforge").sourceSets.main.get())
        }
        register("coal_yacl_adapter_testmod") {
            sourceSet(testmod)
        }
    }
}

dependencies {
    // YACL — hard runtime dep.
    implementation("dev.isxander:yet-another-config-lib:$yaclVersion")

    api(projects.libraries.coal.coalApi)
    api(projects.libraries.coal.coalApiGuiNeoforge)
    // MC-free loader-independent adapter code (schema reader, IO, config
    // manager, validators, provider + factory, event bus, YaclScreenSupport
    // helpers). Loader modules keep only the MC/YACL-typed classes.
    api(projects.libraries.coal.coalAdapterCommon)
    jarJar(projects.libraries.coal.coalAdapterCommon)

    // gson for the JSON codec + commons-language reflection helpers.
    implementation(libs.gson)
    implementation(libs.oliveryasuna.commonsLanguage)

    jarJar(libs.gson)
    jarJar(libs.oliveryasuna.commonsLanguage)

    // Dev-runtime only. The published jar does NOT bundle these — end users
    // install coal.jar separately.
    runtimeOnly(projects.libraries.coal.coalNeoforge)
    runtimeOnly(projects.libraries.coal.coalNoop)
    runtimeOnly(projects.libraries.coal.coalApiSync)

    "testmodImplementation"(sourceSets.main.get().output)

    // In dev, JiJ contents aren't unpacked. External libraries need to be on
    // the additional runtime classpath so the mod's classloader can see them.
    "additionalRuntimeClasspath"(libs.oliveryasuna.commonsLanguage)
    "additionalRuntimeClasspath"(libs.gson)
}
