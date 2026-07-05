plugins {
    id("oy-mod")
    id("net.neoforged.moddev") version "2.0.141"
}

repositories {
    maven("https://maven.shedaniel.me/") {
        name = "Shedaniel"
        content { includeGroup("me.shedaniel.cloth") }
    }
}

val mcVersion = "1.21.8"
val neoforgeVer = "21.8.53"

val clothVersion = "19.0.147"

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
    id = "coal_cloth_adapter"
    version = providers.gradleProperty("coal.version").get()
    name = "COAL — Cloth Config adapter (NeoForge)"
    description = "COAL provider adapter backed by Cloth Config (GUI + gson JSON persistence)."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    neoforgeVersion = neoforgeVer
}

val adapterSubprojectPaths =
    listOf(
        ":libraries:coal:coal-api",
        ":libraries:coal:coal-api-gui-neoforge",
        ":libraries:coal:coal-adapter-common",
    )

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
        register("coal_cloth_adapter") {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":libraries:coal:coal-adapter-common").sourceSets.main.get())
        }
        register("coal_cloth_adapter_testmod") {
            sourceSet(testmod)
        }
        // Fold the COAL mod's dev-time sources into their own registered mod
        // so ServiceLoader finds NeoForgePlatform + coal-noop's factory, and so
        // coal-api / coal-api-gui-neoforge packages have exactly one exporting
        // module.
        register("coal") {
            coalModSubprojectPaths.forEach { path ->
                sourceSet(project(path).sourceSets.main.get())
            }
            sourceSet(project(":libraries:coal:coal-api").sourceSets.main.get())
            sourceSet(project(":libraries:coal:coal-api-gui-neoforge").sourceSets.main.get())
        }
    }
}

dependencies {
    // Cloth Config — hard runtime dep.
    implementation("me.shedaniel.cloth:cloth-config-neoforge:$clothVersion")

    api(projects.libraries.coal.coalApi)
    api(projects.libraries.coal.coalApiGuiNeoforge)
    api(projects.libraries.coal.coalAdapterCommon)
    jarJar(projects.libraries.coal.coalAdapterCommon)

    implementation(libs.gson)
    implementation(libs.oliveryasuna.commonsLanguage)

    jarJar(libs.gson)
    jarJar(libs.oliveryasuna.commonsLanguage)

    // Dev-runtime only.
    runtimeOnly(projects.libraries.coal.coalNeoforge)
    runtimeOnly(projects.libraries.coal.coalNoop)
    runtimeOnly(projects.libraries.coal.coalApiSync)

    "additionalRuntimeClasspath"(libs.oliveryasuna.commonsLanguage)
    "additionalRuntimeClasspath"(libs.gson)

    "testmodImplementation"(sourceSets.main.get().output)
}
