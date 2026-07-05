plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("coal.version").get()

dependencies {
    // Abstract test classes reference JUnit types on their signatures — extending providers pick them up transitively.
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    // Launcher: ConformanceReportListener registers via META-INF/services as a TestExecutionListener.
    api(libs.junit.platform.launcher)

    // COAL types appear on the abstract-class API (factories, snapshots, etc.).
    api(projects.libraries.coal.coalApi)
    api(projects.libraries.coal.coalApiSync)

    // TestkitPlatform + ConformanceReportListener log via SLF4J. Providers on the testkit
    // classpath already have an SLF4J impl from their own runtime; declare compileOnly here
    // to keep the testkit from pinning a specific backend.
    compileOnly(libs.slf4j.api)

    implementation(libs.gson)
    implementation(libs.oliveryasuna.commonsLanguage)

    testRuntimeOnly(libs.slf4j.api)
    testRuntimeOnly(libs.logback)
    testImplementation(projects.libraries.coal.coalNoop)
}
