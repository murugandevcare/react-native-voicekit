package com.voicekit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

class VoiceKitService(private val context: ReactApplicationContext) {
  private var speechRecognizer: SpeechRecognizer? = null
  private var options: ReadableMap = Arguments.createMap()
  private val PERMISSION_REQUEST_CODE = 1000
  private var audioManager: AudioManager? = null
  private var previousMusicVolume: Int = 0
  private var previousNotificationVolume: Int = 0

  private var isListening: Boolean = false
    set(value) {
      if (field != value) {
        field = value
        sendEvent("RNVoiceKit.listening-state-change", value)
      }
    }

  private var lastResultTimer: Handler? = null
  private var lastTranscription: String? = null

  fun sendEvent(eventName: String, params: Any?) {
    context
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  private fun muteRecognizerBeep() {
    if (audioManager == null) {
      audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    previousMusicVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    previousNotificationVolume = audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 0

    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
    audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
  }

  private fun unmuteRecognizerBeep() {
    Log.d(TAG, "Unmuting recognizer beep")
    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, previousMusicVolume, AudioManager.FLAG_ALLOW_RINGER_MODES)
    audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, previousNotificationVolume, AudioManager.FLAG_ALLOW_RINGER_MODES)
  }

  fun startListening(options: ReadableMap, skipMuteBeep: Boolean = false) {
    val currentActivity = context.currentActivity
    if (currentActivity == null) {
      Log.e(TAG, "Activity is null")
      throw VoiceError.Unknown("Activity is null")
    }

    if (isListening) {
      Log.w(TAG, "Already listening, aborting startListening")
      sendEvent("RNVoiceKit.error", VoiceError.InvalidState)
      return
    }

    this.options = options

    // TODO: We currently don't wait for the permission to be granted, but we should
    if (ContextCompat.checkSelfPermission(currentActivity, Manifest.permission.RECORD_AUDIO)
      != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
        currentActivity,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        PERMISSION_REQUEST_CODE
      )
      throw VoiceError.PermissionDenied
    }

    // Initialize speech recognizer if needed
    if (speechRecognizer == null) {
      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    // Set up recognition listener
    speechRecognizer?.setRecognitionListener(createRecognitionListener())

    // Configure recognition intent
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

      if (options.hasKey("locale")) {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, options.getString("locale"))
      }

      if (options.hasKey("mode") && (options.getString("mode") == "continuous" || options.getString("mode") == "continuous-and-stop")) {
        // TODO: On Android 13+, we might be able to use a custom audio source to improve continuous mode
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 600000)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 600000)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600000)
      }
    }

    // Mute beep sound before starting recognition
    if (!skipMuteBeep && options.hasKey("muteAndroidBeep") && options.getBoolean("muteAndroidBeep")) {
      muteRecognizerBeep()
    }

    // Start listening
    speechRecognizer?.startListening(intent)

    isListening = true

    return
  }

  fun stopListening() {
    if (!isListening) {
      Log.w(TAG, "Not listening, aborting stopListening")
      sendEvent("RNVoiceKit.error", VoiceError.InvalidState)
      return
    }

    lastResultTimer?.removeCallbacksAndMessages(null)
    lastResultTimer = null
    lastTranscription = null

    speechRecognizer?.stopListening()
    speechRecognizer?.destroy()
    speechRecognizer = null
    isListening = false

    // Restore audio volume when stopping
    if (options.hasKey("muteAndroidBeep") && options.getBoolean("muteAndroidBeep")) {
      unmuteRecognizerBeep()
    }
  }

  private fun restartRecognizer() {
    speechRecognizer?.stopListening()
    speechRecognizer?.destroy()
    speechRecognizer = null
    startListening(options, skipMuteBeep = true)
  }

  private fun createRecognitionListener(): RecognitionListener {
    return object : RecognitionListener {
      override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "SpeechRecognizer event fired: onReadyForSpeech")
      }

      override fun onBeginningOfSpeech() {
        Log.d(TAG, "SpeechRecognizer event fired: onBeginningOfSpeech")
      }

      override fun onRmsChanged(rmsdB: Float) {}

      override fun onBufferReceived(buffer: ByteArray?) {}

      override fun onEndOfSpeech() {
        Log.d(TAG, "SpeechRecognizer event fired: onEndOfSpeech")
        if (!options.hasKey("mode") || options.getString("mode") == "single") {
          // We're in single mode and the recognizer stopped, clean-up
          stopListening()
        }
      }

      override fun onError(error: Int) {
        Log.d(TAG, "SpeechRecognizer event fired: onError ($error)")
        val voiceError = when (error) {
          SpeechRecognizer.ERROR_AUDIO -> VoiceError.RecordingStartFailed
          SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PermissionDenied
          SpeechRecognizer.ERROR_NETWORK,
          SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceError.Unknown("Network error occurred")
          SpeechRecognizer.ERROR_NO_MATCH -> VoiceError.RecognitionFailed
          SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceError.Unknown("Recognition service busy")
          SpeechRecognizer.ERROR_SERVER -> VoiceError.Unknown("Server error occurred")
          SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceError.Unknown("No speech input")
          else -> VoiceError.Unknown("Unknown error occurred")
        }

        if (error != SpeechRecognizer.ERROR_NO_MATCH) {
          // An error occurred that we can't recover from, send error and notify of listening state change
          sendEvent("RNVoiceKit.error", voiceError)
          isListening = false

          // Restore audio volume when erroring out
          if (options.hasKey("muteAndroidBeep") && options.getBoolean("muteAndroidBeep")) {
            unmuteRecognizerBeep()
          }
        } else {
          // Always restart the recognizer if the recognition stopped due to no speech detected / no match
          restartRecognizer()
        }
      }

      override fun onResults(results: Bundle?) {
        Log.d(TAG, "SpeechRecognizer event fired: onResults")
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        if ((!options.hasKey("mode") || options.getString("mode") == "single") && matches != null && matches.isNotEmpty()) {
          // We're in single mode and have a result, send it
          sendEvent("RNVoiceKit.result", matches[0])
        }

        // Cancel any pending timer
        lastResultTimer?.removeCallbacksAndMessages(null)

        if (options.hasKey("mode") && options.getString("mode") == "continuous") {
          // We're in continuous mode, restart the recognizer
          restartRecognizer()
        }
      }

      override fun onPartialResults(partialResults: Bundle?) {
        Log.d(TAG, "SpeechRecognizer event fired: onPartialResults")
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
          lastTranscription = matches[0]

          // Check if the partial result is empty, if so, don't do anything
          if (lastTranscription?.trim()?.isEmpty() == true) {
            return
          }

          sendEvent("RNVoiceKit.partial-result", lastTranscription)
          Log.d(TAG, "Partial result: $lastTranscription")

          // Reset timer for continuous mode
          if (options.hasKey("mode") && (options.getString("mode") == "continuous" || options.getString("mode") == "continuous-and-stop")) {
            // When we're in continuous mode, we want to send the final result if there's no more speech input for 1s
            lastResultTimer?.removeCallbacksAndMessages(null)
            lastResultTimer = Handler(Looper.getMainLooper()).apply {
              postDelayed({
                lastTranscription?.let { transcription ->
                  sendEvent("RNVoiceKit.result", transcription)
                }

                if (options.getString("mode") == "continuous-and-stop") {
                  stopListening()
                }
              }, 1000)
            }
          }
        }
      }

      override fun onEvent(eventType: Int, params: Bundle?) {}
    }
  }

  companion object {
    private const val TAG = "VoiceKitService"
  }
}
