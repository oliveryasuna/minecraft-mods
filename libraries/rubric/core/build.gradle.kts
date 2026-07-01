plugins {
    id("repo.java-library-conventions")
    id("repo.published-library")
}

version = "0.1.0"

dependencies {
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricValue)
    api(projects.libraries.rubric.rubricSchema)
    api(projects.libraries.rubric.rubricValidation)
    api(projects.libraries.rubric.rubricMigration)
    api(projects.libraries.rubric.rubricIoSpi)

    compileOnly(libs.slf4j.api)
    runtimeOnly(libs.logback)

    implementation(libs.oliveryasuna.commonsLanguage)
}
