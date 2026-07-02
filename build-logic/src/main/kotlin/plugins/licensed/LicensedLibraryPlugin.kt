package plugins.licensed

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import java.io.File

abstract class LicensedLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.applyDependentPlugins()

        val familyDir = generateSequence(project.projectDir) { it.parentFile }
                            .firstOrNull { it.parentFile?.name == "libraries" }
                        ?: error(
                            "Module ${project.path} is not under libraries/<family>/ — " +
                            "apply this plugin only to family-rooted modules.",
                        )

        val licenseFile = familyDir.resolve("LICENSE").also {
            require(it.isFile) { "Missing license text at $it" }
        }
        val spdxFile = familyDir.resolve("LICENSE.spdx").also {
            require(it.isFile) { "Missing SPDX descriptor at $it (expected: '<SPDX-ID>|<URL>')" }
        }
        val (spdxId, licenseUrl) = spdxFile.readText().trim().split("|", limit = 2).also {
            require(it.size == 2) { "Malformed $spdxFile — expected '<SPDX-ID>|<URL>'" }
        }

        project.bundleLicense(licenseFile)

        project.configurePomLicense(spdxId, licenseUrl)
    }

    private fun Project.applyDependentPlugins() {
        pluginManager.apply("java-library")
    }

    private fun Project.bundleLicense(licenseFile: File) {
        tasks.withType<Jar>().configureEach {
            from(licenseFile) { into("META-INF") }
        }
    }

    private fun Project.configurePomLicense(spdxId: String, licenseUrl: String) {
        plugins.withId("maven-publish") {
            extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication>().configureEach {
                    pom.licenses {
                        license {
                            name.set(spdxId)
                            url.set(licenseUrl)
                            distribution.set("repo")
                        }
                    }
                }
            }
        }
    }

}
