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
            url = uri("https://maven.topstepht.com/repository/maven-public/")
        }
        //sdk-fitcloud中com.github.artillerymans.Core:paycertification:leadingSmart_1.0.50依赖需要此配置
        maven {
            credentials {
                username = "5ff28ca9ed01613630f9d551"
                password = "cxN-HwJ]yzST"
            }
            url= uri("https://packages.aliyun.com/6718aa5c2c78927f26d82a35/maven/mltcloudai")
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
    include(":sdk-prototb-adapter")
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
    project(":sdk-prototb-adapter").projectDir = file("../../sdk-prototb-adapter")
    project(":sdk-helper").projectDir = file("../../sdk-helper")
}

/**
 * Developers and authors may use different dependencies
 */
fun isDeveloperEnvironment(): Boolean {
    return !rootProject.projectDir.path.toString().contains("android-sdk-wearkit")
}