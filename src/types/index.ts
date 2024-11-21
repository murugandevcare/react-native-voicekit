export enum VoiceEvent {
  Result = 'result',
  PartialResult = 'partial-result',
  AvailabilityChange = 'availability-change',
  ListeningStateChange = 'listening-state-change',
  Error = 'error',
}

export interface VoiceEventMap extends Record<VoiceEvent, any[]> {
  [VoiceEvent.Result]: [string];
  [VoiceEvent.PartialResult]: [string];
  [VoiceEvent.AvailabilityChange]: [boolean];
  [VoiceEvent.ListeningStateChange]: [boolean];
  [VoiceEvent.Error]: any[];
}

export enum VoiceMode {
  Continuous = 'continuous',
  Single = 'single',
}

export interface VoiceStartListeningOptions {
  /**
   * The locale to use for speech recognition. Defaults to `en-US`.
   */
  locale?: string;
  /**
   * The mode to use for speech recognition. Can either be `continuous` or `single`. When set to `continuous`, the
   * speech recognizer will continue to listen until stopped manually or an error occurs and will emit regular
   * `partial-result` events. When the user stops speaking, it will also emit a `result` event. When set to `single`,
   * the recognizer will automatically stop after the first utterance and emit a `result` event. Defaults to `single`.
   */
  mode?: VoiceMode;
  /**
   * Whether to try and mute the beep sound Android makes when starting and stopping the speech recognizer. This will
   * mute the device's music audio stream when starting to listen and unmute it when stopping. This does not work on
   * all devices. Defaults to `false`.
   */
  muteAndroidBeep?: boolean;
}
