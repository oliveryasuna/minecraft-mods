plugins {
    `java-library`
}

// Walk up to find the family directory directly under `libraries/`.
val familyDir = generateSequence(project.projectDir) { it.parentFile }
    .firstOrNull { it.parentFile?.name == "libraries" }
    ?: error("Module ${project.path} is not under libraries/<family>/ — apply this plugin only to family-rooted modules.")

val licenseFile = familyDir.resolve("LICENSE").also {
    require(it.isFile) { "Missing license text at $it" }
}
val spdxFile = familyDir.resolve("LICENSE.spdx").also {
    require(it.isFile) { "Missing SPDX descriptor at $it (expected: '<SPDX-ID>|<URL>')" }
}
val (spdxId, licenseUrl) = spdxFile.readText().trim().split("|", limit = 2).also {
    require(it.size == 2) { "Malformed $spdxFile — expected '<SPDX-ID>|<URL>'" }
}

// Bundle the family LICENSE into every published jar under META-INF/.
tasks.withType<Jar>().configureEach {
    from(licenseFile) { into("META-INF") }
}

// Wire the license into the Maven POM whenever maven-publish is applied.
plugins.withId("maven-publish") {
    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            pom.licenses {
                license {
                    name = spdxId
                    url = licenseUrl
                    distribution = "repo"
                }
            }
        }
    }
}