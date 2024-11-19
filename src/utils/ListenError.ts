import type { ListenErrorCode } from '../types/native';

class ListenError extends Error {
  code: ListenErrorCode;
  details?: any;

  constructor(message: string, code: ListenErrorCode, details?: any) {
    super(message);
    this.code = code;
    this.details = details;
  }
}

export default ListenError;
