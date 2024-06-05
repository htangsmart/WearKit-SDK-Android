# WearKit-SDK-Android

## 1.Add maven config in project `setting.gradle`:
```
dependencyResolutionManagement {
	...
    repositories {
        ...
        maven {
            allowInsecureProtocol = true
            url "http://120.78.153.20:8081/repository/maven-public/"
        }
    }
}
```

## 2.Add dependencies in module `build.gradle`:
```
dependencies{
    ...
    def weakit_version = "3.0.1-SNAPSHOT"
    implementation("com.topstep.wearkit:sdk-core:$weakit_version")
    implementation("com.topstep.wearkit:sdk-flywear-adapter:$weakit_version")
    implementation("com.topstep.wearkit:sdk-fitcloud-adapter:$weakit_version")
    implementation("com.topstep.wearkit:sdk-shenju-adapter:$weakit_version")
    implementation("com.topstep.wearkit:sdk-helper:$weakit_version")
}
```

## 3.Init
1. Use `wearKitInit` to obtain `WKWearKit`. As shown in the following example code.
2. Use `wearKit.scanner` to scan device
3. Use `wearKit.connector` to connect device
4. Use `weatkit.***Ability` to perform various device operations.

```
package com.topstep.wearkit.sample

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.polidea.rxandroidble3.RxBleClient
import com.topstep.wearkit.apis.WKWearKit
import com.topstep.wearkit.base.ProcessLifecycleManager
import com.topstep.wearkit.core.buildWKWearKit
import com.topstep.wearkit.fitcloud.WKFitCloudKit
import com.topstep.wearkit.flywear.WKFlyWearKit
import com.topstep.wearkit.sample.utils.log.AppLogger
import io.reactivex.rxjava3.exceptions.CompositeException
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import timber.log.Timber

fun wearKitInit(application: Application): WKWearKit {
    /**
     * ToNote:
     * Configure log. "WearKit-SDK" use the Timber to output log, so you need to configure the Timber.
     * Because [AppLogger] has already init Timber. So it's commented out here.
     */
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
    } else {
        Timber.plant(object : Timber.DebugTree() {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return priority > Log.DEBUG
            }
        })
    }

    /**
     * ToNote:
     * 2.Create wearKit
     */
    val builders = ArrayList<WKWearKit.Builder>()
    val processLifecycleObserver = MyProcessLifecycleManager().also {
        application.registerActivityLifecycleCallbacks(it)
    }
    val rxBleClient = RxBleClient.create(application)
    builders.add(
        WKFitCloudKit.Builder(application, processLifecycleObserver, rxBleClient)
    )
    builders.add(
        WKFlyWearKit.Builder(application, processLifecycleObserver, rxBleClient)
    )
//    if (BuildConfig.isSupportShenJu) {
//        builders.add(
//            WKShenJuKit.Builder(application, processLifecycleObserver, rxBleClient)
//        )
//    }
    val wearKit = buildWKWearKit(builders)

    /**
     * ToNote:
     * 3.RxJavaPlugins.setErrorHandler
     * Because rxjava is used in the SDK, some known exceptions that cannot be distributed need to be handled to avoid app crash.
     */
    val ignoreExceptions = HashSet<Class<out Throwable>>()
//    ignoreExceptions.add(YourAppIgnoredException::class.java)//Exceptions need to be ignored in your own app (maybe not according to your own app)
    ignoreExceptions.addAll(wearKit.rxJavaPluginsIgnoreExceptions())//Exceptions need to be ignored in the SDK
    RxJavaPlugins.setErrorHandler(RxJavaPluginsErrorHandler(ignoreExceptions))

    return wearKit
}

private class MyProcessLifecycleManager : ProcessLifecycleManager(), Application.ActivityLifecycleCallbacks {
    private var startCount = 0
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        if (startCount == 0) {
            //At this time, the APP enters the foreground
            setForeground(true)
        }
        startCount++
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        startCount--
        if (startCount == 0) {
            //At this time, the APP enters the background
            setForeground(false)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}

private class RxJavaPluginsErrorHandler(
    /**
     * Exception types that can be ignored
     */
    private val ignores: Set<Class<out Throwable>>,
) : Consumer<Throwable> {

    override fun accept(t: Throwable) {
        if (handle(t)) return
        throw RuntimeException(t)
    }

    /**
     * Handle exception
     * @param throwable
     * @return True for handled, false for not
     */
    private fun handle(throwable: Throwable): Boolean {
        val cause = if (throwable is UndeliverableException) {
            throwable.cause
        } else {
            throwable
        } ?: return true

        if (cause is CompositeException) {
            var nullCount = 0
            for (e in cause.exceptions) {
                if (e == null) {
                    nullCount++
                } else if (isIgnore(e)) {
                    return true
                }
            }

            if (nullCount == cause.exceptions.size) {
                return true
            }
        } else if (isIgnore(cause)) {
            return true
        }

        return false
    }

    private fun isIgnore(throwable: Throwable): Boolean {
        val clazz = throwable::class.java
        for (ignore in ignores) {
            if (ignore.isAssignableFrom(clazz)) {
                return true
            }
        }
        return false
    }
}
```
