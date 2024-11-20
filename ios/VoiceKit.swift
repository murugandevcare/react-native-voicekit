import AVFoundation
import Foundation
import Speech

@objc(VoiceKit)
class VoiceKit: NSObject, SFSpeechRecognizerDelegate {
  private var speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
  private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
  private var recognitionTask: SFSpeechRecognitionTask?
  private let audioEngine = AVAudioEngine()
  private var lastResultTimer: Timer?
  private var lastTranscription: String?

  override init() {
    super.init()
    speechRecognizer?.delegate = self
  }

  func speechRecognizer(_: SFSpeechRecognizer, availabilityDidChange available: Bool) {
    VoiceKitEventEmitter.shared.sendEvent(
      withName: "RNVoiceKit.availability-change",
      body: available
    )
  }

  @objc(startListening:withResolver:withRejecter:)
  func startListening(options: [String: Any], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // Request authorization
    SFSpeechRecognizer.requestAuthorization { authStatus in
      OperationQueue.main.addOperation {
        switch authStatus {
        case .authorized:
          do {
            try self.startRecording(options: options, resolve: resolve, reject: reject)
          } catch {
            reject(VoiceError.recordingStartFailed.code, VoiceError.recordingStartFailed.message, nil)
          }
        case .denied:
          reject(VoiceError.permissionDenied.code, VoiceError.permissionDenied.message, nil)
        case .restricted:
          reject(VoiceError.permissionRestricted.code, VoiceError.permissionRestricted.message, nil)
        case .notDetermined:
          reject(VoiceError.permissionNotDetermined.code, VoiceError.permissionNotDetermined.message, nil)
        @unknown default:
          reject(VoiceError.unknown(message: "Unknown authorization status").code, VoiceError.unknown(message: "Unknown authorization status").message, nil)
        }
      }
    }
  }

  private func startRecording(options: [String: Any], resolve: @escaping RCTPromiseResolveBlock, reject _: @escaping RCTPromiseRejectBlock) throws {
    Logger.log(level: .info, message: "Starting recording")
    Logger.log(level: .debug, message: "Options: \(options)")

    // Cancel any ongoing tasks
    recognitionTask?.cancel()
    recognitionTask = nil

    // Configure speech recognizer
    let locale = options["locale"] as? String
    speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: locale ?? "en-US"))
    speechRecognizer?.delegate = self

    // Configure audio session
    let audioSession = AVAudioSession.sharedInstance()
    try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
    try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

    // Create and configure recognition request
    recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
    recognitionRequest?.shouldReportPartialResults = true

    // Start recognition task
    recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest!) { result, error in
      if let result {
        Logger.log(level: .debug, message: "SpeechRecognizerResult received: \(result)")

        // Store the latest transcription
        self.lastTranscription = result.bestTranscription.formattedString

        // Emit partial results through event emitter
        VoiceKitEventEmitter.shared.sendEvent(
          withName: "RNVoiceKit.partial-result",
          body: self.lastTranscription
        )

        // Reset the timer
        self.lastResultTimer?.invalidate()
        self.lastResultTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: false) { _ in
          Logger.log(level: .debug, message: "Final result timer fired")
          if let finalTranscription = self.lastTranscription {
            VoiceKitEventEmitter.shared.sendEvent(
              withName: "RNVoiceKit.result",
              body: finalTranscription
            )
          }
        }
      }

      if error != nil {
        self.lastResultTimer?.invalidate()
        self.lastResultTimer = nil
        self.lastTranscription = nil
        self.audioEngine.stop()
        self.audioEngine.inputNode.removeTap(onBus: 0)
        self.recognitionRequest = nil
        self.recognitionTask = nil
        Logger.log(level: .error, message: "Error: \(error)")
        if let error = error as NSError?, error.domain == "kAFAssistantErrorDomain" && error.code == 1110 {
          // No speech detected - ignore
          Logger.log(level: .debug, message: "No speech detected")
          return
        }

        // Send error event
        VoiceKitEventEmitter.shared.sendEvent(
          withName: "RNVoiceKit.error",
          body: VoiceError.unknown(message: error?.localizedDescription ?? "An unknown error occurred").message
        )
      }
    }

    // Configure audio engine
    let inputNode = audioEngine.inputNode
    let recordingFormat = inputNode.outputFormat(forBus: 0)
    inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
      self.recognitionRequest?.append(buffer)
    }

    audioEngine.prepare()
    try audioEngine.start()
    resolve(true)
  }

  @objc(stopListening:withRejecter:)
  func stopListening(resolve: @escaping RCTPromiseResolveBlock, reject _: @escaping RCTPromiseRejectBlock) {
    Logger.log(level: .info, message: "Stopping recording")
    audioEngine.stop()
    audioEngine.inputNode.removeTap(onBus: 0)
    recognitionRequest?.endAudio()
    recognitionRequest = nil
    recognitionTask?.cancel()
    recognitionTask = nil
    resolve(true)
  }

  @objc(isSpeechRecognitionAvailable:withRejecter:)
  func isSpeechRecognitionAvailable(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    withPromise(resolve: resolve, reject: reject) {
      guard let recognizer = speechRecognizer else {
        return false
      }
      return recognizer.isAvailable
    }
  }
}
