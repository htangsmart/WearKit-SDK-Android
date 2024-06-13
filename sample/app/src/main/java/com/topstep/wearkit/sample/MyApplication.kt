package com.topstep.wearkit.sample

import androidx.multidex.MultiDexApplication
import com.github.kilnn.tool.system.SystemUtil
import com.topstep.wearkit.apis.WKWearKit
import com.topstep.wearkit.sample.data.PreferencesStorage
import com.topstep.wearkit.sample.ui.music.MyMediaController
import com.topstep.wearkit.sample.utils.log.AppLogger
import com.topstep.wearkit.sample.utils.log.MyCrashHandler

class MyApplication : MultiDexApplication() {

    companion object {
        @JvmStatic
        lateinit var instance: MyApplication

        @JvmStatic
        lateinit var wearKit: WKWearKit

        @JvmStatic
        lateinit var myMediaController: MyMediaController
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initAllProcess()
        if (SystemUtil.getProcessName(this) == packageName) {
            initMainProcess()
        }
    }

    private fun initAllProcess() {
        AppLogger.init(this)
        MyCrashHandler()
    }

    private fun initMainProcess() {
        wearKit = wearKitInit(this)
        PreferencesStorage.init(this)
        myMediaController = MyMediaController(this, wearKit)
    }

}
