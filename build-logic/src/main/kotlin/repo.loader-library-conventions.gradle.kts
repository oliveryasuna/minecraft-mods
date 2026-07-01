plugins {
    id("repo.base-conventions")
    id("repo.licensed-library")
    id("repo.modded-variants")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.get().toInt()
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-deprecation",
            "-Xlint:-processing",
            "-parameters",
        ),
    )
}
