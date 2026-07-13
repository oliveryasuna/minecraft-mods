plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)

    // Makes the version catalog (`libs`) available inside precompiled script
    // plugins.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    compileOnly("net.fabricmc:fabric-loom:1.17.14")
    compileOnly("net.neoforged:moddev-gradle:2.0.141")

    implementation("com.vanniktech:gradle-maven-publish-plugin:0.37.0")
}

gradlePlugin {
    plugins {
        create("baseConventions") {
            id = "oy-base-conventions"
            implementationClass = "plugins.base.BaseConventionsPlugin"
        }
        create("testingConventions") {
            id = "oy-testing-conventions"
            implementationClass = "plugins.testing.TestingConventionsPlugin"
        }
        create("licensedLibrary") {
            id = "oy-licensed-library"
            implementationClass = "plugins.licensed.LicensedLibraryPlugin"
        }
        create("javaLibraryConventions") {
            id = "oy-java-library-conventions"
            implementationClass = "plugins.javalibrary.JavaLibraryConventionsPlugin"
        }
        create("kotlinLibraryConventions") {
            id = "oy-kotlin-library-conventions"
            implementationClass = "plugins.kotlinlibrary.KotlinLibraryConventionsPlugin"
        }
        create("publishedLibrary") {
            id = "oy-published-library"
            implementationClass = "plugins.published.PublishedLibraryPlugin"
        }
        create("mod") {
            id = "oy-mod"
            implementationClass = "plugins.mod.ModPlugin"
        }
    }
}
