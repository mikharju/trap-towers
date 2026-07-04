dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core")
include(":adapter-scenario")
include(":adapter-storage")
include(":trap-towers-ui-lanterna")

rootProject.name = "trap-towers"
