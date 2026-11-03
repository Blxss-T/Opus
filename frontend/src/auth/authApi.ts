import { apiGet, apiPost } from "../api/client";
import type {
  AuthResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
  OtpResponse,
  RefreshRequest,
  RegisterRequest,
  ResetPasswordRequest,
  SendOtpRequest,
  UserResponse,
  VerifyOtpRequest,
} from "./types";

export function register(body: RegisterRequest): Promise<AuthResponse> {
  return apiPost<AuthResponse>("/auth/register", body);
}

export function login(body: LoginRequest): Promise<AuthResponse> {
  return apiPost<AuthResponse>("/auth/login", body);
}

export function me(): Promise<UserResponse> {
  return apiGet<UserResponse>("/auth/me");
}

export function refresh(body: RefreshRequest): Promise<AuthResponse> {
  return apiPost<AuthResponse>("/auth/refresh", body);
}

export function logout(): Promise<void> {
  return apiPost<void>("/auth/logout");
}

export function changePassword(body: ChangePasswordRequest): Promise<void> {
  return apiPost<void>("/auth/password/change", body);
}

export function forgotPassword(body: ForgotPasswordRequest): Promise<MessageResponse> {
  return apiPost<MessageResponse>("/auth/password/forgot", body);
}

export function resetPassword(body: ResetPasswordRequest): Promise<MessageResponse> {
  return apiPost<MessageResponse>("/auth/password/reset", body);
}

export function sendOtp(body: SendOtpRequest): Promise<OtpResponse> {
  return apiPost<OtpResponse>("/auth/otp/send", body);
}

export function verifyOtp(body: VerifyOtpRequest): Promise<OtpResponse> {
  return apiPost<OtpResponse>("/auth/otp/verify", body);
}
