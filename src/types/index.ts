export enum VoiceEvent {
  Result = 'result',
  PartialResult = 'partial-result',
  AvailabilityChange = 'availability-change',
  Error = 'error',
}

export interface VoiceEventMap extends Record<VoiceEvent, any[]> {
  [VoiceEvent.Result]: [string];
  [VoiceEvent.PartialResult]: [string];
  [VoiceEvent.AvailabilityChange]: [boolean];
  [VoiceEvent.Error]: any[];
}

export enum VoiceMode {
  Continuous = 'continuous',
  Single = 'single',
}
