export enum VoiceKitErrorCode {
  UNKNOWN = 'ERR_UNKNOWN',
}

export default interface NativeRNVoiceKit {
  startListening: (options: { locale?: string }) => Promise<void>;
  stopListening: () => Promise<void>;
}
