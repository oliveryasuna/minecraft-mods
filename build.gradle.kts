tasks.register("checkAll") {
    group = "verification"
    description = "Runs `check` on every leaf subproject."

    // Filter to leaf projects only; intermediate grouping projects
    // (`:libraries`, `:libraries:rubric`) have no `check` task.
    dependsOn(subprojects.filter { it.childProjects.isEmpty() }.map { "${it.path}:check" })
}

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
