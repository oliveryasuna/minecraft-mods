plugins {
    id("oy-mod")
    id("net.neoforged.moddev") version "2.0.141"
}

repositories {
    // ModDevGradle installs its own project-level repos (NeoForge userdev) so
    // PREFER_PROJECT mode is fine for the settings-level mirrors — the
    // frontends below are only dev-runtime deps.
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
    maven("https://www.cursemaven.com") {
        name = "CurseMaven"
        content { includeGroup("curse.maven") }
    }
    maven("https://maven.isxander.dev/releases") {
        name = "IsxanderReleases"
        content { includeGroup("dev.isxander") }
    }
    // YACL transitively depends on org.quiltmc.parsers:{json,gson}.
    maven("https://maven.quiltmc.org/repository/release/") {
        name = "QuiltReleases"
        content { includeGroup("org.quiltmc.parsers") }
    }
    maven("https://maven.shedaniel.me/") {
        name = "ShedanielMaven"
        content { includeGroup("me.shedaniel.cloth") }
    }
}

val mcVersion = "1.21.8"
val neoforgeVer = "21.8.53"

val yaclVersion = "3.7.0+1.21.6-neoforge"
val clothVersion = "19.0.147"
val catalogueFileId = "6926819"

// MC 1.21.8 declares `strictly` constraints on several transitive libs
// (commons-lang3, slf4j-api, ...). Rubric's non-loader modules pull newer
// versions and NG's userdev refuses to merge — resolution fails at
// `runClient` time. Override our requests to match MC's pins so the runtime
// classpath resolves cleanly. `useVersion` bypasses strict constraints
// (unlike `force`).
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
    id = "rubric"
    version = providers.gradleProperty("rubric.version").get()
    name = "Rubric"
    description = "Multi-loader Minecraft mod configuration library."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    neoforgeVersion = neoforgeVer

    variants {
        register("catalogue") {
            mods("curse.maven:catalogue-459701:$catalogueFileId")
            applyTo("client", "testmodClient")
        }

        val yacl =
            register("yacl") {
                mods("dev.isxander:yet-another-config-lib:$yaclVersion")
                applyTo("client", "testmodClient")
            }

        val cloth =
            register("cloth") {
                mods("me.shedaniel.cloth:cloth-config-neoforge:$clothVersion")
                applyTo("client", "testmodClient")
            }

        register("yaclCloth") {
            extends(yacl, cloth)
            applyTo("client", "testmodClient")
        }
    }
}

// rubric-* subprojects that get their sourceSets bound to the `rubric` mod's
// classloader boundary below. Kept as a top-level list so we can pre-force
// project evaluation up-front (IntelliJ's model fetch calls afterEvaluate on
// this project mid-sync; calling `evaluationDependsOn` from inside NG's own
// afterEvaluate block violates Gradle's mutation guard).
val rubricSubprojectPaths =
    listOf(
        ":libraries:util",
        ":libraries:rubric:rubric-api",
        ":libraries:rubric:rubric-migration",
        ":libraries:rubric:rubric-model",
        ":libraries:rubric:rubric-io",
        ":libraries:rubric:rubric-core",
        ":libraries:rubric:rubric-format",
        ":libraries:rubric:rubric-sync",
        ":libraries:rubric:rubric-mojang-codec",
        ":libraries:rubric:rubric-loader-common",
    )

rubricSubprojectPaths.forEach { evaluationDependsOn(it) }

// Declare the `testmod` source set before the `neoForge { runs { ... } }` block
// references it — NG's DSL resolves sourceSets eagerly at configuration time,
// unlike Loom.
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
        register("server") {
            server()
            gameDirectory = layout.projectDirectory.dir("run-server")
        }
        register("testmodClient") {
            client()
            gameDirectory = layout.projectDirectory.dir("run-client")
            sourceSet = testmod
        }
        register("testmodServer") {
            server()
            gameDirectory = layout.projectDirectory.dir("run-server")
            sourceSet = testmod
        }
    }

    mods {
        register("rubric") {
            sourceSet(sourceSets.main.get())
            // Every rubric-* subproject is a plain library from NeoForge's
            // point of view. In dev, ModDev's classloader isolates each mod;
            // library projects on runtimeClasspath aren't visible unless
            // bound to the mod here. (In production the jarJar mechanism
            // handles them.) `evaluationDependsOn` calls at the top of this
            // script ensure `sourceSets.main` is available on each project.
            rubricSubprojectPaths.forEach { path ->
                sourceSet(project(path).sourceSets.main.get())
            }
        }
        register("rubric-testmod") {
            sourceSet(testmod)
        }
    }
}

dependencies {
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricMigration)
    api(projects.libraries.rubric.rubricModel)
    api(projects.libraries.rubric.rubricIo)
    api(projects.libraries.rubric.rubricCore)
    api(projects.libraries.rubric.rubricFormat)
    api(projects.libraries.rubric.rubricSync)
    api(projects.libraries.rubric.rubricMojangCodec)
    api(projects.libraries.rubric.rubricLoaderCommon)

    jarJar(projects.libraries.rubric.rubricApi)
    jarJar(projects.libraries.rubric.rubricMigration)
    jarJar(projects.libraries.rubric.rubricModel)
    jarJar(projects.libraries.rubric.rubricIo)
    jarJar(projects.libraries.rubric.rubricCore)
    jarJar(projects.libraries.rubric.rubricFormat)
    jarJar(projects.libraries.rubric.rubricSync)
    jarJar(projects.libraries.rubric.rubricMojangCodec)
    jarJar(projects.libraries.rubric.rubricLoaderCommon)
    jarJar(projects.libraries.util)
    // night-config-core/toml NOT jarJar'd: NeoForge ships these as game
    // libraries. Bundling our own copy produces two JPMS modules with the
    // same Automatic-Module-Name → ResolutionException in Prism at boot.
    // night-config-json IS bundled — NG doesn't ship it.
    jarJar(libs.nightconfig.json)
    jarJar(libs.jankson)
    jarJar(libs.oliveryasuna.commonsLanguage)

    compileOnly("dev.isxander:yet-another-config-lib:$yaclVersion")
    compileOnly("me.shedaniel.cloth:cloth-config-neoforge:$clothVersion")

    implementation(libs.oliveryasuna.commonsLanguage)

    // In dev, JiJ contents aren't unpacked (mods run from classes/ dirs, not
    // packaged jars). External libraries must be placed on NG's additional
    // runtime classpath so the mod's classloader can see them. In production
    // the jarJar declarations above handle the same content.
    "additionalRuntimeClasspath"(libs.oliveryasuna.commonsLanguage)
    "additionalRuntimeClasspath"(libs.nightconfig.core)
    "additionalRuntimeClasspath"(libs.nightconfig.toml)
    "additionalRuntimeClasspath"(libs.nightconfig.json)
    "additionalRuntimeClasspath"(libs.jankson)
}

dependencies {
    "testmodImplementation"(sourceSets.main.get().output)
}
