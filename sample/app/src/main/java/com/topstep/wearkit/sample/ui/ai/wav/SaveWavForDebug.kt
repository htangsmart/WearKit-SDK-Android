package com.topstep.wearkit.sample.ui.ai.wav

import android.content.Context
import androidx.core.content.ContextCompat
import com.topstep.aikit.AiKit
import com.topstep.aikit.model.AiAudioFormat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * **仅用于 Debug**：将会话音频落盘为 wav/pcm，便于本地播放排查识别问题。
 *
 * 文件目录：App 外部私有目录 `Android/data/<pkg>/files/`。
 */
class SaveWavForDebug(private val context: Context) {

    private val writer = WavFileWriter(TAG, saveAsWav = AiKit.Companion.AUDIO_FILE_FORMAT_WAV)

    fun start(format: AiAudioFormat) {
        val file = generateRecordFile(context, AiKit.Companion.AUDIO_FILE_FORMAT_WAV) ?: return
        writer.start(file, format)
    }

    fun write(data: ByteArray) = writer.write(data)

    fun finish() {
        lastSavedFile = writer.finish()
    }

    companion object {
        private const val TAG = "SaveWavForDebug"

        @Volatile
        var lastSavedFile: File? = null
            private set

        /** 最近一次落盘文件；若内存无记录则按修改时间取目录内最新的 `*_audio.wav` / `*_audio.pcm`。 */
        fun latestRecordFile(context: Context): File? {
            lastSavedFile?.takeIf { it.exists() && it.length() > 0 }?.let { return it }
            val parent = ContextCompat.getExternalFilesDirs(context, null).getOrNull(0) ?: return null
            return parent.listFiles { f ->
                f.isFile && (f.name.endsWith("_audio.wav", true) || f.name.endsWith("_audio.pcm", true))
            }?.maxByOrNull { it.lastModified() }?.takeIf { it.length() > 0 }
        }

        /** 与 [AiKit.Companion.generateRecordFile] 相同的默认路径规则。 */
        private fun generateRecordFile(context: Context, saveAsWav: Boolean): File? {
            val parent = ContextCompat.getExternalFilesDirs(context, null).getOrNull(0) ?: return null
            if (!parent.exists() && !parent.mkdirs()) {
                return null
            }
            val format = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            return File(parent, format.format(Date()) + if (saveAsWav) "_audio.wav" else "_audio.pcm")
        }
    }
}