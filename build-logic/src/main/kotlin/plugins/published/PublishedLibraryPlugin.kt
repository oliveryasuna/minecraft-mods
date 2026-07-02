package plugins.published

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.SigningExtension

abstract class PublishedLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // 1. Apply dependent plugins.
        project.applyDependentPlugins()

        // 4. Eager configuration.
        project.configureSigning()
        project.configurePublishing()
    }

    private fun Project.applyDependentPlugins() {
        pluginManager.apply("oy-java-library-conventions")
        pluginManager.apply("com.vanniktech.maven.publish")
        pluginManager.apply("signing")
    }

    private fun Project.configureSigning() {
        extensions.configure<SigningExtension> {
            useGpgCmd()
        }
    }

    private fun Project.configurePublishing() {
        val projectName = name
        val descriptionProvider = provider {
            description
            ?: "Part of the Rubric multi-loader Minecraft mod configuration library."
        }

        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral(automaticRelease = false)

            signAllPublications()

            configure(
                JavaLibrary(
                    javadocJar = JavadocJar.Javadoc(),
                    sourcesJar = true,
                ),
            )

            pom {
                name.set("Rubric — $projectName")
                description.set(descriptionProvider)
                url.set("https://github.com/oliveryasuna/minecraft-mods")
                inceptionYear.set("2026")

                developers {
                    developer {
                        id.set("oliveryasuna")
                        name.set("Oliver Yasuna")
                        url.set("https://github.com/oliveryasuna")
                    }
                }

                scm {
                    url.set("https://github.com/oliveryasuna/minecraft-mods")
                    connection.set("scm:git:git@github.com:oliveryasuna/minecraft-mods.git")
                    developerConnection.set("scm:git:git@github.com:oliveryasuna/minecraft-mods.git")
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/oliveryasuna/minecraft-mods/issues")
                }

                // <licenses> is filled by oy-licensed-library — do not
                // duplicate it.
            }
        }
    }

}
