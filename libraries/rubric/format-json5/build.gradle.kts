plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    api(projects.libraries.rubric.rubricIoSpi)
    api(libs.jankson)

    implementation(projects.libraries.rubric.rubricSchema)
}
