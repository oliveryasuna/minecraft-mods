plugins {
    id("workshop.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    api(projects.libraries.omniconfig.omniconfigIoSpi)

    implementation(libs.oliveryasuna.commonsLanguage)
}
