plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

dependencies {
    // Pure-JVM loader-agnostic bits: RubricConfig POJO, RubricSerialization
    // factory, ScreenBuildContext, Constants, RubricSelf. MC-touching screen
    // code lives in each loader module — it can't be shared as a single
    // compiled artifact because Fabric prod uses intermediary mappings while
    // NG prod uses Mojmap.
    api(projects.libraries.rubric.rubricCore)
    api(projects.libraries.rubric.rubricModel)
    api(projects.libraries.rubric.rubricIo)
    api(projects.libraries.rubric.rubricFormat)

    implementation(libs.oliveryasuna.commonsLanguage)
}
