plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.md7parvez.voiceassistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.md7parvez.voiceassistant"
        minSdk = 23
        targetSdk = 35
        versionCode = 5
        versionName = "0.5-y01-neural"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation("com.google.mediapipe:tasks-genai:0.10.27")
    testImplementation("junit:junit:4.13.2")
}
