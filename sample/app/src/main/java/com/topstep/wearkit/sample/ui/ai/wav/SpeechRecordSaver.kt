package com.topstep.wearkit.sample.ui.ai.wav

import android.content.Context
import androidx.core.content.ContextCompat
import com.topstep.aikit.model.AiAudioFormat
import com.topstep.wearkit.apis.model.speech.WKSpeechSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 录音场景落盘：保存到 `Android/data/<pkg>/files/speech_records/`。
 */
class SpeechRecordSaver(
    private val context: Context,
    private val fileSuffix: String,
) {

    constructor(context: Context, scene: WKSpeechSession.Scene) : this(
        context,
        when (scene) {
            WKSpeechSession.Scene.CALL_RECORD -> "call_record"
            else -> "record"
        },
    )

    private val writer = WavFileWriter(TAG)

    fun start(format: AiAudioFormat): Boolean {
        val file = createRecordFile(context, fileSuffix) ?: return false
        return writer.start(file, format)
    }

    fun write(data: ByteArray) = writer.write(data)

    /** @return 已保存的文件，失败或为空则 null */
    fun finish(): File? {
        return writer.finish()?.takeIf { it.exists() && it.length() > 0 }
    }

    companion object {
        private const val TAG = "SpeechRecordSaver"
        private const val DIR_NAME = "speech_records"
        const val SUFFIX_APP_RECORD = "app_record"

        fun recordDir(context: Context): File? {
            val parent = ContextCompat.getExternalFilesDirs(context, null).getOrNull(0) ?: return null
            val dir = File(parent, DIR_NAME)
            if (!dir.exists() && !dir.mkdirs()) {
                return null
            }
            return dir
        }

        /** 按修改时间取最近 [limit] 条 wav。 */
        fun latestRecords(context: Context, limit: Int = 3): List<File> {
            val dir = recordDir(context) ?: return emptyList()
            return dir.listFiles { f ->
                f.isFile && f.name.endsWith(".wav", true) && f.length() > 0
            }?.sortedByDescending { it.lastModified() }
                ?.take(limit.coerceAtLeast(0))
                .orEmpty()
        }

        private fun createRecordFile(context: Context, fileSuffix: String): File? {
            val dir = recordDir(context) ?: return null
            val prefix = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            return File(dir, "${prefix}_${fileSuffix}.wav")
        }
    }
}