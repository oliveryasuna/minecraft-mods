plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0"

dependencies {
    implementation(projects.libraries.util)
    implementation(libs.oliveryasuna.commonsLanguage)
}
