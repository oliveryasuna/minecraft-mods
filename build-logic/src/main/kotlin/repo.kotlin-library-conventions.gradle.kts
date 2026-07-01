import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("repo.java-library-conventions")
    kotlin("jvm")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.java.get())
        allWarningsAsErrors = true
        freeCompilerArgs.addAll(
            "-Xjvm-default=all",
            "-Xjsr305=strict",
        )
    }
}

dependencies {
    "testImplementation"(libs.kotlin.test.junit5)
}

extensions.configure<SpotlessExtension> {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
}
