dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // `workshop.modded-variants` compiles against Fabric Loom and
        // ModDevGradle types so it can configure both loaders' run DSLs.
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
