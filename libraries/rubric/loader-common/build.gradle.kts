plugins {
    id("oy-java-library-conventions")
    id("net.neoforged.moddev") version "2.0.141"
}

repositories {
    // Frontend deps are compileOnly here (mc-common builds against their
    // interfaces without shipping them).
    maven("https://maven.isxander.dev/releases") {
        name = "IsxanderReleases"
        content { includeGroup("dev.isxander") }
    }
    // YACL transitively depends on org.quiltmc.parsers.
    maven("https://maven.quiltmc.org/repository/release/") {
        name = "QuiltReleases"
        content { includeGroup("org.quiltmc.parsers") }
    }
    maven("https://maven.shedaniel.me/") {
        name = "ShedanielMaven"
        content { includeGroup("me.shedaniel.cloth") }
    }
}

version = providers.gradleProperty("rubric.version").get()

val mcVersion = "1.21.8"
val neoFormVer = "1.21.8-20250717.133445"

val yaclVersion = "3.7.0+1.21.6-neoforge"
val clothVersion = "19.0.147"

// ModDev's `neoFormVersion` mode: puts Mojmap-remapped Minecraft on the
// compile classpath WITHOUT NeoForge itself. Produces a plain-Java lib jar
// referring to `net.minecraft.*` under Mojmap names — identical to what
// both Loom (Fabric) and ModDev proper (NG) resolve against, so the same
// jar links cleanly on either loader.
neoForge {
    neoFormVersion = neoFormVer
}

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

dependencies {
    // mc-common absorbs what used to be rubric-loader-common — the non-MC
    // bits (RubricConfig, RubricSerialization, Constants, RubricSelf,
    // ScreenBuildContext) plus the MC-touching screen code. One shared
    // module is simpler; nothing else consumed loader-common in practice.
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricValue)
    api(projects.libraries.rubric.rubricSchema)
    api(projects.libraries.rubric.rubricCore)
    api(projects.libraries.rubric.rubricIoSpi)
    api(projects.libraries.rubric.rubricIoFile)
    api(projects.libraries.rubric.rubricFormatToml)
    api(projects.libraries.rubric.rubricFormatJson)
    api(projects.libraries.rubric.rubricFormatJson5)
    api(projects.libraries.rubric.rubricValidation)
    api(projects.libraries.rubric.rubricSync)

    // YACL / Cloth Config: mc-common references their APIs but doesn't
    // require them at runtime — each loader supplies its own flavored jar.
    // Class names/signatures are identical across the fabric/neoforge
    // classifiers, so either is fine for compilation.
    compileOnly("dev.isxander:yet-another-config-lib:$yaclVersion")
    compileOnly("me.shedaniel.cloth:cloth-config-neoforge:$clothVersion")

    implementation(libs.oliveryasuna.commonsLanguage)
}
