plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()
