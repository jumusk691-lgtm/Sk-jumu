package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

data class AudioScenario(
    val id: String,
    val name: String,
    val noiseTypeDescription: String,
    val initialNoiseLevelDb: Float,
    val durationSeconds: Int = 6
)

/**
 * Generates realistic acoustic audio scenarios containing speech formants mixed with
 * authentic environmental background noises (traffic, HVAC, wind, crowd chatter).
 */
object SyntheticAudioGenerator {

    val SCENARIOS = listOf(
        AudioScenario(
            id = "traffic",
            name = "Street Vlog with Traffic",
            noiseTypeDescription = "Heavy engine rumble (60-120Hz), tire friction, and distant car horns",
            initialNoiseLevelDb = -18f
        ),
        AudioScenario(
            id = "hvac_fan",
            name = "Studio Fan & Air Conditioner",
            noiseTypeDescription = "Continuous 50Hz/60Hz electrical hum with broad-spectrum air hiss",
            initialNoiseLevelDb = -22f
        ),
        AudioScenario(
            id = "wind_outdoor",
            name = "Outdoor Windy Video Interview",
            noiseTypeDescription = "Turbulent low-frequency wind gusts and atmospheric rumble",
            initialNoiseLevelDb = -15f
        ),
        AudioScenario(
            id = "subway",
            name = "Subway Train Commute",
            noiseTypeDescription = "Rumbling train tracks, metallic friction, and station echo",
            initialNoiseLevelDb = -16f
        )
    )

    /**
     * Synthesize 16-bit PCM audio samples containing realistic spoken speech pulses
     * layered over authentic background noise according to the chosen scenario.
     */
    fun generateScenarioPcm(
        scenarioId: String,
        sampleRate: Int = 16000,
        durationSec: Float = 6.0f
    ): Pair<ShortArray, ShortArray> {
        val totalSamples = (sampleRate * durationSec).toInt()
        val noisyMix = ShortArray(totalSamples)
        val pureVoiceOnly = ShortArray(totalSamples)

        val random = Random(42)

        // Generate synthetic voice speaking phonetic syllables with natural pitch modulation
        // Formants: F0 ~ 130 Hz (fundamental pitch), F1 ~ 500 Hz, F2 ~ 1500 Hz, F3 ~ 2500 Hz
        val speechSyllables = listOf(
            Pair(0.4f, 0.9f),   // "Ze-"
            Pair(1.0f, 1.6f),   // "-ro"
            Pair(1.9f, 2.7f),   // "Noise"
            Pair(3.0f, 3.8f),   // "Voice"
            Pair(4.0f, 5.2f)    // "Cleaned"
        )

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate

            // Check if within any speech window
            var voiceSample = 0.0f
            for (window in speechSyllables) {
                if (t >= window.first && t <= window.second) {
                    val progress = (t - window.first) / (window.second - window.first)
                    // Bell-shaped amplitude envelope
                    val envelope = sin(progress * PI.toFloat())

                    // Glottal pulse fundamental with vibrato
                    val pitch = 135.0f + 12.0f * sin(t * 8.0f)
                    val fundamental = sin(2.0 * PI * pitch * t).toFloat()
                    val f1 = 0.6f * sin(2.0 * PI * 520.0 * t).toFloat()
                    val f2 = 0.4f * sin(2.0 * PI * 1480.0 * t).toFloat()
                    val f3 = 0.25f * sin(2.0 * PI * 2600.0 * t).toFloat()

                    voiceSample = (fundamental * 0.5f + f1 + f2 + f3) * envelope * 0.75f
                    break
                }
            }

            // Generate specific scenario background noise
            var noiseSample = 0.0f
            when (scenarioId) {
                "traffic" -> {
                    // Low rumble 60-120Hz + periodic horn + tire noise
                    val engineRumble = sin(2.0 * PI * 65.0 * t).toFloat() * 0.35f +
                            sin(2.0 * PI * 130.0 * t).toFloat() * 0.2f
                    val tireFriction = (random.nextFloat() * 2f - 1f) * 0.15f
                    val horn = if (t in 2.2f..2.7f) sin(2.0 * PI * 440.0 * t).toFloat() * 0.4f else 0f
                    noiseSample = engineRumble + tireFriction + horn
                }
                "hvac_fan" -> {
                    // 60Hz power hum + harmonics + broad air rush
                    val hum60 = sin(2.0 * PI * 60.0 * t).toFloat() * 0.35f
                    val hum120 = sin(2.0 * PI * 120.0 * t).toFloat() * 0.18f
                    val airRush = (random.nextFloat() * 2f - 1f) * 0.25f
                    noiseSample = hum60 + hum120 + airRush
                }
                "wind_outdoor" -> {
                    // Modulated low frequency gusts
                    val gustEnvelope = (0.5f + 0.5f * sin(t * 1.5f))
                    val lowGust = sin(2.0 * PI * 45.0 * t).toFloat() * 0.5f * gustEnvelope
                    val micPopping = if (random.nextFloat() < 0.03f) (random.nextFloat() * 2f - 1f) * 0.6f else 0f
                    noiseSample = lowGust + micPopping
                }
                "subway" -> {
                    // Track clatter + metallic resonance
                    val clatter = sin(2.0 * PI * 85.0 * t).toFloat() * 0.3f
                    val railScreech = if (t in 1.2f..2.5f) sin(2.0 * PI * 1850.0 * t).toFloat() * 0.25f else 0f
                    val rumble = (random.nextFloat() * 2f - 1f) * 0.2f
                    noiseSample = clatter + railScreech + rumble
                }
                else -> {
                    // Default ambient white/pink noise
                    noiseSample = (random.nextFloat() * 2f - 1f) * 0.3f
                }
            }

            val mixed = (voiceSample + noiseSample).coerceIn(-1.0f, 1.0f)
            noisyMix[i] = (mixed * 32767.0f).toInt().toShort()
            pureVoiceOnly[i] = (voiceSample.coerceIn(-1.0f, 1.0f) * 32767.0f).toInt().toShort()
        }

        return Pair(noisyMix, pureVoiceOnly)
    }
}
