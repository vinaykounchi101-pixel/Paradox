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
  SavedAccount,
  User,
} from "@/features/auth/types";

const SAVED_ACCOUNTS_STORAGE_KEY = "paradox_saved_accounts";
const ACTIVE_USER_STORAGE_KEY = "paradox_active_user_id";

function loadSavedAccounts(): SavedAccount[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(SAVED_ACCOUNTS_STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function persistSavedAccounts(accounts: SavedAccount[]) {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(SAVED_ACCOUNTS_STORAGE_KEY, JSON.stringify(accounts));
  } catch {}
}

function getActiveUserId(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return localStorage.getItem(ACTIVE_USER_STORAGE_KEY);
  } catch {
    return null;
  }
}

function setActiveUserId(userId: string | null) {
  if (typeof window === "undefined") return;
  try {
    if (userId) {
      localStorage.setItem(ACTIVE_USER_STORAGE_KEY, userId);
    } else {
      localStorage.removeItem(ACTIVE_USER_STORAGE_KEY);
    }
  } catch {}
}

interface AuthContextType {
  user: User | null;
  savedAccounts: SavedAccount[];
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<AuthTokens>;
  register: (data: RegisterRequest) => Promise<AuthTokens>;
  completeRegistration: (data: CompleteRegistrationRequest) => Promise<AuthTokens>;
  loginWithGoogle: (data: GoogleLoginRequest) => Promise<AuthTokens>;
  switchAccount: (userId: string) => Promise<void>;
  removeSavedAccount: (userId: string) => void;
  logout: () => Promise<void>;
  logoutAll: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [savedAccounts, setSavedAccounts] = useState<SavedAccount[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Initialize saved accounts on mount
  useEffect(() => {
    setSavedAccounts(loadSavedAccounts());
  }, []);

  const saveOrUpdateAccount = useCallback((u: User, refreshToken?: string) => {
    if (!refreshToken) return;
    setSavedAccounts((prev) => {
      const existingIdx = prev.findIndex((a) => a.user.id === u.id);
      let updated: SavedAccount[];
      if (existingIdx >= 0) {
        updated = [...prev];
        updated[existingIdx] = { user: u, refreshToken };
      } else {
        updated = [...prev, { user: u, refreshToken }];
      }
      persistSavedAccounts(updated);
      return updated;
    });
  }, []);

  const removeSavedAccount = useCallback((userId: string) => {
    setSavedAccounts((prev) => {
      const updated = prev.filter((a) => a.user.id !== userId);
      persistSavedAccounts(updated);
      return updated;
    });
  }, []);

  const initAuth = useCallback(async () => {
    try {
      const activeId = getActiveUserId();
      const saved = loadSavedAccounts();
      const activeAccount = saved.find((a) => a.user.id === activeId);

      // If we have an explicitly selected active account with a refresh token, restore it
      if (activeAccount && activeAccount.refreshToken) {
        try {
          const res = await authApi.switchAccount(activeAccount.refreshToken);
          if (res?.user) {
            setUser(res.user);
            setActiveUserId(res.user.id);
            if (res.refresh_token) {
              saveOrUpdateAccount(res.user, res.refresh_token);
            }
            return;
          }
        } catch {
          // Token expired or invalid
          removeSavedAccount(activeAccount.user.id);
        }
      }

      // Default fallback: silent session restoration using HttpOnly cookie
      const data = await authApi.refresh();
      if (data?.user) {
        setUser(data.user);
        setActiveUserId(data.user.id);
        if (data.refresh_token) {
          saveOrUpdateAccount(data.user, data.refresh_token);
        }
      }
    } catch {
      // If cookie refresh failed, check if any saved account has a valid token
      const saved = loadSavedAccounts();
      if (saved.length > 0 && saved[0].refreshToken) {
        try {
          const res = await authApi.switchAccount(saved[0].refreshToken);
          if (res?.user) {
            setUser(res.user);
            setActiveUserId(res.user.id);
            saveOrUpdateAccount(res.user, res.refresh_token || saved[0].refreshToken);
            return;
          }
        } catch {
          removeSavedAccount(saved[0].user.id);
        }
      }
      setUser(null);
      setActiveUserId(null);
      setAccessToken(null);
    } finally {
      setIsLoading(false);
    }
  }, [saveOrUpdateAccount, removeSavedAccount]);

  useEffect(() => {
    initAuth();
  }, [initAuth]);

  const login = async (data: LoginRequest): Promise<AuthTokens> => {
    const res = await authApi.login(data);
    setUser(res.user);
    setActiveUserId(res.user.id);
    if (res.refresh_token) {
      saveOrUpdateAccount(res.user, res.refresh_token);
    }
    return res;
  };

  const register = async (data: RegisterRequest): Promise<AuthTokens> => {
    const res = await authApi.register(data);
    setUser(res.user);
    setActiveUserId(res.user.id);
    if (res.refresh_token) {
      saveOrUpdateAccount(res.user, res.refresh_token);
    }
    return res;
  };

  const completeRegistration = async (data: CompleteRegistrationRequest): Promise<AuthTokens> => {
    const res = await authApi.completeRegistration(data);
    setUser(res.user);
    setActiveUserId(res.user.id);
    if (res.refresh_token) {
      saveOrUpdateAccount(res.user, res.refresh_token);
    }
    return res;
  };

  const loginWithGoogle = async (data: GoogleLoginRequest): Promise<AuthTokens> => {
    const res = await authApi.loginWithGoogle(data);
    setUser(res.user);
    setActiveUserId(res.user.id);
    if (res.refresh_token) {
      saveOrUpdateAccount(res.user, res.refresh_token);
    }
    return res;
  };

  const switchAccount = async (userId: string): Promise<void> => {
    const saved = loadSavedAccounts();
    const target = saved.find((a) => a.user.id === userId);
    if (!target) {
      throw new Error("Target account not found in saved accounts list.");
    }
    if (!target.refreshToken) {
      throw new Error(
        `Session credentials missing for ${target.user.email}. Please click "Add" to reconnect this account.`
      );
    }
    setIsLoading(true);
    try {
      const res = await authApi.switchAccount(target.refreshToken);
      setUser(res.user);
      setActiveUserId(res.user.id);
      if (res.refresh_token) {
        saveOrUpdateAccount(res.user, res.refresh_token);
      }
      // Force page reload to flush all React Query cache and state unconditionally
      if (typeof window !== "undefined") {
        window.location.reload();
      }
    } catch (err: any) {
      // Only remove if explicitly unauthorized / token revoked
      if (err?.status === 401 || err?.status === 403) {
        removeSavedAccount(userId);
      }
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    const currentUserId = user?.id;
    try {
      await authApi.logout();
    } catch {
      // Ignore network errors during logout
    } finally {
      if (currentUserId) {
        removeSavedAccount(currentUserId);
      }
      const remaining = loadSavedAccounts().filter((a) => a.user.id !== currentUserId);
      if (remaining.length > 0) {
        setActiveUserId(remaining[0].user.id);
        try {
          await switchAccount(remaining[0].user.id);
          return;
        } catch {
          // If fallback switch fails, proceed to clear
        }
      }
      setActiveUserId(null);
      setUser(null);
      setAccessToken(null);
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
    }
  };

  const logoutAll = async (): Promise<void> => {
    try {
      await authApi.logoutAll();
    } catch {
      // Ignore network errors during logout
    } finally {
      setActiveUserId(null);
      setSavedAccounts([]);
      persistSavedAccounts([]);
      setUser(null);
      setAccessToken(null);
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
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
        savedAccounts,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        completeRegistration,
        loginWithGoogle,
        switchAccount,
        removeSavedAccount,
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
