package com.topstep.wearkit.sample.ui.camera

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

interface FileTestReader {
    fun readLine(): String?
    fun close()
}

class FileFixReader(
    private val context: Context,
) : FileTestReader {
    private var reader: BufferedReader? = null

    @Throws(IOException::class)
    override fun readLine(): String? {
        var r = this.reader
        if (r == null) {
            r = BufferedReader(InputStreamReader(context.assets.open("test.h264"))).also {
                this.reader = it
            }
        }
        var line = r.readLine()
        if (line == null) {
            // 读到末尾，把reader设置为null
            r.close()
            this.reader = null
        }
        return line
    }

    override fun close() {
        this.reader?.close()
    }

}