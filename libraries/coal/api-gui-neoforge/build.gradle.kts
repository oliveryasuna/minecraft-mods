plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
    id("net.neoforged.moddev") version "2.0.141"
}

version = providers.gradleProperty("coal.version").get()

neoForge {
    neoFormVersion = "1.21.8-20250717.133445"
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if(requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
            useVersion("3.17.0")
            because("MC 1.21.8 strictly pins commons-lang3 to 3.17.0")
        }
        if(requested.group == "org.slf4j" && requested.name == "slf4j-api") {
            useVersion("2.0.16")
            because("MC 1.21.8 strictly pins slf4j-api to 2.0.16")
        }
    }
}

dependencies {
    api(projects.libraries.coal.coalApi)

    implementation(libs.oliveryasuna.commonsLanguage)
}
