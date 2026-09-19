package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import com.example.dsp.DspStats
import com.example.dsp.NoiseCancellationMode
import com.example.dsp.RealNoiseDspEngine
import com.example.dsp.WavHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class LiveMicController(
    private val context: Context,
    val sampleRate: Int = 16000
) {
    private val TAG = "LiveMicController"

    val dspEngine = RealNoiseDspEngine(sampleRate = sampleRate, fftSize = 512)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _hardwareSuppressorActive = MutableStateFlow(false)
    val hardwareSuppressorActive: StateFlow<Boolean> = _hardwareSuppressorActive.asStateFlow()

    private val _dspStats = MutableStateFlow(DspStats())
    val dspStats: StateFlow<DspStats> = _dspStats.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    private var processingJob: Job? = null
    private var timerJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // WAV Recording state
    private var currentRecordingFile: File? = null
    private var currentRecordingOut: FileOutputStream? = null
    private var recordedBytesCount = 0
    private var recordingStartTime = 0L

    // Fallback simulation when physical mic is not providing data or permission pending
    private val useSyntheticFallback = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun startCapture() {
        if (_isProcessing.value) return

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(1024)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0 && NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                    enabled = true
                }
                _hardwareSuppressorActive.value = noiseSuppressor?.enabled == true
            }
            if (sessionId != 0 && AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                    enabled = true
                }
            }
            if (sessionId != 0 && AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(sessionId)?.apply {
                    enabled = true
                }
            }

            audioRecord?.startRecording()
            _isProcessing.value = true
            useSyntheticFallback.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord init error, falling back to simulated microphone stream", e)
            _isProcessing.value = true
            useSyntheticFallback.set(true)
        }

        startAudioLoop(bufferSize)
    }

    private fun startAudioLoop(bufferSize: Int) {
        processingJob?.cancel()
        processingJob = coroutineScope.launch {
            val inBuffer = ShortArray(256)
            val outBuffer = ShortArray(256)
            var statsUpdateThrottle = 0
            var synthPhase = 0f

            while (isActive && _isProcessing.value) {
                val readCount: Int
                if (!useSyntheticFallback.get() && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    readCount = audioRecord?.read(inBuffer, 0, inBuffer.size) ?: -1
                } else {
                    // Generate subtle room ambience and occasional speech pulses for sandbox/emulator preview
                    readCount = inBuffer.size
                    for (i in 0 until inBuffer.size) {
                        synthPhase += 0.05f
                        val ambientHum = (Math.sin(synthPhase.toDouble() * 0.4) * 1200.0).toInt()
                        val noiseHiss = ((Math.random() - 0.5) * 800.0).toInt()
                        inBuffer[i] = (ambientHum + noiseHiss).coerceIn(-32768, 32767).toShort()
                    }
                    kotlinx.coroutines.delay(16) // ~16ms frame timing
                }

                if (readCount > 0) {
                    val stats = dspEngine.processBuffer(inBuffer, outBuffer, readCount)

                    // Write to live WAV recording if active
                    if (_isRecording.value && currentRecordingOut != null) {
                        val byteBuf = ByteArray(readCount * 2)
                        for (i in 0 until readCount) {
                            val v = outBuffer[i].toInt()
                            byteBuf[i * 2] = (v and 0xFF).toByte()
                            byteBuf[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
                        }
                        try {
                            currentRecordingOut?.write(byteBuf)
                            recordedBytesCount += byteBuf.size
                        } catch (e: Exception) {
                            Log.e(TAG, "Recording write error", e)
                        }
                    }

                    // Feed to AudioTrack monitoring if active
                    if (_isMonitoring.value && audioTrack != null) {
                        audioTrack?.write(outBuffer, 0, readCount)
                    }

                    statsUpdateThrottle++
                    if (statsUpdateThrottle % 2 == 0) {
                        _dspStats.value = stats
                    }
                }
            }
        }
    }

    fun stopCapture() {
        _isProcessing.value = false
        stopRecording()
        stopMonitoring()

        processingJob?.cancel()
        processingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null

        noiseSuppressor?.release()
        noiseSuppressor = null
        echoCanceler?.release()
        echoCanceler = null
        gainControl?.release()
        gainControl = null
        _hardwareSuppressorActive.value = false
    }

    fun setNoiseCancellationEnabled(enabled: Boolean) {
        dspEngine.isNoiseCancellationEnabled = enabled
        noiseSuppressor?.enabled = enabled
        _hardwareSuppressorActive.value = noiseSuppressor?.enabled == true
    }

    fun setMode(mode: NoiseCancellationMode) {
        dspEngine.mode = mode
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        if (enabled == _isMonitoring.value) return
        if (enabled) {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack?.play()
                _isMonitoring.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AudioTrack monitor", e)
                _isMonitoring.value = false
            }
        } else {
            stopMonitoring()
        }
    }

    private fun stopMonitoring() {
        _isMonitoring.value = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
        audioTrack = null
    }

    fun startRecording(): File? {
        if (_isRecording.value) return currentRecordingFile

        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "clean_mic_${System.currentTimeMillis()}.wav")
        currentRecordingFile = file
        recordedBytesCount = 0
        recordingStartTime = System.currentTimeMillis()

        try {
            val fos = FileOutputStream(file)
            currentRecordingOut = fos
            // Write placeholder WAV header
            WavHelper.writeWavHeader(fos, sampleRate, 1, 16, 0)
            _isRecording.value = true
            _recordingDurationSec.value = 0

            timerJob?.cancel()
            timerJob = coroutineScope.launch {
                while (isActive && _isRecording.value) {
                    kotlinx.coroutines.delay(1000)
                    _recordingDurationSec.value =
                        ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                }
            }
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create recording file", e)
            return null
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        _isRecording.value = false
        timerJob?.cancel()
        timerJob = null

        val file = currentRecordingFile
        try {
            currentRecordingOut?.flush()
            currentRecordingOut?.close()
            currentRecordingOut = null

            if (file != null && file.exists()) {
                WavHelper.updateWavDataLength(file, recordedBytesCount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing WAV recording", e)
        }
        currentRecordingFile = null
        return file
    }
}
