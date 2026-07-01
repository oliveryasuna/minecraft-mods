import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("repo.java-library-conventions")
    id("com.vanniktech.maven.publish")
    signing
}

// Modules that opt into `repo.published-library` are shipped to Maven Central.
// Coordinates come from Gradle project attributes:
//   groupId    = project.group    (set to "com.oliveryasuna.mc" in repo.base-conventions)
//   artifactId = project.name     (post-rename in settings.gradle.kts, e.g. "rubric-api")
//   version    = project.version  (per-module string in each build.gradle.kts)
//
// The Vanniktech plugin handles: publication creation, sources + javadoc jars,
// GPG signing (via ~/.gradle/gradle.properties → signing.gnupg.*), and the
// Central Portal upload endpoint. License wiring is left to
// `repo.licensed-library`, which reads from LICENSE + LICENSE.spdx and injects
// the POM `<licenses>` block when maven-publish is present — Vanniktech applies
// maven-publish, so that hook fires here automatically.
// Shell out to the local `gpg` command using the credentials in
// ~/.gradle/gradle.properties (signing.gnupg.keyName + signing.gnupg.passphrase).
// Vanniktech's `signAllPublications()` calls the standard signing plugin — we
// just need to point that plugin at gpg. Alternative for CI:
// ORG_GRADLE_PROJECT_signingInMemoryKey (see Vanniktech docs).
signing {
    useGpgCmd()
}

mavenPublishing {
    // Staging: uploaded artifacts land in the Central Portal's staging area.
    // With automaticRelease = false, releases go through a manual "Publish"
    // click at central.sonatype.com — safer while the pipeline is new. Flip
    // to true once verified.
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
