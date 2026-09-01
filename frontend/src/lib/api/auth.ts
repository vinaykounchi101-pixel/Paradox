import { client, setAccessToken } from "./client";
import {
  AuthTokens,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  GoogleLoginRequest,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  ResetPasswordRequest,
  User,
} from "@/features/auth/types";

export const authApi = {
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
