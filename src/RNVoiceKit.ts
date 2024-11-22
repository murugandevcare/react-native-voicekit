import type NativeRNVoiceKit from './types/native';
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import RNVoiceError from './utils/VoiceError';
import { VoiceErrorCode } from './types/native';
import { VoiceEvent, VoiceMode } from './types';
import type { VoiceEventMap, VoiceStartListeningOptions } from './types';

const LINKING_ERROR =
  `The package 'react-native-voicekit' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// proxy NativeModules.VoiceKit to catch any errors thrown by the native module and wrap them in a VoiceError
const nativeInstance: NativeRNVoiceKit = NativeModules.VoiceKit
  ? new Proxy(NativeModules.VoiceKit, {
      get(target: NativeRNVoiceKit, prop: keyof NativeRNVoiceKit) {
        const originalFunction = target[prop];
        if (typeof originalFunction === 'function') {
          return async (...args: any[]) => {
            try {
              // @ts-expect-error - we can't know the types of the functions and their arguments
              return await originalFunction(...args);
            } catch (error: any) {
              if (error?.code && Object.values(VoiceErrorCode).includes(error.code)) {
                throw new RNVoiceError(error?.message || '', error.code as VoiceErrorCode);
              } else {
                throw new RNVoiceError('Unknown error', VoiceErrorCode.UNKNOWN, error);
              }
            }
          };
        }
        return originalFunction;
      },
    })
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const nativeEmitter = new NativeEventEmitter(NativeModules.VoiceKitEventEmitter);

class RNVoiceKit {
  private listeners: Partial<Record<VoiceEvent, ((...args: VoiceEventMap[VoiceEvent]) => void)[]>> = {};

  constructor() {
    for (const event of Object.values(VoiceEvent)) {
      nativeEmitter.addListener(`RNVoiceKit.${event}`, (...args) => {
        if (this.listeners[event]) {
          this.listeners[event]?.forEach((listener) => listener(...args));
        }
      });
    }
  }

  async startListening(options?: VoiceStartListeningOptions): Promise<void> {
    await nativeInstance.startListening({
      locale: options?.locale ?? 'en-US',
      mode: options?.mode ?? VoiceMode.Continuous,
      silenceTimeoutMs: options?.silenceTimeoutMs ?? 1000,
      muteAndroidBeep: options?.muteAndroidBeep ?? false,
    });
  }

  async stopListening(): Promise<void> {
    await nativeInstance.stopListening();
  }

  async isAvailable(): Promise<boolean> {
    return await nativeInstance.isSpeechRecognitionAvailable();
  }

  addListener<T extends VoiceEvent>(event: T, listener: (...args: VoiceEventMap[T]) => void) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(listener as (...args: any[]) => void);
  }

  removeListener<T extends VoiceEvent>(event: T, listener: (...args: VoiceEventMap[T]) => void) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter((l) => l !== listener);
    }
  }
}

export default new RNVoiceKit();
