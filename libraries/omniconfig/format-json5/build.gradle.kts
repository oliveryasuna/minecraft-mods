plugins {
    id("workshop.java-library-conventions")
}

version = "0.1.0-SNAPSHOT"

dependencies {
    api(projects.libraries.omniconfig.omniconfigIoSpi)
    api(libs.jankson)

    implementation(projects.libraries.omniconfig.omniconfigSchema)
}
