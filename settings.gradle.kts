pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://maven.kikugie.dev/releases/") {
            name = "KikuGie Releases"
        }
        maven("https://maven.kikugie.dev/snapshots/") {
            name = "KikuGie Snapshots"
        }
        maven("https://maven-central.storage-download.googleapis.com/maven2/") {
            name = "CentralMirror"
        }
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
}

stonecutter {
    create(rootProject) {
        versions("1.21.1", "1.21.8", "1.21.10", "1.21.11", "26.1.2", "26.2")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "fogxy"
