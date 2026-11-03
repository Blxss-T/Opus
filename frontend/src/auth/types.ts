export type Role = "SUPER_ADMIN" | "ORG_ADMIN" | "MANAGER" | "EMPLOYEE";

export type OtpType = "EMAIL_VERIFICATION" | "LOGIN_2FA" | "PASSWORD_RESET";

export interface UserResponse {
  id: string;
  organizationId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  active: boolean;
  emailVerified: boolean;
}

export interface OrganizationResponse {
  id: string;
  name: string;
  slug: string;
  active: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  user: UserResponse;
  organization: OrganizationResponse;
}

export interface RegisterRequest {
  organizationName: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

export interface SendOtpRequest {
  email: string;
  type: OtpType;
}

export interface VerifyOtpRequest {
  email: string;
  code: string;
  type: OtpType;
}

export interface OtpResponse {
  success: boolean;
  message: string;
  expiresAt: string | null;
}

export interface MessageResponse {
  message: string;
}
