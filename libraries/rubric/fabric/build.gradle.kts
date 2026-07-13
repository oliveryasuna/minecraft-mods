plugins {
    id("oy-mod")
    id("fabric-loom") version "1.17.14"
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

val mcVersion = "1.21.8"
val fabricLoaderVer = "0.16.14"
val fabricApiVersion = "0.136.1+1.21.8"

val modMenuVersion = "15.0.2"
val catalogueVersion = "6926816"
val yaclVersion = "3.7.0+1.21.6-fabric"
val clothVersion = "19.0.147"

mod {
    id = "rubric"
    version = providers.gradleProperty("rubric.version").get()
    name = "Rubric"
    description = "Multi-loader Minecraft mod configuration library."
    author = "Oliver Yasuna"

    minecraftVersion = mcVersion
    fabricLoaderVersion = fabricLoaderVer

    variants {
        val modMenu =
            register("modMenu") {
                mods("com.terraformersmc:modmenu:$modMenuVersion")
                applyTo("client", "testmodClient")
            }

        register("catalogue") {
            extends(modMenu)
            mods("curse.maven:catalogue-459701:$catalogueVersion")
            applyTo("client", "testmodClient")
        }

        val yacl =
            register("yacl") {
                mods("com.terraformersmc:modmenu:$modMenuVersion")
                mods("dev.isxander:yet-another-config-lib:$yaclVersion")
                applyTo("client", "testmodClient")
            }

        val cloth =
            register("cloth") {
                mods("com.terraformersmc:modmenu:$modMenuVersion")
                mods("me.shedaniel.cloth:cloth-config-fabric:$clothVersion")
                applyTo("client", "testmodClient")
            }

        register("yaclCloth") {
            extends(yacl, cloth)
            applyTo("client", "testmodClient")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVer")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricMigration)
    api(projects.libraries.rubric.rubricModel)
    api(projects.libraries.rubric.rubricIo)
    api(projects.libraries.rubric.rubricCore)
    api(projects.libraries.rubric.rubricFormat)
    api(projects.libraries.rubric.rubricSync)
    api(projects.libraries.rubric.rubricMojangCodec)
    api(projects.libraries.rubric.rubricLoaderCommon)

    include(projects.libraries.rubric.rubricApi)
    include(projects.libraries.rubric.rubricMigration)
    include(projects.libraries.rubric.rubricModel)
    include(projects.libraries.rubric.rubricIo)
    include(projects.libraries.rubric.rubricCore)
    include(projects.libraries.rubric.rubricFormat)
    include(projects.libraries.rubric.rubricSync)
    include(projects.libraries.rubric.rubricMojangCodec)
    include(projects.libraries.rubric.rubricLoaderCommon)
    include(projects.libraries.util)
    include(libs.nightconfig.core)
    include(libs.nightconfig.toml)
    include(libs.nightconfig.json)
    include(libs.jankson)
    include(libs.oliveryasuna.commonsLanguage)

    modCompileOnly("com.terraformersmc:modmenu:$modMenuVersion")
    modCompileOnly("dev.isxander:yet-another-config-lib:$yaclVersion")
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:$clothVersion") {
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
