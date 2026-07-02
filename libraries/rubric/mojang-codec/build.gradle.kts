plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

repositories {
    // The root `dependencyResolutionManagement` uses PREFER_PROJECT (so
    // Loom/MDG synthetic repos resolve before global mirrors); the moment a
    // project-level `repositories {}` block is declared, Gradle stops
    // consulting the settings-level list for THIS project. Redeclare the
    // shared mirrors here so DFU's transitives (fastutil, jsr305) still
    // resolve from Maven Central.
    mavenCentral()
    // Mojang publishes DFU to their own libraries repo, not Maven Central.
    // Content-filtered so only the Mojang group resolves here.
    exclusiveContent {
        forRepository {
            maven("https://libraries.minecraft.net/") { name = "Mojang" }
        }
        filter {
            includeGroup("com.mojang")
        }
    }
}

dependencies {
    api(projects.libraries.rubric.rubricValue)

    compileOnly(libs.datafixerupper)
    compileOnly(libs.gson)

    implementation(libs.oliveryasuna.commonsLanguage)
}
