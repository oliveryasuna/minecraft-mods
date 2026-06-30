tasks.register("checkAll") {
    group = "verification"
    description = "Runs `check` on every leaf subproject."

    // Filter to leaf projects only; intermediate grouping projects
    // (`:libraries`, `:libraries:omniconfig`) have no `check` task.
    dependsOn(subprojects.filter { it.childProjects.isEmpty() }.map { "${it.path}:check" })
}
