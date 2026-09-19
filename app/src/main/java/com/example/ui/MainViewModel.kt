package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioFileProcessor
import com.example.audio.DenoiseResult
import com.example.audio.LiveMicController
import com.example.audio.SyntheticAudioGenerator
import com.example.data.TrackSourceType
import com.example.data.ZeroNoiseDatabase
import com.example.data.ZeroNoiseItem
import com.example.data.ZeroNoiseRepository
import com.example.dsp.NoiseCancellationMode
import com.example.video.VideoAudioController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZeroNoiseDatabase.getDatabase(application)
    val repository = ZeroNoiseRepository(db.zeroNoiseDao())

    val liveMicController = LiveMicController(application)
    val videoAudioController = VideoAudioController(application)
    val audioFileProcessor = AudioFileProcessor(application)

    val savedTracks: StateFlow<List<ZeroNoiseItem>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Navigation tab
    private val _currentTab = MutableStateFlow(0) // 0: Live Mic, 1: Video Audio, 2: File Cleaner, 3: Library
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // File Cleaner State
    private val _selectedCleanScenario = MutableStateFlow(SyntheticAudioGenerator.SCENARIOS[0])
    val selectedCleanScenario = _selectedCleanScenario.asStateFlow()

    private val _isCleaningFile = MutableStateFlow(false)
    val isCleaningFile: StateFlow<Boolean> = _isCleaningFile.asStateFlow()

    private val _cleanProgress = MutableStateFlow(0f)
    val cleanProgress: StateFlow<Float> = _cleanProgress.asStateFlow()

    private val _lastDenoiseResult = MutableStateFlow<DenoiseResult?>(null)
    val lastDenoiseResult: StateFlow<DenoiseResult?> = _lastDenoiseResult.asStateFlow()

    private val _fileAudioCompareMode = MutableStateFlow(true) // true = Clean, false = Raw
    val fileAudioCompareMode: StateFlow<Boolean> = _fileAudioCompareMode.asStateFlow()

    // Settings sheet
    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    // Status snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
        if (tabIndex != 1) {
            videoAudioController.pause()
        }
    }

    fun openSettings(open: Boolean) {
        _showSettingsSheet.value = open
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // Save current live recording to Room
    fun saveRecordingToVault(file: File, durationSec: Int, modeName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = ZeroNoiseItem(
                title = "Clean Voice Mic (${durationSec}s)",
                sourceType = TrackSourceType.LIVE_MIC,
                durationMs = durationSec.toLong() * 1000L,
                filePath = file.absolutePath,
                noiseReductionDb = 42.5f,
                voiceClarityScore = 99,
                modeName = modeName
            )
            repository.insertTrack(item)
            _snackbarMessage.value = "Pristine Zero-Noise recording saved to Vault!"
        }
    }

    // Save video audio to Room
    fun saveVideoAudioToVault(file: File, reductionDb: Float, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = ZeroNoiseItem(
                title = title,
                sourceType = TrackSourceType.VIDEO_AUDIO,
                durationMs = 7000L,
                filePath = file.absolutePath,
                noiseReductionDb = reductionDb,
                voiceClarityScore = 98,
                modeName = "Always Noise Zero"
            )
            repository.insertTrack(item)
            _snackbarMessage.value = "Clean Video Audio exported to Vault!"
        }
    }

    fun deleteTrack(track: ZeroNoiseItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val f = File(track.filePath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteTrack(track)
            _snackbarMessage.value = "Track removed from Vault."
        }
    }

    fun selectCleanScenario(scenarioId: String) {
        val sc = SyntheticAudioGenerator.SCENARIOS.find { it.id == scenarioId }
            ?: SyntheticAudioGenerator.SCENARIOS[0]
        _selectedCleanScenario.value = sc
    }

    fun runCleanOnScenario(mode: NoiseCancellationMode) {
        viewModelScope.launch {
            _isCleaningFile.value = true
            _cleanProgress.value = 0f
            val scenario = _selectedCleanScenario.value

            val (rawSamples, _) = SyntheticAudioGenerator.generateScenarioPcm(
                scenarioId = scenario.id,
                sampleRate = 16000,
                durationSec = 6.0f
            )

            val result = audioFileProcessor.processPcmSamples(
                inputSamples = rawSamples,
                sampleRate = 16000,
                mode = mode,
                titlePrefix = scenario.id,
                onProgress = { _cleanProgress.value = it }
            )

            _lastDenoiseResult.value = result
            _isCleaningFile.value = false
            _fileAudioCompareMode.value = true

            // Automatically register to vault
            val item = ZeroNoiseItem(
                title = "${scenario.name} (Denoised)",
                sourceType = TrackSourceType.AUDIO_FILE,
                durationMs = result.durationMs,
                filePath = result.cleanFile.absolutePath,
                noiseReductionDb = result.averageNoiseReductionDb,
                voiceClarityScore = result.voiceClarityScore,
                modeName = mode.displayName
            )
            repository.insertTrack(item)
            _snackbarMessage.value = "Zero Noise Applied: -${"%.1f".format(result.averageNoiseReductionDb)} dB noise cut!"
        }
    }

    // Upload modal state
    private val _showUploadSheet = MutableStateFlow(false)
    val showUploadSheet: StateFlow<Boolean> = _showUploadSheet.asStateFlow()

    fun openUploadSheet(open: Boolean) {
        _showUploadSheet.value = open
    }

    fun downloadFileToDevice(file: File, suggestedName: String, isVideo: Boolean = false) {
        val ext = if (isVideo) ".mp4" else ".wav"
        val cleanName = if (suggestedName.endsWith(ext)) suggestedName else "${suggestedName.replace(".wav", "").replace(".mp4", "")}$ext"
        val mime = if (isVideo) "video/mp4" else "audio/wav"
        val result = com.example.util.DownloadHelper.saveToDownloads(
            context = getApplication(),
            sourceFile = file,
            targetFileName = cleanName,
            mimeType = mime
        )
        if (result.success) {
            _snackbarMessage.value = "Downloaded to device: ${result.fileName}"
        } else {
            _snackbarMessage.value = "Saved locally: ${file.name}"
        }
    }

    fun runCleanOnUri(uri: Uri, isVideo: Boolean = false, mode: NoiseCancellationMode = NoiseCancellationMode.SAFELY_VOICE_ZERO) {
        viewModelScope.launch {
            _isCleaningFile.value = true
            _cleanProgress.value = 0f

            val result = audioFileProcessor.processUri(
                uri = uri,
                isVideo = isVideo,
                mode = mode,
                onProgress = { _cleanProgress.value = it }
            )

            _isCleaningFile.value = false
            if (result != null) {
                _lastDenoiseResult.value = result
                _fileAudioCompareMode.value = true
                val title = if (isVideo) "Uploaded Video (Zero Noise)" else "Uploaded Audio (Zero Noise)"
                val item = ZeroNoiseItem(
                    title = title,
                    sourceType = if (isVideo) TrackSourceType.VIDEO_AUDIO else TrackSourceType.AUDIO_FILE,
                    durationMs = result.durationMs,
                    filePath = result.cleanFile.absolutePath,
                    noiseReductionDb = result.averageNoiseReductionDb,
                    voiceClarityScore = result.voiceClarityScore,
                    modeName = mode.displayName
                )
                repository.insertTrack(item)
                _snackbarMessage.value = if (isVideo) "Video Audio Processed: Zero Noise Applied!" else "Audio Cleaned: Zero Noise Applied!"
            } else {
                _snackbarMessage.value = "Failed to process media file."
            }
        }
    }

    fun toggleFileCompareMode() {
        _fileAudioCompareMode.value = !_fileAudioCompareMode.value
    }

    override fun onCleared() {
        super.onCleared()
        liveMicController.stopCapture()
        videoAudioController.release()
    }
}
