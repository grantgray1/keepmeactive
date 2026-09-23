plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release builds take their version from the git tag, via the CI workflow.
// Local builds just get 0.0.0-dev, which is fine because they never get published.
val appVersionName: String = System.getenv("APP_VERSION_NAME") ?: "0.0.0-dev"
val appVersionCode: Int = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1

// Set by the release workflow when a signing keystore is available. Without it we fall back
// to the debug key, which still installs but cannot be updated over. See RELEASING.md.
val keystorePath: String? = System.getenv("KEYSTORE_PATH")
val hasReleaseKey: Boolean = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

android {
    namespace = "com.keepmeactive"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.keepmeactive"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("release")
            } else {
                // Lets `assembleRelease` produce an installable APK with no keystore set up.
                signingConfigs.getByName("debug")
            }
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
