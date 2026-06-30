plugins {
    id("workshop.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    api(projects.libraries.omniconfig.omniconfigCore)
    api(projects.libraries.omniconfig.omniconfigSchema)
    api(projects.libraries.omniconfig.omniconfigValidation)
    api(projects.libraries.omniconfig.omniconfigSyncProtocol)

    implementation(libs.oliveryasuna.commonsLanguage)
}
