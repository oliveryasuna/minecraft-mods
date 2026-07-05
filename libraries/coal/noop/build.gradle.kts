plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("coal.version").get()

// The no-op COAL provider. Modeled on slf4j-nop: consumers add this dep when
// they want config calls to silently no-op instead of failing / logging warnings
// about a missing provider.
//
// The same behavior is also baked into coal-api as an inline private fallback
// so consumers who forget to add ANY provider don't crash. This module exists
// for consumers who want no-op behavior to be an explicit, deliberate choice.
dependencies {
    api(projects.libraries.coal.coalApi)
}
