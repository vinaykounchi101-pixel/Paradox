"use client";

import React, { createContext, useContext, useEffect, useState, useMemo } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { authApi } from "@/lib/api/auth";

export type CurrencyCode = "INR" | "USD" | "EUR" | "GBP";

interface CurrencyContextType {
  currency: CurrencyCode;
  currencySymbol: string;
  setCurrency: (code: CurrencyCode) => Promise<void>;
  formatCurrency: (amount: number | string | null | undefined) => string;
}

const SYMBOLS: Record<CurrencyCode, string> = {
  INR: "₹",
  USD: "$",
  EUR: "€",
  GBP: "£",
};

const LOCAL_STORAGE_KEY = "paradox_user_currency";

const CurrencyContext = createContext<CurrencyContextType | undefined>(undefined);

export function CurrencyProvider({ children }: { children: React.ReactNode }) {
  const { user, refreshUser } = useAuth();
  const [currency, setCurrencyState] = useState<CurrencyCode>("INR");

  // Sync from user profile or local storage
  useEffect(() => {
    if (user?.currency && user.currency in SYMBOLS) {
      setCurrencyState(user.currency as CurrencyCode);
    } else {
      const saved = typeof window !== "undefined" ? localStorage.getItem(LOCAL_STORAGE_KEY) : null;
      if (saved && saved in SYMBOLS) {
        setCurrencyState(saved as CurrencyCode);
      }
    }
  }, [user?.currency]);

  const currencySymbol = useMemo(() => SYMBOLS[currency] || "₹", [currency]);

  const setCurrency = async (code: CurrencyCode) => {
    setCurrencyState(code);
    if (typeof window !== "undefined") {
      localStorage.setItem(LOCAL_STORAGE_KEY, code);
    }
    if (user) {
      try {
        await authApi.updateMe({ currency: code });
        await refreshUser();
      } catch {
        // Local preference still preserved
      }
    }
  };

  const formatCurrency = (amount: number | string | null | undefined): string => {
    if (amount === null || amount === undefined || amount === "") return `${currencySymbol}0.00`;
    const num = typeof amount === "number" ? amount : parseFloat(String(amount));
    if (isNaN(num)) return `${currencySymbol}0.00`;
    return `${currencySymbol}${num.toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  };

  return (
    <CurrencyContext.Provider value={{ currency, currencySymbol, setCurrency, formatCurrency }}>
      {children}
    </CurrencyContext.Provider>
  );
}

export function useCurrency() {
  const context = useContext(CurrencyContext);
  if (!context) {
    throw new Error("useCurrency must be used within a CurrencyProvider");
  }
  return context;
}
