plugins {
    id("workshop.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    api(projects.libraries.omniconfig.omniconfigApi)
    api(projects.libraries.omniconfig.omniconfigValue)
    api(projects.libraries.omniconfig.omniconfigValidation)

    implementation(projects.libraries.util)
    implementation(libs.commons.lang3)
    implementation(libs.oliveryasuna.commonsLanguage)
}
