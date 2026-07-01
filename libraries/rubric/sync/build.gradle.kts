plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    api(projects.libraries.rubric.rubricCore)
    api(projects.libraries.rubric.rubricSchema)
    api(projects.libraries.rubric.rubricValidation)
    api(projects.libraries.rubric.rubricSyncProtocol)

    implementation(libs.oliveryasuna.commonsLanguage)
}
