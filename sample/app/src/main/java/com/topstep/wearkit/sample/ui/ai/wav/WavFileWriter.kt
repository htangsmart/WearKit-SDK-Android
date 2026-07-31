package com.topstep.wearkit.sample.ui.ai.wav

import com.topstep.aikit.model.AiAudioFormat
import com.topstep.opus.tool.OpusDecoder
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * 将会话音频写入 wav/pcm 文件（opus 先解码再写）。
 */
class WavFileWriter(
    private val tag: String,
    private val saveAsWav: Boolean = true,
) {

    private var format: AiAudioFormat? = null
    private var opusDecoder: OpusDecoder? = null
    private var pcmFos: FileOutputStream? = null
    private var pcmFile: File? = null
    private var pcmDataSize = 0

    fun start(file: File, format: AiAudioFormat): Boolean {
        this.format = format
        return runCatching {
            pcmFile = file
            pcmDataSize = 0
            pcmFos = FileOutputStream(file).also { fos ->
                if (saveAsWav) {
                    beginWavFile(fos)
                }
            }
            Timber.Forest.tag(tag).i("save audio to: %s", file.absolutePath)
            true
        }.getOrElse {
            Timber.Forest.tag(tag).w(it, "start failed")
            pcmFos = null
            pcmFile = null
            false
        }
    }

    fun write(data: ByteArray) {
        val fos = pcmFos ?: return
        val format = format ?: return
        if (data.isEmpty()) return
        try {
            val decoder = opusDecoder ?: if (format.isOpus) {
                OpusDecoder(PCM_SAMPLE_RATE, PCM_CHANNELS, 8).also { opusDecoder = it }
            } else {
                null
            }
            if (decoder != null) {
                decoder.decode(data, format.opusFrameSize)?.forEach { d ->
                    fos.write(d)
                    pcmDataSize += d.size
                }
            } else {
                fos.write(data)
                pcmDataSize += data.size
            }
        } catch (e: Exception) {
            Timber.Forest.tag(tag).w(e, "write failed, size=%d", data.size)
        }
    }

    /** @return 已写入的文件；失败或未开始则 null */
    fun finish(): File? {
        var result: File? = null
        runCatching {
            pcmFos?.let { fos ->
                fos.flush()
                if (saveAsWav) {
                    endWavFile(fos)
                }
                fos.close()
            }
            pcmFile?.let {
                result = it
                Timber.Forest.tag(tag).i("audio saved: %s pcmBytes=%d", it.absolutePath, pcmDataSize)
            }
        }.onFailure {
            Timber.Forest.tag(tag).w(it, "finish failed")
        }
        opusDecoder?.release()
        opusDecoder = null
        pcmFos = null
        pcmFile = null
        pcmDataSize = 0
        format = null
        return result
    }

    private fun beginWavFile(fos: FileOutputStream) {
        fos.write(generateWavHeader(0, 0))
        fos.flush()
    }

    private fun endWavFile(fos: FileOutputStream) {
        fos.channel.position(0)
        val totalDataLen = pcmDataSize + 36
        fos.write(generateWavHeader(pcmDataSize.toLong(), totalDataLen.toLong()))
        fos.flush()
    }

    private fun generateWavHeader(totalAudioLen: Long, totalDataLen: Long): ByteArray {
        val sampleRate = PCM_SAMPLE_RATE
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()

        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        header[20] = 1
        header[21] = 0

        header[22] = channels.toByte()
        header[23] = 0

        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()

        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()

        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0

        header[34] = bitsPerSample.toByte()
        header[35] = 0

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        return header
    }

    companion object {
        private const val PCM_SAMPLE_RATE = 16000
        private const val PCM_CHANNELS = 1
    }
}