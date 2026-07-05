plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("coal.version").get()

dependencies {
    // coal-api is the spec surface; consumers of this module (the loader
    // variants) already declare it themselves, so `api` keeps it transitive.
    api(projects.libraries.coal.coalApi)

    // gson: JSON persistence for the shared ConfigIO impl.
    implementation(libs.gson)

    // commons-language: reflection / instantiation helpers used by
    // AnnotationSchemaReader.
    implementation(libs.oliveryasuna.commonsLanguage)

    // commons-lang3: EntryMetadata.Builder extends
    // org.apache.commons.lang3.builder.Builder — required on compile.
    compileOnly(libs.commons.lang3)

    // slf4j: manager/provider log via SLF4J. Loader mods pin a real backend
    // at runtime; this module doesn't ship its own.
    compileOnly(libs.slf4j.api)

    testRuntimeOnly(libs.slf4j.api)
    testRuntimeOnly(libs.logback)
}
