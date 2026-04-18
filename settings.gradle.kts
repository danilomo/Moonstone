pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "Moonstone"

// Include KleinLisp as a composite build from sibling directory
includeBuild("../KleinLisp") {
    dependencySubstitution {
        substitute(module("net.sourceforge.kleinlisp:KleinLisp")).using(project(":"))
    }
}

include(":core")
include(":desktop")
include(":android")
