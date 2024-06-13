package com.topstep.wearkit.sample.ui.music

import android.content.Context

import com.topstep.wearkit.apis.WKWearKit
import com.topstep.wearkit.base.utils.media.AbsMediaController

class MyMediaController(
    context: Context,
    val wkWearKit: WKWearKit,
) : AbsMediaController(context, true) {

    override fun isSupportMusicInfo(): Boolean {
        return true
    }

    override fun isSupportMusicState(): Boolean {
        return true
    }

}