plugins {
    kotlin("multiplatform") version "2.1.0" apply false
    kotlin("jvm") version "2.1.0" apply false
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
    jacoco
}

group = "net.sourceforge.moonstone"
version = "0.1.0"

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "jacoco")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
        parallel = true

        source.setFrom(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/desktopMain/kotlin",
            "src/main/kotlin"
        )
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        android.set(true)
        ignoreFailures.set(false)

        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }

    afterEvaluate {
        tasks.findByName("check")?.dependsOn("ktlintCheck", "detekt")
    }
}

tasks.register("lintAll") {
    group = "verification"
    description = "Run all linting checks"
    dependsOn(
        subprojects.map { "${it.path}:ktlintCheck" },
        subprojects.map { "${it.path}:detekt" }
    )
}

tasks.register("formatAll") {
    group = "formatting"
    description = "Auto-format all Kotlin code"
    dependsOn(subprojects.map { "${it.path}:ktlintFormat" })
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
    delete(file("android/webide-frontend/node_modules"))
}
