plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tvlinkremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tvlinkremote"
        minSdk = 28
        targetSdk = 32
        versionCode = 1
        versionName = "0.1.0"
    }
}
