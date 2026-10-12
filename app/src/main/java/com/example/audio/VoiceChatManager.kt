package com.example.audio

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class AudioDeviceType {
    EARPIECE,
    SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH
}

class VoiceChatManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive: StateFlow<Boolean> = _isVoiceActive.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentRmsAmplitude = MutableStateFlow(0f)
    val currentRmsAmplitude: StateFlow<Float> = _currentRmsAmplitude.asStateFlow()

    // Video volume ducking: 1.0f (full volume) down to 0.25f (ducked) when voice is active
    private val _videoVolume = MutableStateFlow(1.0f)
    val videoVolume: StateFlow<Float> = _videoVolume.asStateFlow()

    private val _audioDevice = MutableStateFlow(AudioDeviceType.SPEAKER)
    val audioDevice: StateFlow<AudioDeviceType> = _audioDevice.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordJob: Job? = null
    private var isHostMutedLocked = false

    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) {
                        _audioDevice.value = AudioDeviceType.WIRED_HEADSET
                        audioManager.isSpeakerphoneOn = false
                    } else if (state == 0) {
                        updateAudioRouting()
                    }
                }
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    val scoState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                    if (scoState == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        _audioDevice.value = AudioDeviceType.BLUETOOTH
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }
        context.registerReceiver(headsetReceiver, filter)
        updateAudioRouting()
    }

    private fun updateAudioRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var hasBt = false
            var hasHeadset = false
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> hasBt = true
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> hasHeadset = true
                }
            }
            _audioDevice.value = when {
                hasBt -> AudioDeviceType.BLUETOOTH
                hasHeadset -> AudioDeviceType.WIRED_HEADSET
                else -> AudioDeviceType.SPEAKER
            }
        } else {
            _audioDevice.value = if (audioManager.isWiredHeadsetOn) AudioDeviceType.WIRED_HEADSET else AudioDeviceType.SPEAKER
        }
    }

    fun startVoiceChat() {
        if (_isVoiceActive.value) return

        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = (_audioDevice.value == AudioDeviceType.SPEAKER)
            if (_audioDevice.value == AudioDeviceType.BLUETOOTH) {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }

            _isVoiceActive.value = true
            // Automatic volume ducking: reduce video volume to 25%
            _videoVolume.value = 0.25f

            initAudioTrack()
            startRecordingLoop()
        } catch (e: Exception) {
            Log.e("VoiceChatManager", "Failed to start voice chat", e)
        }
    }

    fun stopVoiceChat() {
        if (!_isVoiceActive.value) return

        _isVoiceActive.value = false
        _isSpeaking.value = false
        // Restore full video volume without restarting video
        _videoVolume.value = 1.0f

        stopRecordingLoop()
        releaseAudioTrack()

        try {
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e("VoiceChatManager", "Failed to reset audio mode", e)
        }
    }

    fun setMicMuted(muted: Boolean) {
        if (isHostMutedLocked && !muted) {
            // Cannot unmute if host locked participant
            return
        }
        _isMicMuted.value = muted
        if (muted) {
            _isSpeaking.value = false
        }
    }

    fun onHostMuteEnforced(isMutedByHost: Boolean) {
        isHostMutedLocked = isMutedByHost
        if (isMutedByHost) {
            _isMicMuted.value = true
            _isSpeaking.value = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecordingLoop() {
        recordJob?.cancel()
        recordJob = scope.launch(Dispatchers.IO) {
            try {
                val minBuf = bufferSize.coerceAtLeast(1024)
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    minBuf * 2
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("VoiceChatManager", "AudioRecord not initialized")
                    return@launch
                }

                audioRecord?.startRecording()
                val audioBuffer = ShortArray(minBuf / 2)

                while (isActive && _isVoiceActive.value) {
                    val readSamples = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readSamples > 0) {
                        if (!_isMicMuted.value && !isHostMutedLocked) {
                            // Calculate RMS Amplitude
                            var sum = 0.0
                            for (i in 0 until readSamples) {
                                val s = audioBuffer[i].toDouble()
                                sum += s * s
                            }
                            val rms = sqrt(sum / readSamples).toFloat()
                            _currentRmsAmplitude.value = rms

                            // Speaking threshold (~550 amplitude in 16-bit PCM)
                            val speaking = rms > 550f
                            if (_isSpeaking.value != speaking) {
                                _isSpeaking.value = speaking
                            }
                        } else {
                            _currentRmsAmplitude.value = 0f
                            _isSpeaking.value = false
                        }
                    }
                    delay(30)
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException && e !is java.util.concurrent.CancellationException) {
                    Log.e("VoiceChatManager", "Error in recording loop", e)
                }
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
                audioRecord = null
                _isSpeaking.value = false
            }
        }
    }

    private fun stopRecordingLoop() {
        recordJob?.cancel()
        recordJob = null
    }

    private fun initAudioTrack() {
        try {
            val minTrackBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfigOut)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(minTrackBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("VoiceChatManager", "Failed to init AudioTrack", e)
        }
    }

    private fun releaseAudioTrack() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun playAudioChunk(data: ByteArray) {
        if (!_isVoiceActive.value) return
        try {
            audioTrack?.write(data, 0, data.size)
        } catch (e: Exception) {
            Log.e("VoiceChatManager", "Error writing audio chunk", e)
        }
    }

    fun cleanup() {
        stopVoiceChat()
        try {
            context.unregisterReceiver(headsetReceiver)
        } catch (_: Exception) {}
    }
}
