// Declared at the root with `apply false` so it lives in a shared classloader
// scope across every subproject. Without this, `oy-published-library` applied
// on a Loom-flavored project (coal-api-gui-fabric) creates a project-scoped
// plugin classloader that conflicts with the util/rubric-* subprojects
// applying the same plugin — build service registration fails.
plugins {
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
}

tasks.register("checkAll") {
    group = "verification"
    description = "Runs `check` on every leaf subproject."

    // Filter to leaf projects only; intermediate grouping projects
    // (`:libraries`, `:libraries:rubric`) have no `check` task.
    dependsOn(subprojects.filter { it.childProjects.isEmpty() }.map { "${it.path}:check" })
}

// ==================================================
// Per-loader aggregators — used by CI matrix jobs so
// each runner only pulls its loader's toolchain
// (Loom mappings vs. ModDev userdev). Every module
// that isn't loader-suffixed is treated as "shared"
// and runs on both matrix legs so its tests always
// execute somewhere.
// ==================================================

val leafSubprojects: List<Project> get() = subprojects.filter { it.childProjects.isEmpty() }

fun registerLoaderAggregator(loader: String, taskName: String, delegateTask: String, desc: String) {
    tasks.register(taskName) {
        group = "verification"
        description = desc
        dependsOn(
            leafSubprojects
                .filter { it.name.endsWith("-$loader") || (!it.name.endsWith("-fabric") && !it.name.endsWith("-neoforge")) }
                .map { "${it.path}:$delegateTask" }
        )
    }
}

registerLoaderAggregator("fabric", "assembleFabric", "assemble", "Assembles every Fabric + shared leaf subproject.")
registerLoaderAggregator("fabric", "checkFabric", "check", "Runs `check` on every Fabric + shared leaf subproject.")
registerLoaderAggregator("neoforge", "assembleNeoForge", "assemble", "Assembles every NeoForge + shared leaf subproject.")
registerLoaderAggregator("neoforge", "checkNeoForge", "check", "Runs `check` on every NeoForge + shared leaf subproject.")

tasks.register("publishRubric") {
    group = "publishing"
    description = "Publish every rubric-* module to Maven Central."
    dependsOn(
        subprojects
            .filter { it.path.startsWith(":libraries:rubric:") }
            .mapNotNull { it.tasks.findByName("publishAllPublicationsToMavenCentralRepository") }
    )
}

tasks.register("publishRubricLocal") {
    group = "publishing"
    description = "Publish every rubric-* module to the local Maven cache."
    dependsOn(
        subprojects
            .filter { it.path.startsWith(":libraries:rubric:") }
            .mapNotNull { it.tasks.findByName("publishToMavenLocal") }
    )
}
