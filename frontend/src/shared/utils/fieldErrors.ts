import { ApiError } from "../../api/types";

// Maps ApiError.fieldErrors (from a 400/422 validation response) onto a
// {fieldName: message} record forms can index into directly.
export function toFieldErrorMap(error: unknown): Record<string, string> {
  if (error instanceof ApiError) {
    return Object.fromEntries(error.fieldErrors.map((detail) => [detail.field, detail.message]));
  }
  return {};
}

export function toErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "Something went wrong. Please try again.";
}
