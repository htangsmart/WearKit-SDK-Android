package com.topstep.wearkit.sample.files

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File

object AppFiles {
    private const val TAG = "AppFiles"

    fun dirDownload(context: Context): File? {
        val dir = ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_DOWNLOADS).firstOrNull() ?: return null
        if (!dir.exists() && !dir.mkdirs()) {
            return null
        }
        return dir
    }

}