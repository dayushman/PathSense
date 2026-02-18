plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }
    val xcf = org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkConfig(project, "PathSenseCore")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "PathSenseCore"
            xcf.add(this)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
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
    namespace = "com.dayushmand.pathsense.core"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = project.group.toString()
            artifactId = "pathsense-core"
            version = project.version.toString()
        }
    }
}

signing {
    // Configure signing credentials in your Gradle user home or CI secrets.
    sign(publishing.publications)
}
