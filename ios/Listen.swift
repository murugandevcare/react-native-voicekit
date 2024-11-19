import AVFoundation
import Foundation
import Speech

@objc(Listen)
class Listen: NSObject, SFSpeechRecognizerDelegate {
  private var speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
  private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
  private var recognitionTask: SFSpeechRecognitionTask?
  private let audioEngine = AVAudioEngine()

  override init() {
    super.init()
    speechRecognizer?.delegate = self
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
            reject("ERROR", "Failed to start recording: \(error.localizedDescription)", error)
          }
        case .denied:
          reject("ERROR", "Speech recognition permission denied", nil)
        case .restricted:
          reject("ERROR", "Speech recognition restricted on this device", nil)
        case .notDetermined:
          reject("ERROR", "Speech recognition not yet authorized", nil)
        @unknown default:
          reject("ERROR", "Unknown authorization status", nil)
        }
      }
    }
  }

  private func startRecording(options: [String: Any], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) throws {
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
        // Emit partial results through event emitter
        ListenEventEmitter.shared.sendEvent(
          withName: "RNListen.partial-result",
          body: result.bestTranscription.formattedString
        )
      }
      if error != nil {
        self.audioEngine.stop()
        self.audioEngine.inputNode.removeTap(onBus: 0)
        self.recognitionRequest = nil
        self.recognitionTask = nil
        reject("ERROR", "Recognition failed: \(error!.localizedDescription)", error)
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
    audioEngine.stop()
    audioEngine.inputNode.removeTap(onBus: 0)
    recognitionRequest?.endAudio()
    recognitionRequest = nil
    recognitionTask?.cancel()
    recognitionTask = nil
    resolve(true)
  }
}
