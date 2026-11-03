export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ErrorResponse;
  timestamp: string;
}

export interface ErrorDetail {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

export interface ErrorResponse {
  status: number;
  // Open string, not a closed union: the backend can add new ERR_* codes
  // (e.g. ERR_429) without the frontend needing a type update.
  error: string;
  message: string;
  path: string;
  details?: ErrorDetail[];
  timestamp: string;
}

export interface PageMeta {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface Page<T> {
  content: T[];
  page: PageMeta;
}

export class ApiError extends Error {
  readonly response: ErrorResponse;
  readonly retryAfterSeconds?: number;

  constructor(response: ErrorResponse, retryAfterSeconds?: number) {
    super(response.message);
    this.name = "ApiError";
    this.response = response;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  get code(): string {
    return this.response.error;
  }

  get fieldErrors(): ErrorDetail[] {
    return this.response.details ?? [];
  }
}
