plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
}
kotlin {
    jvmToolchain(8)
}
android {
    signingConfigs {
        create("wearkit") {
            storeFile = file("keystore/debug.keystore")
            storePassword = "android"
            keyPassword = "android"
            keyAlias = "androiddebugkey"
        }
    }

    namespace = "com.topstep.wearkit.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.topstep.wearkit.sample"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "isSupportFitCloud", "true")
        buildConfigField("boolean", "isSupportFlyWear", "true")
        buildConfigField("boolean", "isSupportShenJu", "true")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("wearkit")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("wearkit")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    configurations.configureEach {
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
        resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
    }
}

dependencies {
    //WearKit Required
    if (isDeveloperEnvironment()) {
        //For developer environment, use remote dependencies
        val weakitVersion = "3.0.1-SNAPSHOT"
        val weakitChanging = weakitVersion.contains("SNAPSHOT")
        implementation("com.topstep.wearkit:sdk-core:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-flywear-adapter:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-shenju-adapter:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-helper:$weakitVersion") { isChanging = weakitChanging }
    } else {
        //For author environment, use local project
        implementation(project(":sdk-core"))
        implementation(project(":sdk-flywear-adapter"))
        implementation(project(":sdk-fitcloud-adapter"))
        implementation(project(":sdk-shenju-adapter"))
        implementation(project(":sdk-helper"))
    }
    implementation(libs.timber)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.rxandroidble)
    implementation(libs.androidx.media)

    //Base
    implementation(platform(libs.kotlin.coroutines.bom))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx3")
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraint)

    //JetPack-Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //JetPack-Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    //JetPack-multidex
    implementation(libs.androidx.multidex)

    //Moshi
    implementation(libs.moshi)
    ksp(libs.moshi.compiler)

    //permission
    implementation(libs.permissionx)

    //retrofit2
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.moshi)
    implementation(libs.okhttp3.logging)

    //others
    implementation(libs.kilnn.toolkit)
    implementation(libs.mars.xlog)

    //glide
    implementation(libs.glide)
    ksp(libs.glide.compiler)
    implementation(libs.glide.okhttp3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)

    //JetPack-CameraX
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.window)

    implementation(libs.kilnn.wheelview)
}

fun isDeveloperEnvironment(): Boolean {
    return !project.projectDir.path.toString().contains("android-sdk-wearkit")
}