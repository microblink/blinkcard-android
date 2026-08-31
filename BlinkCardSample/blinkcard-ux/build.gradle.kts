plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.microblink.blinkcard.ux"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
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

    sourceSets {
        getByName("main") {
            // Reuse BlinkCard UX source module from this mono-repo setup.
            java.srcDirs("../../../../BlinkCard/blinkcard-ux/src/main/kotlin")
            res.srcDirs("../../../../BlinkCard/blinkcard-ux/src/main/res")
            manifest.srcFile("../../../../BlinkCard/blinkcard-ux/src/main/AndroidManifest.xml")
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
        // Fix for R8 StringConcatFactory issues on some toolchains.
        freeCompilerArgs.add("-Xstring-concat=inline")
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    api(libs.blinkcard.core)

    api(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.material3)
    api(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.extensions)
}
