package com.topstep.wearkit.sample.ui.base

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.topstep.wearkit.sample.utils.promptToast

abstract class BaseActivity : AppCompatActivity() {

    private val toast by promptToast()

    fun toast(text: String) {
        lifecycleScope.launchWhenResumed {
            toast.showInfo(text)
        }
    }

    fun toast(@StringRes text: Int) {
        lifecycleScope.launchWhenResumed {
            toast.showInfo(text)
        }
    }

}
