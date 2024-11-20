export enum VoiceErrorCode {
  UNKNOWN = 'ERR_UNKNOWN',
}

export default interface NativeRNVoiceKit {
  startListening: (options: { locale?: string; mode?: 'continuous' | 'single' }) => Promise<void>;
  stopListening: () => Promise<void>;
  isSpeechRecognitionAvailable: () => Promise<boolean>;
}
