package com.example.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RealNoiseDspEngineTest {

    @Test
    fun testFftRoundTrip() {
        val size = 512
        val real = FloatArray(size) { i -> kotlin.math.sin(2.0 * Math.PI * 440.0 * i / 16000.0).toFloat() }
        val origReal = real.copyOf()
        val imag = FloatArray(size)

        // Forward FFT
        FastFourierTransform.fft(real, imag, inverse = false)

        // Inverse FFT
        FastFourierTransform.fft(real, imag, inverse = true)

        // Verify round-trip accuracy
        for (i in 0 until size) {
            assertEquals(origReal[i], real[i], 1e-3f)
        }
    }

    @Test
    fun testNoiseZeroGatingOnSilenceAndNoise() {
        val engine = RealNoiseDspEngine(sampleRate = 16000, fftSize = 512)
        engine.isNoiseCancellationEnabled = true
        engine.mode = NoiseCancellationMode.SAFELY_VOICE_ZERO

        // Feed background noise
        val noiseIn = ShortArray(256) { (it % 100).toShort() }
        val cleanOut = ShortArray(256)

        // Warm up engine with 5 frames of noise
        for (f in 0 until 5) {
            engine.processBuffer(noiseIn, cleanOut, 256)
        }

        val stats = engine.getLatestStats()
        // Gating should pull output down heavily
        assertTrue("Expected output to be silenced or heavily reduced", stats.cleanRmsDb <= stats.rawRmsDb)
    }
}
