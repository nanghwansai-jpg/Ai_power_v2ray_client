plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseSigningEnv = listOf(
    System.getenv("ANDROID_KEYSTORE_FILE"),
    System.getenv("ANDROID_KEYSTORE_PASSWORD"),
    System.getenv("ANDROID_KEY_ALIAS"),
    System.getenv("ANDROID_KEY_PASSWORD"),
)
val releaseSigningConfigured = releaseSigningEnv.all { !it.isNullOrBlank() }
val releaseSigningPartiallyConfigured = releaseSigningEnv.any { !it.isNullOrBlank() } &&
    !releaseSigningConfigured

if (releaseSigningPartiallyConfigured) {
    throw GradleException("Release signing is partially configured; all ANDROID_* signing values are required.")
}

val releaseKeystoreFile = releaseSigningEnv[0]?.let(::file)
if (releaseSigningConfigured && (releaseKeystoreFile == null || !releaseKeystoreFile.isFile)) {
    throw GradleException("Configured release keystore file does not exist.")
}

android {
    namespace = "com.aipower.v2ray"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aipower.v2ray"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        create("release") {
            // Release workflow injects these values from GitHub Actions secrets.
            // Never commit the keystore or passwords to the repository.
            storeFile = System.getenv("ANDROID_KEYSTORE_FILE")?.let { file(it) }
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("release") {
            // PR CI can build an unsigned release variant for DEX inspection.\n            // Actual release jobs provide all signing env vars and use the protected keystore.\n            signingConfig = if (releaseSigningConfigured) signingConfigs.getByName("release") else null
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    // Generated from the pinned XTLS/libXray source by scripts/build-libxray-android.sh.
    implementation(fileTree("libs") { include("*.aar") })
}
