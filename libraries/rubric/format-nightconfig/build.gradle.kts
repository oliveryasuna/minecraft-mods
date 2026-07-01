plugins {
    id("repo.java-library-conventions")
    id("repo.published-library")
}

version = "0.1.0"

dependencies {
    api(projects.libraries.rubric.rubricIoSpi)
    api(libs.nightconfig.core)

    implementation(projects.libraries.rubric.rubricSchema)
    implementation(libs.oliveryasuna.commonsLanguage)
}
