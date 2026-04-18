plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "net.sourceforge.moonstone.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.sourceforge.moonstone.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/native-image/**"
            excludes += "/META-INF/*.kotlin_module"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":core"))

    // KleinLisp (for Android extensions that use Lisp types)
    implementation("net.sourceforge.kleinlisp:KleinLisp:0.0.1")

    // Compose
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)

    // AndroidX
    implementation("androidx.activity:activity-compose:1.9.0")

    // NanoHTTPD for Web IDE server
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

// Web IDE Frontend build tasks
val webideFrontendDir = file("webide-frontend")
val webideAssetsDir = file("src/main/assets/webide")

// Find node bin directory (supports nvm, system node, etc.)
fun findNodeBinDir(): String? {
    val homeDir = System.getProperty("user.home")
    val candidates = listOf(
        // nvm paths - check if npm exists
        "$homeDir/.nvm/versions/node/v20.11.0/bin",
        "$homeDir/.nvm/versions/node/v18.19.0/bin",
        "$homeDir/.nvm/versions/node/v22.0.0/bin",
        // Common paths
        "/usr/local/bin",
        "/usr/bin"
    )
    for (candidate in candidates) {
        if (file("$candidate/npm").exists()) {
            return candidate
        }
    }
    return null
}

val nodeBinDir: String? = findNodeBinDir()
val npmPath: String? = nodeBinDir?.let { "$it/npm" }

tasks.register<Exec>("webideNpmInstall") {
    description = "Install Web IDE frontend dependencies"
    workingDir = webideFrontendDir
    commandLine = listOf(npmPath ?: "npm", "install")
    inputs.file(file("$webideFrontendDir/package.json"))
    outputs.dir(file("$webideFrontendDir/node_modules"))
    onlyIf { npmPath != null }
    // Add node bin dir to PATH so npm can find node
    nodeBinDir?.let {
        environment("PATH", "$it:${System.getenv("PATH")}")
    }
    doFirst {
        logger.lifecycle("Using npm at: $npmPath")
    }
}

tasks.register<Exec>("webideBuild") {
    description = "Build Web IDE frontend"
    dependsOn("webideNpmInstall")
    workingDir = webideFrontendDir
    commandLine = listOf(npmPath ?: "npm", "run", "build")
    inputs.dir(file("$webideFrontendDir/src"))
    inputs.file(file("$webideFrontendDir/index.html"))
    inputs.file(file("$webideFrontendDir/vite.config.ts"))
    outputs.dir(webideAssetsDir)
    onlyIf { npmPath != null }
    // Add node bin dir to PATH so npm can find node
    nodeBinDir?.let {
        environment("PATH", "$it:${System.getenv("PATH")}")
    }
    doFirst {
        logger.lifecycle("Building webide frontend with npm at: $npmPath")
    }
}

// Make preBuild depend on webideBuild only if frontend exists and npm is available
tasks.named("preBuild") {
    if (webideFrontendDir.exists() && file("$webideFrontendDir/package.json").exists()) {
        dependsOn("webideBuild")
    }
}

tasks.register("webideClean") {
    description = "Clean Web IDE frontend build artifacts"
    doLast {
        delete(webideAssetsDir)
        delete(file("$webideFrontendDir/node_modules"))
    }
}
