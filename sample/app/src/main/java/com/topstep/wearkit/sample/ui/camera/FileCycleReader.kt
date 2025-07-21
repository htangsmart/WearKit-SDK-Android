package com.topstep.wearkit.sample.ui.camera

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class FileCycleReader(private val file: File) : FileTestReader {
    private var reader: BufferedReader? = null

    @Throws(IOException::class)
    override fun readLine(): String? {
        var r = this.reader
        if (r == null) {
            r = BufferedReader(FileReader(file)).also {
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