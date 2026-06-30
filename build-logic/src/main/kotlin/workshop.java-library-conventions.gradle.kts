plugins {
    `java-library`
    `maven-publish`
    id("workshop.base-conventions")
    id("workshop.testing-conventions")
    id("workshop.licensed-library")
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
    withSourcesJar()
    withJavadocJar()
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
