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
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.UiThreadUtil

class VoiceKitModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private var speechRecognizer: SpeechRecognizer? = null
  private val PERMISSION_REQUEST_CODE = 1000

  override fun getName(): String {
    return NAME
  }

  private fun sendEvent(eventName: String, params: Any?) {
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  @ReactMethod
  fun startListening(options: ReadableMap, promise: Promise) {
    val currentActivity = currentActivity
    if (currentActivity == null) {
      promise.reject(
        VoiceError.Unknown("Activity is null").code,
        VoiceError.Unknown("Activity is null").message
      )
      return
    }

    // Check for permission
    if (ContextCompat.checkSelfPermission(currentActivity, Manifest.permission.RECORD_AUDIO)
      != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
        currentActivity,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        PERMISSION_REQUEST_CODE
      )
      promise.reject(
        VoiceError.PermissionDenied.code,
        VoiceError.PermissionDenied.message
      )
      return
    }

    try {
      // Run on main thread
      UiThreadUtil.runOnUiThread {
        try {
          // Initialize speech recognizer if needed
          if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(reactApplicationContext)
          }

          // Set up recognition listener
          speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {
              Log.d("VoiceKitModule", "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
              Log.d("VoiceKitModule", "onEndOfSpeech")
            }

            override fun onError(error: Int) {
              Log.d("VoiceKitModule", "onError: $error")
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
              promise.reject(voiceError.code, voiceError.message)
            }

            override fun onResults(results: Bundle?) {
              val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              if (matches != null && matches.isNotEmpty()) {
                // Send final results
                sendEvent("RNVoiceKit.result", matches[0])
              }
            }

            override fun onPartialResults(partialResults: Bundle?) {
              val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              if (matches != null && matches.isNotEmpty()) {
                // Send partial results
                sendEvent("RNVoiceKit.partial-result", matches[0])
              }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
          })

          // Configure recognition intent
          val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // Set language if provided in options
            if (options.hasKey("locale")) {
              putExtra(RecognizerIntent.EXTRA_LANGUAGE, options.getString("locale"))
            }
          }

          // Start listening
          speechRecognizer?.startListening(intent)
          promise.resolve(true)
        } catch (e: Exception) {
          promise.reject(VoiceError.RecognitionFailed.code, VoiceError.RecognitionFailed.message)
        }
      }
    } catch (e: Exception) {
      promise.reject(VoiceError.RecognitionFailed.code, VoiceError.RecognitionFailed.message)
    }
  }

  @ReactMethod
  fun stopListening(promise: Promise) {
    try {
      UiThreadUtil.runOnUiThread {
        try {
          speechRecognizer?.stopListening()
          speechRecognizer?.destroy()
          speechRecognizer = null
          promise.resolve(true)
        } catch (e: Exception) {
          promise.reject(VoiceError.RecognitionFailed.code, VoiceError.RecognitionFailed.message)
        }
      }
    } catch (e: Exception) {
      promise.reject(VoiceError.RecognitionFailed.code, VoiceError.RecognitionFailed.message)
    }
  }

  @ReactMethod
  fun isSpeechRecognitionAvailable(promise: Promise) {
    try {
      val available = SpeechRecognizer.isRecognitionAvailable(reactApplicationContext)
      promise.resolve(available)
    } catch (e: Exception) {
      promise.resolve(false)
    }
  }

  companion object {
    const val NAME = "VoiceKit"
  }
}
