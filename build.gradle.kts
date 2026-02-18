plugins {
    kotlin("multiplatform") version "2.0.20" apply false
    kotlin("android") version "2.0.20" apply false
    id("com.android.library") version "8.9.2" apply false
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}

group = "com.dayushman.pathsense"
version = "0.0.1-alpha"

subprojects {
    group = rootProject.group
    version = rootProject.version
}
