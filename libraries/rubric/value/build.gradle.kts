plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    implementation(projects.libraries.util)
    implementation(libs.oliveryasuna.commonsLanguage)
}
