plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0"

dependencies {
    api(projects.libraries.rubric.rubricFormatNightconfig)
    api(libs.nightconfig.toml)
}
