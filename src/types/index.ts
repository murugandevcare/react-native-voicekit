export enum ListenEvent {
  Result = 'result',
  PartialResult = 'partial-result',
}

export type ListenEventMap = {
  'result': [string];
  'partial-result': [string];
};
