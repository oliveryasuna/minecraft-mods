plugins {
    id("repo.loader-library-conventions")
    id("fabric-loom") version "1.17.13"
}

repositories {
    // Loom installs its own project-level repos (synthetic flat-dirs for
    // remapped MC, fabric maven), which under PREFER_PROJECT mode shadow
    // anything declared in settings.gradle.kts. ModMenu, Catalogue, and YACL
    // are only ever dev-runtime deps here, so project-level repos are fine.
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

version = "0.1.0-SNAPSHOT"

val mcVersion = "1.21.8"
val fabricLoaderVersion = "0.16.14"
val fabricApiVersion = "0.136.1+1.21.8"

val modMenuVersion = "15.0.2"
val catalogueVersion = "6926816"
val yaclVersion = "3.7.0+1.21.6-fabric"
val clothVersion = "19.0.147"

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")

    // api() — consumers need to compile against the types.
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricValue)
    api(projects.libraries.rubric.rubricValidation)
    api(projects.libraries.rubric.rubricMigration)
    api(projects.libraries.rubric.rubricIoSpi)
    api(projects.libraries.rubric.rubricSchema)
    api(projects.libraries.rubric.rubricCore)
    api(projects.libraries.rubric.rubricIoFile)
    api(projects.libraries.rubric.rubricFormatNightconfig)
    api(projects.libraries.rubric.rubricFormatToml)
    api(projects.libraries.rubric.rubricFormatJson)
    api(projects.libraries.rubric.rubricFormatJson5)
    api(projects.libraries.rubric.rubricSyncProtocol)
    api(projects.libraries.rubric.rubricSync)
    api(projects.libraries.rubric.rubricMojangCodec)

    // include() — JiJ-bundle into the shipped mod jar.
    include(projects.libraries.rubric.rubricApi)
    include(projects.libraries.rubric.rubricValue)
    include(projects.libraries.rubric.rubricValidation)
    include(projects.libraries.rubric.rubricMigration)
    include(projects.libraries.rubric.rubricIoSpi)
    include(projects.libraries.rubric.rubricSchema)
    include(projects.libraries.rubric.rubricCore)
    include(projects.libraries.rubric.rubricIoFile)
    include(projects.libraries.rubric.rubricFormatNightconfig)
    include(projects.libraries.rubric.rubricFormatToml)
    include(projects.libraries.rubric.rubricFormatJson)
    include(projects.libraries.rubric.rubricFormatJson5)
    include(projects.libraries.rubric.rubricSyncProtocol)
    include(projects.libraries.rubric.rubricSync)
    include(projects.libraries.rubric.rubricMojangCodec)

    // NightConfig + Jankson (JiJ — Fabric doesn't provide them).
    include(libs.nightconfig.core)
    include(libs.nightconfig.toml)
    include(libs.nightconfig.json)
    include(libs.jankson)

    modCompileOnly("com.terraformersmc:modmenu:${modMenuVersion}")
    modCompileOnly("dev.isxander:yet-another-config-lib:${yaclVersion}")
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${clothVersion}") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    implementation(libs.oliveryasuna.commonsLanguage)
}

val testmod by sourceSets.creating {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

dependencies {
    "testmodImplementation"(sourceSets.main.get().output)
}

loom {
    mods {
        register("rubric-testmod") {
            sourceSet(testmod)
        }
    }
    runs {
        register("testmodServer") {
            server()
            source(testmod)
            runDir = "run-server"
        }
        register("testmodClient") {
            client()
            source(testmod)
            runDir = "run-client"
        }
    }
}

moddedVariants {
    create("modMenu") {
        gameDir = "run-modmenu"
        mods("com.terraformersmc:modmenu:${modMenuVersion}")
        applyTo("client", "testmodClient")
    }
    create("catalogue") {
        gameDir = "run-catalogue"
        mods("com.terraformersmc:modmenu:${modMenuVersion}")
        mods("curse.maven:catalogue-459701:${catalogueVersion}")
        applyTo("client", "testmodClient")
    }
    create("yacl") {
        gameDir = "run-yacl"
        mods("com.terraformersmc:modmenu:${modMenuVersion}")
        mods("dev.isxander:yet-another-config-lib:${yaclVersion}")
        applyTo("client", "testmodClient")
    }
    create("cloth") {
        gameDir = "run-cloth"
        mods("com.terraformersmc:modmenu:${modMenuVersion}")
        mods("me.shedaniel.cloth:cloth-config-fabric:${clothVersion}")
        applyTo("client", "testmodClient")
    }
}
