plugins {
    id("workshop.loader-library-conventions")
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
}

version = "0.1.0-SNAPSHOT"

val mcVersion = "1.21.8"
val fabricLoaderVersion = "0.16.14"
val fabricApiVersion = "0.136.1+1.21.8"

val modMenuVersion = "15.0.2"
val yaclVersion = "3.7.0+1.21.6-fabric"
val catalogueVersion = "6926816"

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")

    // api() — consumers need to compile against the types.
    api(projects.libraries.omniconfig.omniconfigApi)
    api(projects.libraries.omniconfig.omniconfigValue)
    api(projects.libraries.omniconfig.omniconfigValidation)
    api(projects.libraries.omniconfig.omniconfigMigration)
    api(projects.libraries.omniconfig.omniconfigIoSpi)
    api(projects.libraries.omniconfig.omniconfigSchema)
    api(projects.libraries.omniconfig.omniconfigCore)
    api(projects.libraries.omniconfig.omniconfigIoFile)
    api(projects.libraries.omniconfig.omniconfigFormatNightconfig)
    api(projects.libraries.omniconfig.omniconfigFormatToml)
    api(projects.libraries.omniconfig.omniconfigFormatJson)
    api(projects.libraries.omniconfig.omniconfigFormatJson5)
    api(projects.libraries.omniconfig.omniconfigSyncProtocol)
    api(projects.libraries.omniconfig.omniconfigSync)
    api(projects.libraries.omniconfig.omniconfigMojangCodec)

    // include() — JiJ-bundle into the shipped mod jar.
    include(projects.libraries.omniconfig.omniconfigApi)
    include(projects.libraries.omniconfig.omniconfigValue)
    include(projects.libraries.omniconfig.omniconfigValidation)
    include(projects.libraries.omniconfig.omniconfigMigration)
    include(projects.libraries.omniconfig.omniconfigIoSpi)
    include(projects.libraries.omniconfig.omniconfigSchema)
    include(projects.libraries.omniconfig.omniconfigCore)
    include(projects.libraries.omniconfig.omniconfigIoFile)
    include(projects.libraries.omniconfig.omniconfigFormatNightconfig)
    include(projects.libraries.omniconfig.omniconfigFormatToml)
    include(projects.libraries.omniconfig.omniconfigFormatJson)
    include(projects.libraries.omniconfig.omniconfigFormatJson5)
    include(projects.libraries.omniconfig.omniconfigSyncProtocol)
    include(projects.libraries.omniconfig.omniconfigSync)
    include(projects.libraries.omniconfig.omniconfigMojangCodec)

    // NightConfig + Jankson (JiJ — Fabric doesn't provide them).
    include(libs.nightconfig.core)
    include(libs.nightconfig.toml)
    include(libs.nightconfig.json)
    include(libs.jankson)

    modCompileOnly("com.terraformersmc:modmenu:${modMenuVersion}")
    modCompileOnly("dev.isxander:yet-another-config-lib:${yaclVersion}")

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
        register("omniconfig-testmod") {
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
    create("yacl") {
        gameDir = "run-yacl"
        mods("com.terraformersmc:modmenu:${modMenuVersion}")
        mods("dev.isxander:yet-another-config-lib:${yaclVersion}")
        applyTo("client", "testmodClient")
    }
    create("catalogue") {
        gameDir = "run-catalogue"
        mods("com.terraformersmc:modmenu:${modMenuVersion}")
        mods("curse.maven:catalogue-459701:${catalogueVersion}")
        applyTo("client", "testmodClient")
    }
}
