plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("coal.version").get()

dependencies {
    compileOnly(libs.slf4j.api)

    implementation(libs.oliveryasuna.commonsLanguage)
    implementation(libs.commons.lang3)

    testRuntimeOnly(libs.slf4j.api)
    testRuntimeOnly(libs.logback)
    testImplementation(projects.libraries.coal.coalNoop)
    testImplementation(libs.logcaptor) {
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}
