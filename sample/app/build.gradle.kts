import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.text.SimpleDateFormat
import java.util.Date

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
        versionName = "1.0.0.2-${SimpleDateFormat("yyMMddHHmm").format(Date())}"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "isSupportFitCloud", "true")
        buildConfigField("boolean", "isSupportFlyWear", "true")
        buildConfigField("boolean", "isSupportShenJu", "true")
        buildConfigField("boolean", "isSupportProtoTb", "true")
        buildConfigField("boolean", "isSupportAbMate", "true")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

    flavorDimensions += "size"
    productFlavors {
        create("full") {
            dimension = "size"
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
        }
        create("lite") {
            dimension = "size"
            versionNameSuffix = "-lite"
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        //解决多个第三库里都包含这个so，导致无法编译的问题
        jniLibs.pickFirsts.add("**/libc++_shared.so")
        //解决多个"META-INF/INDEX.LIST"文件的问题
        resources.excludes.add("META-INF/INDEX.LIST")
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("wearkit")
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
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

// Reduce apk size of lite flavor
extensions.configure<ApplicationAndroidComponentsExtension>("androidComponents") {
    onVariants(selector().withFlavor("size" to "lite")) { variant ->
        variant.packaging.jniLibs.excludes.addAll(
            listOf(
                //sdk-flywear-adapter(Very few devices)
                "**/libpython3.11.so",
                "**/libcrypto1.1.so",
                "**/libsqlite3.so",
                "**/libssl1.1.so",
                "**/libpersimwear.so",
                "**/libffi.so",

                //vlc(Uncommon functions)
                "**/libvlc.so",
                "**/libvlcjni.so",

                //huawei scanplus(Uncommon functions)
                "**/libscannative.so",
            )
        )
    }
}

afterEvaluate {
    android.applicationVariants.configureEach {
        if (buildType.name != "debug") return@configureEach
        val variant = this
        val installTaskName = "install${variant.name.replaceFirstChar { it.uppercaseChar() }}"
        tasks.matching { it.name == installTaskName }.configureEach {
            doLast {
                val flavorName = variant.flavorName ?: "default"
                val oldApkFile = variant.outputs.first().outputFile
                val newApkFile = file(
                    "${layout.buildDirectory.get()}/outputs/apk/${flavorName}/debug/WearKit-sample-${flavorName}-v${variant.versionName}.apk"
                )
                newApkFile.parentFile.mkdirs()
                if (newApkFile.exists()) newApkFile.delete()
                if (oldApkFile.exists()) oldApkFile.copyTo(newApkFile, overwrite = true)
            }
        }
    }
}

dependencies {
    //WearKit Required
    val weakitVersion = "3.0.2.4-SNAPSHOT"
    val weakitChanging = weakitVersion.contains("SNAPSHOT")
    if (isDeveloperEnvironment()) {
        //For developer environment, use remote dependencies
        implementation("com.topstep.wearkit:sdk-core:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-flywear-adapter:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-shenju-adapter:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-prototb-adapter:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-abmate-adapter:$weakitVersion") { isChanging = weakitChanging }
        implementation("com.topstep.wearkit:sdk-helper:$weakitVersion") { isChanging = weakitChanging }
    } else {
        //For author environment, use local project
        implementation(project(":sdk-core"))
        implementation(project(":sdk-flywear-adapter"))
        implementation(project(":sdk-fitcloud-adapter"))
        implementation(project(":sdk-shenju-adapter"))
        implementation(project(":sdk-prototb-adapter"))
        if (hasSubmoduleAbMate()) {
            implementation(project(":sdk-abmate-adapter"))
        } else {
            implementation("com.topstep.wearkit:sdk-abmate-adapter:$weakitVersion") { isChanging = weakitChanging }
        }
        implementation(project(":sdk-helper"))
    }
    implementation(libs.timber)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.rxandroidble)
    implementation(libs.androidx.media)

    //RTSP playback - libVLC（Media3 RTSP 对非标 SDP 容错差，换 libVLC）
    implementation("org.videolan.android:libvlc-all:3.6.0")

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

    //protobuf
    implementation(libs.protobuf)

    implementation(libs.kilnn.wheelview)
    implementation("com.huawei.hms:scanplus:2.12.0.301")
}

fun isDeveloperEnvironment(): Boolean {
    return !project.projectDir.path.toString().contains("android-sdk-wearkit")
}

fun hasSubmoduleAbMate(): Boolean {
    val index = project.projectDir.path.indexOf("android-sdk-wearkit")
    if (index == -1) return false
    val parent = project.projectDir.path.take(index + "android-sdk-wearkit".length)
    val file = File(parent, "sdk-abmate-adapter/build.gradle.kts")
    return file.exists()
}