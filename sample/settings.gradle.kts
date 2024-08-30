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

if (!isDeveloperEnvironment()) {//Developers ignored this
    include(":sdk-base")
    include(":sdk-apis")
    include(":sdk-core")
    include(":sdk-flywear")
    include(":sdk-flywear-adapter")
    include(":sdk-fitcloud")
    include(":sdk-fitcloud-adapter")
    include(":sdk-shenju:base")
    include(":sdk-shenju:core")
    include(":sdk-shenju:opencv")
    include(":sdk-shenju-adapter")
    include(":sdk-helper")
    project(":sdk-base").projectDir = file("../../sdk-base")
    project(":sdk-apis").projectDir = file("../../sdk-apis")
    project(":sdk-core").projectDir = file("../../sdk-core")
    project(":sdk-flywear").projectDir = file("../../sdk-flywear")
    project(":sdk-flywear-adapter").projectDir = file("../../sdk-flywear-adapter")
    project(":sdk-fitcloud").projectDir = file("../../sdk-fitcloud")
    project(":sdk-fitcloud-adapter").projectDir = file("../../sdk-fitcloud-adapter")
    project(":sdk-shenju:base").projectDir = file("../../sdk-shenju/base")
    project(":sdk-shenju:core").projectDir = file("../../sdk-shenju/core")
    project(":sdk-shenju:opencv").projectDir = file("../../sdk-shenju/opencv")
    project(":sdk-shenju-adapter").projectDir = file("../../sdk-shenju-adapter")
    project(":sdk-helper").projectDir = file("../../sdk-helper")
}

/**
 * Developers and authors may use different dependencies
 */
fun isDeveloperEnvironment(): Boolean {
    return !rootProject.projectDir.path.toString().contains("android-sdk-wearkit")
}