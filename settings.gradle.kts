plugins {
    id("de.fayard.refreshVersions").version("0.51.0")
}

refreshVersions {
    rejectVersionIf {
        candidate.stabilityLevel.isLessStableThan(current.stabilityLevel)
    }
}