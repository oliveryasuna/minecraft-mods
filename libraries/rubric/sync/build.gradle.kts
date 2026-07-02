plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricCore)
    api(projects.libraries.rubric.rubricSchema)
    api(projects.libraries.rubric.rubricValidation)
    api(projects.libraries.rubric.rubricSyncProtocol)

    implementation(libs.oliveryasuna.commonsLanguage)
}
