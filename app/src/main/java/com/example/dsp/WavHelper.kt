package com.example.dsp

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility for reading, writing, and manipulating standard 16-bit PCM WAV audio files.
 */
object WavHelper {

    fun writeWavHeader(
        out: FileOutputStream,
        sampleRate: Int,
        channels: Short,
        bitsPerSample: Short,
        pcmDataLength: Int = 0
    ) {
        val totalDataLen = pcmDataLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen)
        buffer.put("WAVE".toByteArray())

        // "fmt " sub-chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size (16 for PCM)
        buffer.putShort(1) // AudioFormat (1 for PCM)
        buffer.putShort(channels)
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(bitsPerSample)

        // "data" sub-chunk
        buffer.put("data".toByteArray())
        buffer.putInt(pcmDataLength)

        out.write(header)
    }

    fun updateWavDataLength(file: File, pcmDataLength: Int) {
        try {
            RandomAccessFile(file, "rw").use { raf ->
                val totalDataLen = pcmDataLength + 36
                // Update RIFF chunk size at byte 4
                raf.seek(4)
                raf.write(
                    byteArrayOf(
                        (totalDataLen and 0xff).toByte(),
                        ((totalDataLen shr 8) and 0xff).toByte(),
                        ((totalDataLen shr 16) and 0xff).toByte(),
                        ((totalDataLen shr 24) and 0xff).toByte()
                    )
                )

                // Update data chunk size at byte 40
                raf.seek(40)
                raf.write(
                    byteArrayOf(
                        (pcmDataLength and 0xff).toByte(),
                        ((pcmDataLength shr 8) and 0xff).toByte(),
                        ((pcmDataLength shr 16) and 0xff).toByte(),
                        ((pcmDataLength shr 24) and 0xff).toByte()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Parse audio samples from a 16-bit PCM WAV input stream.
     */
    fun readPcmFromWav(inputStream: InputStream): Pair<ShortArray, Int> {
        val bytes = inputStream.readBytes()
        if (bytes.size < 44) return Pair(ShortArray(0), 16000)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Read sample rate at byte 24
        buffer.position(24)
        val sampleRate = buffer.getInt()

        // Find "data" chunk
        var dataOffset = 12
        while (dataOffset < bytes.size - 8) {
            val chunkId = String(bytes, dataOffset, 4)
            val chunkSize = ByteBuffer.wrap(bytes, dataOffset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (chunkId == "data") {
                dataOffset += 8
                val availableBytes = minOf(chunkSize, bytes.size - dataOffset)
                val shortCount = availableBytes / 2
                val shortArray = ShortArray(shortCount)
                buffer.position(dataOffset)
                buffer.asShortBuffer().get(shortArray)
                return Pair(shortArray, sampleRate)
            }
            dataOffset += 8 + chunkSize
        }

        // Fallback: assume header is 44 bytes
        val shortCount = (bytes.size - 44) / 2
        val shortArray = ShortArray(shortCount)
        buffer.position(44)
        buffer.asShortBuffer().get(shortArray)
        return Pair(shortArray, sampleRate)
    }
}
