plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.md7parvez.voiceassistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.md7parvez.voiceassistant"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
