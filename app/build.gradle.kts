plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.stark.jarvis"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stark.jarvis"
        minSdk = 26            // API 26 (Android 8.0) — TYPE_APPLICATION_OVERLAY + foreground services
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        }
    }
}

dependencies {
    // --- Core / lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)          // LifecycleService for the overlay service
    implementation(libs.androidx.lifecycle.runtime.compose)  // collectAsStateWithLifecycle
    implementation(libs.androidx.activity.compose)

    // --- Jetpack Compose (BOM-managed) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)         // Canvas / drawing
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- Animation: Lottie (optional vector HUD assets) ---
    implementation(libs.lottie.compose)

    // --- Audio: Media3/ExoPlayer for the sustained hum; SoundPool (framework) for SFX ---
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)

    // --- Test ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
