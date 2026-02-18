plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dayushmand.pathsense.sample.compose"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dayushmand.pathsense.sample.compose"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.github.dayushman.PathSense:pathsense-ui:0.0.3-alpha")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("com.google.android.material:material:1.12.0")
}
