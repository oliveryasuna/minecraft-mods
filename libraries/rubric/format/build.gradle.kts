plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricIo)
    api(libs.nightconfig.core)
    api(libs.nightconfig.toml)
    api(libs.nightconfig.json)
    api(libs.jankson)

    implementation(projects.libraries.rubric.rubricModel)
    implementation(libs.oliveryasuna.commonsLanguage)
}
