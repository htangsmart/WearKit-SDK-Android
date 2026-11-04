package com.topstep.wearkit.sample.ui.others

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decode any audio [Uri] to signed 16-bit LE PCM at 16 kHz mono.
 *
 * Decodes the whole track once. Callers loop the returned buffer themselves —
 * avoids MediaCodec EOS/`flush` restart issues (common with AAC in MP4).
 */
internal object Pcm16kMonoDecoder {

    const val TARGET_SAMPLE_RATE = 16_000
    const val TARGET_CHANNELS = 1

    fun decodeAll(context: Context, uri: Uri): ByteArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context.applicationContext, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("Missing mime")
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            codec = MediaCodec.createDecoderByType(mime).also {
                it.configure(format, null, null, 0)
                it.start()
            }
            val decoder = codec
            val mono = ArrayList<Short>(sampleRate.coerceAtLeast(TARGET_SAMPLE_RATE))
            val info = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var spins = 0

            while (!outputEos) {
                if (++spins > MAX_TOTAL_SPINS) {
                    error("Decode timeout")
                }
                if (!inputEos) {
                    val inIndex = decoder.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inBuf = decoder.getInputBuffer(inIndex)
                        if (inBuf != null) {
                            val size = extractor.readSampleData(inBuf, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(
                                    inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputEos = true
                            } else {
                                decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                when (val outIndex = decoder.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = decoder.outputFormat
                        if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                .coerceAtLeast(1)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                            outFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)
                        ) {
                            pcmEncoding = outFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (outIndex >= 0) {
                        val outBuf = decoder.getOutputBuffer(outIndex)
                        if (outBuf != null && info.size > 0) {
                            appendDecodedMono(outBuf, info, channels, pcmEncoding, mono)
                        }
                        val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (eos) outputEos = true
                    }
                }
            }

            if (mono.isEmpty()) error("Decoded PCM is empty")
            return resampleToTarget(mono.toShortArray(), sampleRate)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun appendDecodedMono(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        channels: Int,
        pcmEncoding: Int,
        mono: ArrayList<Short>,
    ) {
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        val ordered = buffer.order(ByteOrder.LITTLE_ENDIAN)
        when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val floats = FloatArray(info.size / 4)
                ordered.asFloatBuffer().get(floats)
                val frames = floats.size / channels
                for (frame in 0 until frames) {
                    var sum = 0f
                    for (ch in 0 until channels) {
                        sum += floats[frame * channels + ch]
                    }
                    mono.add(
                        (sum / channels * Short.MAX_VALUE)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    )
                }
            }
            else -> {
                val shorts = ShortArray(info.size / 2)
                ordered.asShortBuffer().get(shorts)
                val frames = shorts.size / channels
                for (frame in 0 until frames) {
                    var sum = 0
                    for (ch in 0 until channels) {
                        sum += shorts[frame * channels + ch].toInt()
                    }
                    mono.add((sum / channels).toShort())
                }
            }
        }
    }

    private fun resampleToTarget(input: ShortArray, inputRate: Int): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        if (inputRate == TARGET_SAMPLE_RATE) {
            return shortsToBytes(input)
        }
        val outCount = (input.size.toLong() * TARGET_SAMPLE_RATE / inputRate).toInt().coerceAtLeast(1)
        val out = ShortArray(outCount)
        val step = inputRate.toDouble() / TARGET_SAMPLE_RATE
        var cursor = 0.0
        for (i in 0 until outCount) {
            val index = cursor.toInt().coerceIn(0, input.lastIndex)
            val next = (index + 1).coerceAtMost(input.lastIndex)
            val frac = cursor - index
            val s0 = input[index].toInt()
            val s1 = input[next].toInt()
            out[i] = (s0 + (s1 - s0) * frac).toInt().toShort()
            cursor += step
        }
        return shortsToBytes(out)
    }

    private fun shortsToBytes(samples: ShortArray): ByteArray {
        val out = ByteArrayOutputStream(samples.size * 2)
        for (s in samples) {
            out.write(s.toInt() and 0xFF)
            out.write((s.toInt() shr 8) and 0xFF)
        }
        return out.toByteArray()
    }

    private const val DEQUEUE_TIMEOUT_US = 10_000L
    private const val MAX_TOTAL_SPINS = 500_000
}
