import { client, setAccessToken } from "./client";
import {
  AuthTokens,
  ChangePasswordRequest,
  CompleteRegistrationRequest,
  ForgotPasswordRequest,
  GoogleLoginRequest,
  InitiateRegistrationRequest,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  ResetPasswordRequest,
  User,
  ValidateRegistrationTokenResponse,
} from "@/features/auth/types";

export const authApi = {
  async initiateRegistration(data: InitiateRegistrationRequest): Promise<MessageResponse> {
    return await client.post<MessageResponse>("/auth/register/initiate", data);
  },

  async validateRegistrationToken(token: string): Promise<ValidateRegistrationTokenResponse> {
    return await client.get<ValidateRegistrationTokenResponse>(
      `/auth/register/validate-token?token=${encodeURIComponent(token)}`
    );
  },

  async completeRegistration(data: CompleteRegistrationRequest): Promise<AuthTokens> {
    const res = await client.post<AuthTokens>("/auth/register/complete", data);
    if (res.access_token) {
      setAccessToken(res.access_token);
    }
    return res;
  },

  async register(data: RegisterRequest): Promise<AuthTokens> {
    const res = await client.post<AuthTokens>("/auth/register", data);
    if (res.access_token) {
      setAccessToken(res.access_token);
    }
    return res;
  },

  async login(data: LoginRequest): Promise<AuthTokens> {
    const res = await client.post<AuthTokens>("/auth/login", data);
    if (res.access_token) {
      setAccessToken(res.access_token);
    }
    return res;
  },

  async loginWithGoogle(data: GoogleLoginRequest): Promise<AuthTokens> {
    const res = await client.post<AuthTokens>("/auth/google", data);
    if (res.access_token) {
      setAccessToken(res.access_token);
    }
    return res;
  },

  async refresh(): Promise<AuthTokens> {
    const res = await client.post<AuthTokens>("/auth/refresh");
    if (res.access_token) {
      setAccessToken(res.access_token);
    }
    return res;
  },

  async logout(): Promise<MessageResponse> {
    try {
      return await client.post<MessageResponse>("/auth/logout");
    } finally {
      setAccessToken(null);
    }
  },

  async logoutAll(): Promise<MessageResponse> {
    try {
      return await client.post<MessageResponse>("/auth/logout-all");
    } finally {
      setAccessToken(null);
    }
  },

  async getMe(): Promise<User> {
    return await client.get<User>("/auth/me");
  },

  async updateMe(data: { currency?: string; display_name?: string }): Promise<User> {
    return await client.patch<User>("/auth/me", data);
  },

  async forgotPassword(data: ForgotPasswordRequest): Promise<MessageResponse> {
    return await client.post<MessageResponse>("/auth/forgot-password", data);
  },

  async resetPassword(data: ResetPasswordRequest): Promise<MessageResponse> {
    return await client.post<MessageResponse>("/auth/reset-password", data);
  },

  async changePassword(data: ChangePasswordRequest): Promise<MessageResponse> {
    return await client.post<MessageResponse>("/auth/change-password", data);
  },
};
