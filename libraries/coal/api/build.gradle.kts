plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("coal.version").get()

dependencies {
    compileOnly(libs.slf4j.api)

    implementation(libs.oliveryasuna.commonsLanguage)
    implementation(libs.commons.lang3)
}
