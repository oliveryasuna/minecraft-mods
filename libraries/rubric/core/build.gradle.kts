plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricMigration)
    api(projects.libraries.rubric.rubricModel)
    api(projects.libraries.rubric.rubricIo)

    compileOnly(libs.slf4j.api)

    implementation(libs.oliveryasuna.commonsLanguage)

    testRuntimeOnly(libs.logback)
}
