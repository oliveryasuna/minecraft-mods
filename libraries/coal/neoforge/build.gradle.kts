plugins {
    id("oy-mod")
    id("net.neoforged.moddev") version "2.0.141"
}

val mcVersion = "1.21.8"
val neoforgeVer = "21.8.53"

// MC 1.21.8 declares `strictly` constraints on transitive libs; useVersion
// bypasses them.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
            useVersion("3.17.0")
            because("MC 1.21.8 strictly pins commons-lang3 to 3.17.0")
        }
        if (requested.group == "org.slf4j" && requested.name == "slf4j-api") {
            useVersion("2.0.16")
            because("MC 1.21.8 strictly pins slf4j-api to 2.0.16")
        }
    }
}

mod {
    id = "coal"
    version = providers.gradleProperty("coal.version").get()
    name = "COAL"
    description = "Config Options Abstraction Layer — SLF4J-style config-library abstraction for Minecraft mods."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    neoforgeVersion = neoforgeVer
}

// coal-* subprojects the mod bundles — bound to the `coal` mod's classloader
// boundary in dev via the `sourceSet(...)` calls below. In prod the jarJar
// mechanism handles the same content.
val coalSubprojectPaths =
    listOf(
        ":libraries:coal:coal-api",
        ":libraries:coal:coal-api-gui-neoforge",
        ":libraries:coal:coal-api-sync",
        ":libraries:coal:coal-noop",
    )

coalSubprojectPaths.forEach { evaluationDependsOn(it) }

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
    }

    mods {
        register("coal") {
            sourceSet(sourceSets.main.get())
            coalSubprojectPaths.forEach { path ->
                sourceSet(project(path).sourceSets.main.get())
            }
        }
    }
}

dependencies {
    api(projects.libraries.coal.coalApi)
    api(projects.libraries.coal.coalApiGuiNeoforge)
    api(projects.libraries.coal.coalApiSync)
    api(projects.libraries.coal.coalNoop)

    jarJar(projects.libraries.coal.coalApi)
    jarJar(projects.libraries.coal.coalApiGuiNeoforge)
    jarJar(projects.libraries.coal.coalApiSync)
    jarJar(projects.libraries.coal.coalNoop)
    jarJar(libs.oliveryasuna.commonsLanguage)

    implementation(libs.oliveryasuna.commonsLanguage)

    // In dev, JiJ contents aren't unpacked. External libraries need to be on
    // the additional runtime classpath so the mod's classloader can see them.
    "additionalRuntimeClasspath"(libs.oliveryasuna.commonsLanguage)
}
