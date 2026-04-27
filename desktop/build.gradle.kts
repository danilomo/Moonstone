import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    @Suppress("DEPRECATION")
    implementation(compose.material3)
    // KleinLisp dependency for direct access to LispEnvironment
    implementation("com.github.danilomo:KleinLisp:0.0.3")
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
                TargetFormat.Dmg, // macOS
                TargetFormat.Msi, // Windows installer
                TargetFormat.Deb, // Linux (Debian/Ubuntu)
                TargetFormat.Rpm, // Linux (Fedora/RHEL)
            )

            includeAllModules = true

            packageName = "moonstone"
            packageVersion = "1.0.0"
            description = "A Scheme-based declarative UI framework built on Jetpack Compose"
            vendor = "Moonstone Project"
            copyright = "Copyright 2024 Moonstone Project"

            linux {
                iconFile.set(project.file("icons/icon.png"))
                debMaintainer = "moonstone@example.com"
                rpmLicenseType = "MIT"
                shortcut = true
                menuGroup = "Development"
                appCategory = "Development"
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

// Helper function to run external commands
fun Project.runCommand(vararg args: String) {
    ProcessBuilder(*args)
        .directory(projectDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor()
}

// Task to modify DEB package to add PATH symlink
tasks.configureEach {
    if (name == "packageDeb") {
        doLast {
            val debFile = file("build/compose/binaries/main/deb/moonstone_1.0.0-1_amd64.deb")
            if (!debFile.exists()) {
                throw GradleException("DEB file not found: ${debFile.absolutePath}")
            }

            val tempDir = file("build/deb-repack")
            val controlDir = file("$tempDir/DEBIAN")

            // Clean and create temp directories
            delete(tempDir)
            tempDir.mkdirs()
            controlDir.mkdirs()

            // Extract the package
            project.runCommand("dpkg-deb", "-x", debFile.absolutePath, tempDir.absolutePath)
            project.runCommand("dpkg-deb", "-e", debFile.absolutePath, controlDir.absolutePath)

            // Modify postinst script to add symlink creation
            val postinst = file("$controlDir/postinst")
            val originalContent = postinst.readText()

            // Insert symlink creation inside the configure case
            val symlinkCode = """        # Create symlink in /usr/bin for PATH access
        if [ -d "/opt/moonstone/bin" ] && [ ! -e /usr/bin/moonstone ]; then
            ln -sf /opt/moonstone/bin/moonstone /usr/bin/moonstone
        fi
"""
            val modifiedContent =
                originalContent.replace(
                    "xdg-desktop-menu install /opt/moonstone/lib/moonstone-moonstone.desktop",
                    "xdg-desktop-menu install /opt/moonstone/lib/moonstone-moonstone.desktop\n" + symlinkCode,
                )

            postinst.writeText(modifiedContent)

            // Modify prerm script to remove symlink
            val prerm = file("$controlDir/prerm")
            val originalPrermContent = prerm.readText()

            val removeSymlinkCode = """        # Remove symlink from /usr/bin
        if [ -L /usr/bin/moonstone ]; then
            rm -f /usr/bin/moonstone
        fi
"""
            val modifiedPrermContent =
                originalPrermContent.replace(
                    "xdg-desktop-menu uninstall /opt/moonstone/lib/moonstone-moonstone.desktop",
                    "xdg-desktop-menu uninstall /opt/moonstone/lib/moonstone-moonstone.desktop\n" + removeSymlinkCode,
                )
            prerm.writeText(modifiedPrermContent)

            // Rebuild the package
            val newDebFile = file("build/compose/binaries/main/deb/moonstone_1.0.0-1_amd64_withsymlink.deb")
            project.runCommand("dpkg-deb", "-b", tempDir.absolutePath, newDebFile.absolutePath)

            // Replace original with modified version
            delete(debFile)
            newDebFile.renameTo(debFile)

            println("✓ DEB package rebuilt with PATH symlink support: ${debFile.absolutePath}")
        }
    }
}

// Task to add instructions for RPM package PATH symlink
tasks.configureEach {
    if (name == "packageRpm") {
        doLast {
            val rpmFile =
                fileTree("build/compose/binaries/main/rpm") {
                    include("*.rpm")
                }.singleFile

            if (!rpmFile.exists()) {
                throw GradleException("RPM file not found")
            }

            // Note: RPM modification is more complex and requires rpmbuild
            // For now, print instructions
            println(
                """
                ⚠ RPM package needs manual modification.
                To add PATH symlink support to RPM:
                1. Install the RPM: sudo rpm -i ${rpmFile.absolutePath}
                2. Run: sudo ln -sf /opt/moonstone/bin/moonstone /usr/bin/moonstone

                Or use the manual setup script: ./desktop/setup-moonstone-path.sh
                """.trimIndent(),
            )
        }
    }
}

// Task to create Windows portable distribution (no installer required)
tasks.register<Zip>("packageWindowsPortable") {
    group = "distribution"
    description = "Create a portable Windows distribution (ZIP archive, no installation required)"

    dependsOn("createDistributable")

    val appImageDir = file("build/compose/binaries/main/app/moonstone")

    from(appImageDir) {
        into("moonstone")
    }

    archiveFileName.set("moonstone-${project.version}-windows-portable.zip")
    destinationDirectory.set(file("build/compose/binaries/main/portable"))

    doLast {
        val outputFile = file("build/compose/binaries/main/portable/moonstone-${project.version}-windows-portable.zip")
        println("✓ Windows portable distribution created: ${outputFile.absolutePath}")
        println("  Extract and run: moonstone\\bin\\moonstone.bat")
    }
}
