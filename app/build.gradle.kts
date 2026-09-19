plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.md7parvez.voiceassistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.md7parvez.voiceassistant"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "0.4-neural"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    packaging {
        jniLibs {
            pickFirsts += setOf(
                "lib/arm64-v8a/libLiteRt.so",
                "lib/arm64-v8a/libLiteRtClGlAccelerator.so",
                "lib/x86_64/libLiteRt.so",
                "lib/x86_64/libLiteRtClGlAccelerator.so"
            )
        }
    }
}

dependencies {
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.0")
    testImplementation("junit:junit:4.13.2")
}
