plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.microblink.blinkcard.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.microblink.blinkcard.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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


    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":lib-common"))
    implementation(libs.blinkcard.ux)
    // Use the following dependency if you want to use blinkcard-ux source module
    // instead of maven dependency, and remove implementation(libs.blinkcard.ux).
    // implementation(project(":blinkcard-ux"))
}
