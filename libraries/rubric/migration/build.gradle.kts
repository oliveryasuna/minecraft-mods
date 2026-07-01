plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0"

dependencies {
    api(projects.libraries.rubric.rubricValue)

    implementation(libs.oliveryasuna.commonsLanguage)
    implementation(libs.commons.lang3)
}
