plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0"

dependencies {
    api(projects.libraries.rubric.rubricIoSpi)
    api(libs.jankson)

    implementation(projects.libraries.rubric.rubricSchema)
    implementation(libs.oliveryasuna.commonsLanguage)
}
