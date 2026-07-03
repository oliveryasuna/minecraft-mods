plugins {
    id("oy-java-library-conventions")
    id("oy-published-library")
}

version = providers.gradleProperty("rubric.version").get()

// Leaf module: annotations + Format enum. No other rubric-* dep — everyone
// depends on us. Keeping this a leaf breaks what would otherwise be a
// core <-> model cycle (Schema references Format; ConfigManager references
// Schema).
