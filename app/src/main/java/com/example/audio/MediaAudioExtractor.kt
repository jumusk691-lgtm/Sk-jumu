package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.example.dsp.WavHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MediaAudioExtractor {
    private const val TAG = "MediaAudioExtractor"

    data class ExtractedAudio(
        val samples: ShortArray,
        val sampleRate: Int,
        val channels: Int
    )

    /**
     * Extracts PCM 16-bit audio from an Audio or Video URI.
     */
    suspend fun extractPcm(context: Context, uri: Uri): ExtractedAudio? = withContext(Dispatchers.IO) {
        // Try simple WAV parser first for WAV files
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val (wavSamples, wavSampleRate) = WavHelper.readPcmFromWav(stream)
                if (wavSamples.isNotEmpty()) {
                    return@withContext ExtractedAudio(wavSamples, wavSampleRate, 1)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Not a standard WAV stream, attempting MediaExtractor: ${e.message}")
        }

        // Use Android MediaExtractor & MediaCodec for Video/Audio formats (MP4, MP3, M4A, etc.)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                Log.w(TAG, "No audio track found in media file")
                extractor.release()
                return@withContext null
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 16000
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var isEOS = false
            val pcmDataList = mutableListOf<Short>()
            val timeoutUs = 5000L
            val maxSamples = 16000 * 30 // Cap to 30 seconds for quick mobile responsiveness

            while (!isEOS && pcmDataList.size < maxSamples) {
                val inputBufIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputBufIndex >= 0) {
                    val inputBuf = codec.getInputBuffer(inputBufIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuf, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }

                var outputBufIndex = codec.dequeueOutputBuffer(info, timeoutUs)
                while (outputBufIndex >= 0) {
                    val outputBuf = codec.getOutputBuffer(outputBufIndex)
                    if (outputBuf != null && info.size > 0) {
                        outputBuf.position(info.offset)
                        outputBuf.limit(info.offset + info.size)
                        val shortBuf = outputBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shortBuf.hasRemaining() && pcmDataList.size < maxSamples) {
                            val sample = shortBuf.get()
                            // Downmix stereo to mono if needed
                            if (channelCount == 2) {
                                if (shortBuf.hasRemaining()) {
                                    val r = shortBuf.get()
                                    pcmDataList.add(((sample.toInt() + r.toInt()) / 2).toShort())
                                } else {
                                    pcmDataList.add(sample)
                                }
                            } else {
                                pcmDataList.add(sample)
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEOS = true
                        break
                    }
                    outputBufIndex = codec.dequeueOutputBuffer(info, timeoutUs)
                }
            }

            if (pcmDataList.isNotEmpty()) {
                val shortArray = ShortArray(pcmDataList.size)
                for (i in pcmDataList.indices) {
                    shortArray[i] = pcmDataList[i]
                }
                return@withContext ExtractedAudio(shortArray, sampleRate, 1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio with MediaCodec: ${e.message}", e)
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {}
            try {
                extractor.release()
            } catch (e: Exception) {}
        }

        null
    }
}
