plugins {
    id("repo.java-library-conventions")
    id("repo.published-library")
}

version = "0.1.0"

dependencies {
    api(projects.libraries.rubric.rubricValue)

    implementation(libs.oliveryasuna.commonsLanguage)
}
