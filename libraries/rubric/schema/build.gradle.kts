plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0"

dependencies {
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricValue)
    api(projects.libraries.rubric.rubricValidation)

    implementation(projects.libraries.util)
    implementation(libs.commons.lang3)
    implementation(libs.oliveryasuna.commonsLanguage)
}
