export enum ListenErrorCode {
  UNKNOWN = 'ERR_UNKNOWN',
}

export default interface NativeRNListen {
  startListening: (options: { locale?: string }) => Promise<void>;
  stopListening: () => Promise<void>;
}
