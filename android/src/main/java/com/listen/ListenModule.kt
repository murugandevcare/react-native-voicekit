package com.listen

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

class ListenModule(reactContext: ReactApplicationContext) :
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
      promise.reject("ERROR", "Activity is null")
      return
    }

    // Check for permission
    // TODO: Currently we don't wait for the pop-up permission dialog to be closed, but we should
    if (ContextCompat.checkSelfPermission(currentActivity, Manifest.permission.RECORD_AUDIO)
      != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
        currentActivity,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        PERMISSION_REQUEST_CODE
      )
      promise.reject("ERROR", "Speech recognition permission denied")
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
              Log.d("ListenModule", "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
              Log.d("ListenModule", "onEndOfSpeech")
            }

            override fun onError(error: Int) {
              Log.d("ListenModule", "onError: $error")
              val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error"
              }
              promise.reject("ERROR", errorMessage)
            }

            override fun onResults(results: Bundle?) {
              val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              if (matches != null && matches.isNotEmpty()) {
                // Send final results
                sendEvent("RNListen.result", matches[0])
              }
            }

            override fun onPartialResults(partialResults: Bundle?) {
              val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              if (matches != null && matches.isNotEmpty()) {
                // Send partial results
                sendEvent("RNListen.partial-result", matches[0])
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
          promise.reject("ERROR", "Failed to start listening: ${e.message}")
        }
      }
    } catch (e: Exception) {
      promise.reject("ERROR", "Failed to start listening: ${e.message}")
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
          promise.reject("ERROR", "Failed to stop listening: ${e.message}")
        }
      }
    } catch (e: Exception) {
      promise.reject("ERROR", "Failed to stop listening: ${e.message}")
    }
  }

  companion object {
    const val NAME = "Listen"
  }
}
