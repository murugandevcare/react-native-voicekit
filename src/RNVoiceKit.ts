import type NativeRNVoiceKit from './types/native';
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import RNVoiceKitError from './utils/VoiceKitError';
import { VoiceKitErrorCode } from './types/native';
import { VoiceKitEvent } from './types';
import type { VoiceKitEventMap } from './types';

const LINKING_ERROR =
  `The package 'react-native-voicekit' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// proxy NativeModules.VoiceKit to catch any errors thrown by the native module and wrap them in a VoiceKitError
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
              if (
                error?.code &&
                Object.values(VoiceKitErrorCode).includes(error.code)
              ) {
                throw new RNVoiceKitError(
                  error?.message || '',
                  error.code as VoiceKitErrorCode
                );
              } else {
                throw new RNVoiceKitError(
                  'Unknown error',
                  VoiceKitErrorCode.UNKNOWN,
                  error
                );
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

const nativeEmitter = new NativeEventEmitter(
  NativeModules.VoiceKitEventEmitter
);

class RNVoiceKit {
  private listeners: Partial<
    Record<
      VoiceKitEvent,
      ((...args: VoiceKitEventMap[VoiceKitEvent]) => void)[]
    >
  > = {};

  constructor() {
    for (const event of Object.values(VoiceKitEvent)) {
      nativeEmitter.addListener(`RNVoiceKit.${event}`, (...args) => {
        if (this.listeners[event]) {
          this.listeners[event]?.forEach((listener) => listener(...args));
        }
      });
    }
  }

  async startListening(options?: { locale?: string }): Promise<void> {
    await nativeInstance.startListening(options ?? {});
  }

  async stopListening(): Promise<void> {
    await nativeInstance.stopListening();
  }

  addListener<T extends VoiceKitEvent>(
    event: T,
    listener: (...args: VoiceKitEventMap[T]) => void
  ) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(listener);
  }

  removeListener<T extends VoiceKitEvent>(
    event: T,
    listener: (...args: VoiceKitEventMap[T]) => void
  ) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(
        (l) => l !== listener
      );
    }
  }
}

export default new RNVoiceKit();
