package com.example.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance Radix-2 Cooley-Tukey Fast Fourier Transform (FFT)
 * and Inverse Fast Fourier Transform (IFFT) implementation for real-time audio DSP.
 */
object FastFourierTransform {

    /**
     * Compute FFT in-place for power-of-2 length arrays of real and imaginary parts.
     */
    fun fft(real: FloatArray, imag: FloatArray, inverse: Boolean = false) {
        val n = real.size
        require(n and (n - 1) == 0) { "Size must be a power of 2, was $n" }

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey butterflies
        var length = 2
        while (length <= n) {
            val halfLength = length shr 1
            val angle = (if (inverse) 2.0 * PI else -2.0 * PI) / length
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until halfLength) {
                    val uR = real[i + k]
                    val uI = imag[i + k]

                    val vIdx = i + k + halfLength
                    val vR = real[vIdx] * wR - imag[vIdx] * wI
                    val vI = real[vIdx] * wI + imag[vIdx] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI

                    real[vIdx] = uR - vR
                    imag[vIdx] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += length
            }
            length = length shl 1
        }

        // Scale on inverse transform
        if (inverse) {
            val invN = 1.0f / n
            for (k in 0 until n) {
                real[k] *= invN
                imag[k] *= invN
            }
        }
    }

    /**
     * Compute magnitudes sqrt(real^2 + imag^2)
     */
    fun computeMagnitude(real: FloatArray, imag: FloatArray, outputMag: FloatArray) {
        val count = outputMag.size
        for (i in 0 until count) {
            outputMag[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
    }

    /**
     * Generate Hann (Hanning) window
     */
    fun generateHannWindow(size: Int): FloatArray {
        val window = FloatArray(size)
        val factor = 2.0 * PI / (size - 1)
        for (i in 0 until size) {
            window[i] = (0.5 * (1.0 - cos(i * factor))).toFloat()
        }
        return window
    }
}
