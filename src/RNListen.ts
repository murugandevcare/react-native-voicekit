import type NativeRNListen from './types/native';
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import RNListenError from './utils/ListenError';
import { ListenErrorCode } from './types/native';
import { ListenEvent } from './types';
import type { ListenEventMap } from './types';

const LINKING_ERROR =
  `The package 'react-native-listen' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// proxy NativeModules.Listen to catch any errors thrown by the native module and wrap them in a ListenError
const nativeInstance: NativeRNListen = NativeModules.Listen
  ? new Proxy(NativeModules.Listen, {
      get(target: NativeRNListen, prop: keyof NativeRNListen) {
        const originalFunction = target[prop];
        if (typeof originalFunction === 'function') {
          return async (...args: any[]) => {
            try {
              // @ts-expect-error - we can't know the types of the functions and their arguments
              return await originalFunction(...args);
            } catch (error: any) {
              if (
                error?.code &&
                Object.values(ListenErrorCode).includes(error.code)
              ) {
                throw new RNListenError(
                  error?.message || '',
                  error.code as ListenErrorCode
                );
              } else {
                throw new RNListenError(
                  'Unknown error',
                  ListenErrorCode.UNKNOWN,
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

const nativeEmitter = new NativeEventEmitter(NativeModules.ListenEventEmitter);

class RNListen {
  private listeners: Partial<
    Record<ListenEvent, ((...args: ListenEventMap[ListenEvent]) => void)[]>
  > = {};

  constructor() {
    for (const event of Object.values(ListenEvent)) {
      nativeEmitter.addListener(`RNListen.${event}`, (...args) => {
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

  addListener<T extends ListenEvent>(
    event: T,
    listener: (...args: ListenEventMap[T]) => void
  ) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(listener);
  }

  removeListener<T extends ListenEvent>(
    event: T,
    listener: (...args: ListenEventMap[T]) => void
  ) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(
        (l) => l !== listener
      );
    }
  }
}

export default new RNListen();
