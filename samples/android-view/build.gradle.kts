plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.dayushmand.pathsense.sample.view"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dayushmand.pathsense.sample.view"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
    implementation("com.github.dayushman.PathSense:pathsense-ui:0.0.2-alpha")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
