plugins {
    `java-library`
    id("repo.base-conventions")
    id("repo.testing-conventions")
    id("repo.licensed-library")
}

// Note: the Maven publication is intentionally NOT created here. Modules that
// publish to Maven Central apply `repo.published-library`, which pulls in the
// Vanniktech plugin — that plugin creates + configures the publication itself,
// so declaring one here would conflict.

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
    // Sources + Javadoc jars are configured by repo.published-library (via the
    // Vanniktech plugin's `JavaLibrary(javadocJar, sourcesJar)` setup). Calling
    // withSourcesJar()/withJavadocJar() here would create a second, conflicting
    // task chain that races with Vanniktech's generateMetadataFileForMavenPublication.
    consistentResolution {
        useCompileClasspathVersions()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.java.get().toInt()
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-processing",
            "-Werror",
            "-parameters",
        ),
    )
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}
