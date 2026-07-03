plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricValue)
    api(projects.libraries.rubric.rubricSchema)
    api(projects.libraries.rubric.rubricValidation)
    api(projects.libraries.rubric.rubricMigration)
    api(projects.libraries.rubric.rubricIoSpi)

    compileOnly(libs.slf4j.api)

    implementation(libs.oliveryasuna.commonsLanguage)

    testRuntimeOnly(libs.logback)
}
