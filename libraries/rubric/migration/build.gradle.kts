plugins {
    id("repo.java-library-conventions")
    id("repo.published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricValue)

    implementation(libs.oliveryasuna.commonsLanguage)
    implementation(libs.commons.lang3)
}
