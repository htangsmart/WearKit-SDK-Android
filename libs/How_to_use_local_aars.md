
How to use local aars?

# Base

```kotlin
dependencies {
    //All required
    implementation("aar/sdk-base-v{latest_version}.aar")
    implementation("aar/sdk-apis-v{latest_version}.aar")
    implementation("aar/sdk-core-v{latest_version}.aar")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.22"))
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("com.polidea.rxandroidble3:rxandroidble:1.17.2")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("androidx.room:room-ktx:2.5.0")
    implementation("androidx.room:room-rxjava3:2.5.0")
    ksp("androidx.room:room-compiler:2.5.0")
}
```

# FitCloud-SDK
```kotlin
dependencies {
    //Required
    implementation("aar/sdk-fitcloud-v{latest_version}.aar")
    implementation("aar/sdk-fitcloud-adapter-v{latest_version}.aar")
    implementation("aar/sdk-realtek-dfu-v{latest_version}.aar")//Use for dfu
    implementation("aar/sdk-realtek-bbpro-v{latest_version}.aar")//Use for MusicAbility,EBookAbility,AlbumAbility
    implementation("aar/sdk-realtek-file-v{latest_version}.jar")//Use for MusicAbility,EBookAbility,AlbumAbility
    implementation("androidx.palette:palette-ktx:1.0.0")//Use for create watchface bitmap

    //Optional
    implementation("aar/sdk-aliagent-v{latest_version}.aar")//Use for AliAgent(Very few watches have this feature)
}
```

## sdk-aliagent-v{latest_version}.aar
When you use this aar, you also need to add some additional dependencies.

```kotlin
dependencies {
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.google.firebase:firebase-crashlytics-buildtools:2.8.1")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.4")
    implementation("com.alibaba:fastjson:1.2.83")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.aliyun.dpa:oss-android-sdk:2.9.13")
}
```

# FlyWear-SDK
```kotlin
dependencies {
    //Required
    implementation("aar/sdk-flywear-v{latest_version}.aar")
    implementation("aar/sdk-flywear-adapter-v{latest_version}.aar")
    implementation("aar/sdk-persimwear-v{latest_version}.aar")
    implementation("com.belerweb:pinyin4j:2.5.0")//For contacts feature
}
```

# ShenJu-SDK
```kotlin
dependencies {
    //Required
    implementation("aar/sdk-shenju-base-v{latest_version}.aar")
    implementation("aar/sdk-shenju-core-v{latest_version}.aar")
    implementation("aar/sdk-shenju-opencv-v{latest_version}.aar")//For create video watchface
    implementation("aar/sdk-shenju-adapter-v{latest_version}.aar")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.arthenica:ffmpeg-kit-min:6.0-2")//For create video watchface
}
```

# Others
```kotlin
//If use AbsMediaController class in <sdk-base>
implementation("androidx.media:media:1.6.0")

```