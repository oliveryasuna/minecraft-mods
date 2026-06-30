plugins {
    id("workshop.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    api(projects.libraries.omniconfig.omniconfigIoSpi)
    api(libs.nightconfig.core)

    implementation(projects.libraries.omniconfig.omniconfigSchema)
    implementation(libs.oliveryasuna.commonsLanguage)
}
