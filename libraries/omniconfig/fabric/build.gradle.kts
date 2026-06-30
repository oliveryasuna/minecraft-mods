plugins {
    id("workshop.loader-library-conventions")
    id("fabric-loom") version "1.17.13"
}

version = "0.1.0-SNAPSHOT"

val mcVersion = "1.21.8"
val fabricLoaderVersion = "0.16.14"
val fabricApiVersion = "0.136.1+1.21.8"

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
