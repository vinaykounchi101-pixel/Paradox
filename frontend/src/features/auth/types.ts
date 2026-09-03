export interface User {
  id: string;
  email: string;
  display_name: string;
  avatar_url?: string | null;
  is_verified: boolean;
  currency?: string;
  created_at: string;
}

export interface AuthTokens {
  access_token: string;
  token_type: string;
  user: User;
  refresh_token?: string;
}

export interface SavedAccount {
  user: User;
  refreshToken: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  display_name?: string;
}

export interface InitiateRegistrationRequest {
  email: string;
}

export interface ValidateRegistrationTokenResponse {
  email: string;
  valid: boolean;
}

export interface CompleteRegistrationRequest {
  token: string;
  display_name: string;
  password: string;
}

export interface GoogleLoginRequest {
  id_token: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  new_password: string;
}

export interface ChangePasswordRequest {
  current_password: string;
  new_password: string;
}

export interface MessageResponse {
  message: string;
  success?: boolean;
}

export interface VerifyOtpRegisterRequest {
  email: string;
  otp: string;
  password: string;
  display_name?: string;
}

export interface RegistrationStatusResponse {
  email: string;
  status: "pending" | "verified" | "completed" | "expired" | "none";
  message: string;
}

