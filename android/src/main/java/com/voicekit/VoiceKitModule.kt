package com.voicekit

import android.speech.SpeechRecognizer
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.bridge.UiThreadUtil

class VoiceKitModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val voiceKitService = VoiceKitService(reactContext)

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun startListening(options: ReadableMap, promise: Promise) {
    try {
      UiThreadUtil.runOnUiThread {
        try {
          voiceKitService.startListening(options)
          promise.resolve(true)
        } catch (e: VoiceError) {
          promise.reject(e.code, e.message)
        } catch (e: Exception) {
          Log.e(TAG, "Error starting recording", e)
          promise.reject(VoiceError.RecognitionFailed.code, VoiceError.RecognitionFailed.message)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error starting recording", e)
      promise.reject(VoiceError.RecognitionFailed.code, VoiceError.RecognitionFailed.message)
    }
  }

  @ReactMethod
  fun stopListening(promise: Promise) {
    try {
      UiThreadUtil.runOnUiThread {
        try {
          voiceKitService.stopListening()
          promise.resolve(true)
        } catch (e: Exception) {
          Log.e(TAG, "Error stopping recording", e)
          promise.reject(VoiceError.RecognitionFailed.code, VoiceError.RecognitionFailed.message)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping recording", e)
      promise.reject(VoiceError.RecognitionFailed.code, VoiceError.RecognitionFailed.message)
    }
  }

  @ReactMethod
  fun isSpeechRecognitionAvailable(promise: Promise) {
    try {
      val available = SpeechRecognizer.isRecognitionAvailable(reactApplicationContext)
      promise.resolve(available)
    } catch (e: Exception) {
      Log.w(TAG, "Error checking speech recognition availability, returning false", e)
      promise.resolve(false)
    }
  }

  companion object {
    const val NAME = "VoiceKit"
    private const val TAG = "VoiceKitModule"
  }
}
