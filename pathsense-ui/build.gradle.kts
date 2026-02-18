plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    id("signing")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":pathsense-core"))
                // Compose runtime required by the compose compiler plugin for all targets (iOS included).
                // Android resolves to the AndroidX artifact via version unification.
                implementation("org.jetbrains.compose.runtime:runtime:1.6.11")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.compose.ui:ui:1.7.6")
                implementation("androidx.compose.ui:ui-graphics:1.7.6")
                implementation("androidx.compose.foundation:foundation:1.7.6")
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test.ext:junit:1.2.1")
                implementation("androidx.test:core:1.6.1")
                implementation("androidx.test:runner:1.6.1")
            }
        }
        val iosMain by creating {
            dependsOn(commonMain)
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "com.dayushmand.pathsense.ui"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications.withType<MavenPublication> {
            groupId = project.group.toString()
            version = project.version.toString()

            pom {
                name.set("PathSense UI")
                description.set("UI components for PathSense gesture recognition SDK")
            }
        }
    }
}

signing {
    isRequired = findProperty("signing.keyId") != null
    sign(publishing.publications)
}
