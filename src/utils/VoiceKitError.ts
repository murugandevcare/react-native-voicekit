import type { VoiceKitErrorCode } from '../types/native';

class VoiceKitError extends Error {
  code: VoiceKitErrorCode;
  details?: any;

  constructor(message: string, code: VoiceKitErrorCode, details?: any) {
    super(message);
    this.code = code;
    this.details = details;
  }
}

export default VoiceKitError;
