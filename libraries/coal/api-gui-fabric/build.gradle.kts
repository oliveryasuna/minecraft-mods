plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
    id("fabric-loom") version "1.17.14"
}

version = providers.gradleProperty("coal.version").get()

val mcVersion = "1.21.8"
val fabricLoaderVer = "0.16.14"

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modCompileOnly("net.fabricmc:fabric-loader:$fabricLoaderVer")

    api(projects.libraries.coal.coalApi)

    implementation(libs.oliveryasuna.commonsLanguage)
}
