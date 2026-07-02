plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricIoSpi)
    api(libs.nightconfig.core)

    implementation(projects.libraries.rubric.rubricSchema)
    implementation(libs.oliveryasuna.commonsLanguage)
}
