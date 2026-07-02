plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricValue)
    api(projects.libraries.rubric.rubricValidation)

    implementation(projects.libraries.util)
    implementation(libs.commons.lang3)
    implementation(libs.oliveryasuna.commonsLanguage)
}
