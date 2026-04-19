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
        maven("https://jitpack.io")
    }
}

rootProject.name = "Moonstone"

// Conditional composite build for KleinLisp
// Uses local sibling directory if available (for active development)
// Falls back to JitPack in CI/CD or for casual contributors
val kleinLispPath = file("../KleinLisp")
if (kleinLispPath.exists() && kleinLispPath.isDirectory) {
    println("✓ Using local KleinLisp from ../KleinLisp (composite build)")
    includeBuild("../KleinLisp") {
        dependencySubstitution {
            substitute(module("com.github.danilomo:KleinLisp"))
                .using(project(":"))
        }
    }
} else {
    println("✓ Using KleinLisp from JitPack repository")
}

include(":core")
include(":desktop")
include(":android")
