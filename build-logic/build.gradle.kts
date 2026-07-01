plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)

    // Makes the version catalog (`libs`) available inside precompiled script
    // plugins.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    compileOnly("net.fabricmc:fabric-loom:1.17.12")
    compileOnly("net.neoforged:moddev-gradle:2.0.141")

    implementation("com.vanniktech:gradle-maven-publish-plugin:0.37.0")
}
