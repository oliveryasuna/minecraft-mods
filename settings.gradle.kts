pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // Fabric Loom + Fabric API artifacts.
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        // ModDevGradle + NeoForge artifacts.
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    // PREFER_PROJECT — Fabric Loom and ModDevGradle install their own
    // project-level repositories (synthetic flat-dirs for remapped MC
    // artifacts, NG userdev, etc.) and need them resolved before our global
    // mirrors. The non-loader modules still resolve cleanly from mavenCentral
    // because nothing they depend on lives in the loader repos.
    repositoriesMode = RepositoriesMode.PREFER_PROJECT
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "minecraft-mods"

include(
    "libraries:util",
    "libraries:coal:api",
    "libraries:coal:api-gui-fabric",
    "libraries:coal:api-gui-neoforge",
    "libraries:coal:api-sync",
    "libraries:coal:noop",
    "libraries:rubric:api",
    "libraries:rubric:core",
    "libraries:rubric:fabric",
    "libraries:rubric:format",
    "libraries:rubric:io",
    "libraries:rubric:loader-common",
    "libraries:rubric:migration",
    "libraries:rubric:model",
    "libraries:rubric:mojang-codec",
    "libraries:rubric:neoforge",
    "libraries:rubric:sync"
)

rootProject.children.single { it.name == "libraries" }.children.forEach { family ->
    family.children.forEach { module ->
        module.name = "${family.name}-${module.name}"
    }
}
