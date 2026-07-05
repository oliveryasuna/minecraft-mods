plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("coal.version").get()

dependencies {
    api(projects.libraries.coal.coalApi)
}
