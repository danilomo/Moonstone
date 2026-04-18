import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    // KleinLisp dependency for direct access to LispEnvironment
    implementation("net.sourceforge.kleinlisp:KleinLisp:0.0.1")
}

// Task to run the REPL for interactive development
tasks.register<JavaExec>("repl") {
    group = "application"
    description = "Start the Moonstone REPL for interactive development"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.sourceforge.moonstone.desktop.ReplMain")
    standardInput = System.`in`
}

compose.desktop {
    application {
        mainClass = "net.sourceforge.moonstone.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,    // macOS
                TargetFormat.Msi,    // Windows
                TargetFormat.Deb,    // Linux (Debian/Ubuntu)
                TargetFormat.Rpm     // Linux (Fedora/RHEL)
            )

            packageName = "Moonstone"
            packageVersion = "1.0.0"
            description = "A Scheme-based declarative UI framework built on Jetpack Compose"
            vendor = "Moonstone Project"
            copyright = "Copyright 2024 Moonstone Project"

            linux {
                iconFile.set(project.file("icons/icon.png"))
                debMaintainer = "moonstone@example.com"
                rpmLicenseType = "MIT"
            }

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = "net.sourceforge.moonstone"
                dockName = "Moonstone"
            }

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup = "Moonstone"
                upgradeUuid = "7f9e8d7c-6b5a-4c3d-2e1f-0a9b8c7d6e5f"
                dirChooser = true
            }
        }

        buildTypes.release {
            proguard {
                isEnabled.set(false)
            }
        }
    }
}
