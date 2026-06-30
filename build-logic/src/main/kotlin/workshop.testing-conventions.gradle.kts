plugins {
    `java-library`
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencies {
    "testImplementation"(platform(libs.junit.bom))
    "testImplementation"(libs.junit.jupiter)
    "testRuntimeOnly"(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
