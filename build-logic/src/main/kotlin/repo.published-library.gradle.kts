import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("repo.java-library-conventions")
    id("com.vanniktech.maven.publish")
    signing
}

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = false)

    signAllPublications()

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Javadoc(),
            sourcesJar = true,
        ),
    )

    pom {
        name.set("Rubric — ${project.name}")
        description.set(
            provider {
                project.description
                    ?: "Part of the Rubric multi-loader Minecraft mod configuration library."
            },
        )
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

        // <licenses> is filled by repo.licensed-library — do not duplicate it.
    }
}
