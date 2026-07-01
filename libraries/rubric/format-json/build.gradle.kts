plugins {
    id("repo.java-library-conventions")
    id("repo.published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    api(projects.libraries.rubric.rubricFormatNightconfig)
    api(libs.nightconfig.json)
}
