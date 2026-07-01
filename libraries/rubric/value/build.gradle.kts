plugins {
    id("repo.java-library-conventions")
    id("repo.published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    implementation(projects.libraries.util)
    implementation(libs.oliveryasuna.commonsLanguage)
}
