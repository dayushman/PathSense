pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "PathSenseSDK"

include(":pathsense-core")
include(":pathsense-ui")
include(":samples:android-view")
include(":samples:android-compose")
