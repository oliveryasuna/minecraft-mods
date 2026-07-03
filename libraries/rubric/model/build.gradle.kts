plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricApi)

    implementation(projects.libraries.util)
    implementation(libs.commons.lang3)
    implementation(libs.oliveryasuna.commonsLanguage)
}
