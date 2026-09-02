"use client";

import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import { authApi } from "@/lib/api/auth";
import { setAccessToken } from "@/lib/api/client";
import {
  AuthTokens,
  CompleteRegistrationRequest,
  GoogleLoginRequest,
  LoginRequest,
  RegisterRequest,
  User,
} from "@/features/auth/types";

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<AuthTokens>;
  register: (data: RegisterRequest) => Promise<AuthTokens>;
  completeRegistration: (data: CompleteRegistrationRequest) => Promise<AuthTokens>;
  loginWithGoogle: (data: GoogleLoginRequest) => Promise<AuthTokens>;
  logout: () => Promise<void>;
  logoutAll: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const initAuth = useCallback(async () => {
    try {
      // Attempt silent session restoration using HttpOnly cookie
      const data = await authApi.refresh();
      if (data?.user) {
        setUser(data.user);
      }
    } catch {
      // No active session or cookie expired
      setUser(null);
      setAccessToken(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    initAuth();
  }, [initAuth]);

  const login = async (data: LoginRequest): Promise<AuthTokens> => {
    const res = await authApi.login(data);
    setUser(res.user);
    return res;
  };

  const register = async (data: RegisterRequest): Promise<AuthTokens> => {
    const res = await authApi.register(data);
    setUser(res.user);
    return res;
  };

  const completeRegistration = async (data: CompleteRegistrationRequest): Promise<AuthTokens> => {
    const res = await authApi.completeRegistration(data);
    setUser(res.user);
    return res;
  };

  const loginWithGoogle = async (data: GoogleLoginRequest): Promise<AuthTokens> => {
    const res = await authApi.loginWithGoogle(data);
    setUser(res.user);
    return res;
  };

  const logout = async (): Promise<void> => {
    try {
      await authApi.logout();
    } catch {
      // Ignore network errors during logout
    } finally {
      setUser(null);
      setAccessToken(null);
    }
  };

  const logoutAll = async (): Promise<void> => {
    try {
      await authApi.logoutAll();
    } catch {
      // Ignore network errors during logout
    } finally {
      setUser(null);
      setAccessToken(null);
    }
  };

  const refreshUser = async (): Promise<void> => {
    try {
      const u = await authApi.getMe();
      setUser(u);
    } catch {
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        completeRegistration,
        loginWithGoogle,
        logout,
        logoutAll,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
