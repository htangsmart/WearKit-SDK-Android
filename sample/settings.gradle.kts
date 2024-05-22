pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public/") }
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://mvn.mob.com/android") }
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/nexus/content/groups/public/") }
        maven { url = uri("https://mvn.0110.be/releases") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://mvn.mob.com/android") }
        maven { url = uri("https://developer.huawei.com/repo/") }
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