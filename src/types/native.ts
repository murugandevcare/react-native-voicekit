import type { VoiceStartListeningOptions } from '.';

export enum VoiceErrorCode {
  UNKNOWN = 'ERR_UNKNOWN',
}

export default interface NativeRNVoiceKit {
  startListening: (options: VoiceStartListeningOptions) => Promise<void>;
  stopListening: () => Promise<void>;
  isSpeechRecognitionAvailable: () => Promise<boolean>;
}
