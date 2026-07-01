plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    api(projects.libraries.rubric.rubricValue)

    implementation(libs.oliveryasuna.commonsLanguage)
}
