plugins {
    id("repo.java-library-conventions")
}

version = "0.1.0"

dependencies {
    api(projects.libraries.rubric.rubricApi)
    api(projects.libraries.rubric.rubricValue)
    api(projects.libraries.rubric.rubricSchema)
}
