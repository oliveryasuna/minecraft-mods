plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)

    // Makes the version catalog (`libs`) available inside precompiled script
    // plugins.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    // Type-only deps for `repo.modded-variants` — it inspects/extends both
    // Fabric Loom's and ModDevGradle's run-config DSLs to register variant runs
    // that mirror the base runs with a different game directory. The plugins
    // themselves are applied by consumer modules, not by build-logic.
    compileOnly("net.fabricmc:fabric-loom:1.17.12")
    compileOnly("net.neoforged:moddev-gradle:2.0.141")

    implementation("com.vanniktech:gradle-maven-publish-plugin:0.37.0")
}
