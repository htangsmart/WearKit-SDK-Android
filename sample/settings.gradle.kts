pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/nexus/content/groups/public/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/nexus/content/groups/public/")
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
        jcenter() // Warning: this repository is going to shut down soon
        maven {
            url = uri("http://120.78.153.20:8081/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "sample"
include(":app")