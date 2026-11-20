import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ── Release signing ─────────────────────────────────────────────────────────
// Put keystore.properties at the root of the project (git-ignored).
// If missing the release build still assembles but is unsigned.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    namespace   = "com.Lia.assistant"
    compileSdk  = 36

    defaultConfig {
        applicationId = "com.Lia.assistant"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Signing ─────────────────────────────────────────────────────────────
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias     = keystoreProperties["keyAlias"]     as String
                keyPassword  = keystoreProperties["keyPassword"]  as String
                storeFile    = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    // ── Build types ─────────────────────────────────────────────────────────
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            isDebuggable        = true
        }
        release {
            isMinifyEnabled     = true
            isShrinkResources   = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // ── Flavors ─────────────────────────────────────────────────────────────
    flavorDimensions += "distribution"

    productFlavors {
        // Full power: AccessibilityService, visual agents, access-key licensing.
        create("direct") {
            dimension        = "distribution"
            applicationIdSuffix = ".direct"
            versionNameSuffix   = "-direct"
            resValue("string", "app_label", "Lia AI")
        }
        // Google Play: accessibility & SYSTEM_ALERT_WINDOW removed at compile time.
        create("play") {
            dimension        = "distribution"
            resValue("string", "app_label", "Lia AI")
        }
    }

    // ── Source sets ─────────────────────────────────────────────────────────
    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
            res.srcDirs("src/main/res")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
        getByName("direct") {
            kotlin.srcDirs("src/direct/kotlin")
            manifest.srcFile("src/direct/AndroidManifest.xml")
        }
        getByName("play") {
            kotlin.srcDirs("src/play/kotlin")
            manifest.srcFile("src/play/AndroidManifest.xml")
        }
        getByName("test")       { kotlin.srcDirs("src/test/kotlin") }
        getByName("testDirect") { kotlin.srcDirs("src/testDirect/kotlin") }
        getByName("testPlay")   { kotlin.srcDirs("src/testPlay/kotlin") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose      = true
        buildConfig  = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // ── Compose BOM ─────────────────────────────────────────────────────────
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    debugImplementation(composeBom)

    // ── Core ─────────────────────────────────────────────────────────────────
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.activity.compose)

    // ── Lifecycle ───────────────────────────────────────────────────────────
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // ── Compose UI ──────────────────────────────────────────────────────────
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)

    // ── Navigation ──────────────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Async ───────────────────────────────────────────────────────────────
    implementation(libs.coroutines.android)

    // ── Persistence ─────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── Network (OkHttp only — no Retrofit, no Gson) ────────────────────────
    implementation(libs.okhttp)

    // ── WebView helper ──────────────────────────────────────────────────────
    implementation(libs.webkit)

    // ── Debug ───────────────────────────────────────────────────────────────
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ── Unit tests ──────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.org.json)
    testImplementation(libs.mockwebserver)
}
