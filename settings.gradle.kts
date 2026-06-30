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

rootProject.name = "workshop"

include(
    "libraries:util",
    "libraries:omniconfig:api",
    "libraries:omniconfig:core",
    "libraries:omniconfig:fabric",
    "libraries:omniconfig:format-json",
    "libraries:omniconfig:format-json5",
    "libraries:omniconfig:format-nightconfig",
    "libraries:omniconfig:format-toml",
    "libraries:omniconfig:io-file",
    "libraries:omniconfig:io-spi",
    "libraries:omniconfig:migration",
    "libraries:omniconfig:mojang-codec",
    "libraries:omniconfig:schema",
    "libraries:omniconfig:sync",
    "libraries:omniconfig:sync-protocol",
    "libraries:omniconfig:validation",
    "libraries:omniconfig:value"
)

rootProject.children.single { it.name == "libraries" }.children.forEach { family ->
    family.children.forEach { module ->
        module.name = "${family.name}-${module.name}"
    }
}
