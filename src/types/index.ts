export enum VoiceKitEvent {
  Result = 'result',
  PartialResult = 'partial-result',
}

export type VoiceKitEventMap = {
  'result': [string];
  'partial-result': [string];
};
