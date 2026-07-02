plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricIoSpi)
    api(libs.jankson)

    implementation(projects.libraries.rubric.rubricSchema)
    implementation(libs.oliveryasuna.commonsLanguage)
}
