"use client";

import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  CreditCard,
  Banknote,
  Landmark,
  Wallet,
  Smartphone,
  MoreHorizontal,
  ChevronDown,
  Check,
} from "lucide-react";
import { PaymentMethodRead } from "@/lib/api/expenses";

export interface PaymentMethodDropdownProps {
  paymentMethods: PaymentMethodRead[];
  value: string;
  onChange: (id: string) => void;
  isLoading?: boolean;
  error?: string;
  label?: string;
}

const getPaymentIcon = (name: string) => {
  const lower = name.toLowerCase();
  if (lower.includes("cash")) return Banknote;
  if (lower.includes("credit") || lower.includes("debit") || lower.includes("card"))
    return CreditCard;
  if (lower.includes("bank") || lower.includes("transfer")) return Landmark;
  if (lower.includes("wallet") || lower.includes("digital") || lower.includes("crypto"))
    return Wallet;
  if (lower.includes("upi") || lower.includes("pay") || lower.includes("mobile"))
    return Smartphone;
  return MoreHorizontal;
};

export const PaymentMethodDropdown: React.FC<PaymentMethodDropdownProps> = ({
  paymentMethods,
  value,
  onChange,
  isLoading = false,
  error,
  label = "Payment Method",
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const selectedMethod = paymentMethods.find((p) => p.id === value);
  const SelectedIcon = selectedMethod ? getPaymentIcon(selectedMethod.name) : Wallet;

  return (
    <div className="flex flex-col space-y-1.5 w-full relative" ref={dropdownRef}>
      {label && (
        <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {label}
        </label>
      )}

      {/* Trigger Button */}
      <button
        type="button"
        disabled={isLoading || paymentMethods.length === 0}
        onClick={() => setIsOpen((prev) => !prev)}
        className={[
          "flex h-11 w-full items-center justify-between rounded-xl border bg-zinc-900/60 px-3.5 py-2.5 text-sm",
          "transition-all duration-150 cursor-pointer select-none outline-none",
          "focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1",
          isOpen
            ? "border-primary ring-2 ring-primary/20 bg-zinc-900/90 shadow-[0_0_12px_rgba(99,102,241,0.25)]"
            : "border-border hover:border-primary/40 hover:bg-zinc-800/80",
          error && "border-destructive focus-visible:ring-destructive",
          (isLoading || paymentMethods.length === 0) && "opacity-50 cursor-not-allowed",
        ].join(" ")}
      >
        <div className="flex items-center gap-2.5 truncate">
          <div className="p-1 rounded-md bg-primary/10 text-primary shrink-0">
            <SelectedIcon className="h-4 w-4" />
          </div>
          <span className="truncate font-medium text-foreground">
            {isLoading
              ? "Loading methods..."
              : selectedMethod
              ? selectedMethod.name
              : "Select payment method..."}
          </span>
        </div>
        <ChevronDown
          className={`h-4 w-4 text-muted-foreground transition-transform duration-200 shrink-0 ${
            isOpen ? "rotate-180 text-primary" : ""
          }`}
        />
      </button>

      {/* Dropdown Menu Popover */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="absolute top-[calc(100%+4px)] left-0 z-50 w-full overflow-hidden rounded-xl border border-border/80 bg-zinc-950/95 p-1.5 shadow-2xl backdrop-blur-xl"
          >
            <div className="max-h-56 overflow-y-auto space-y-1 scrollbar-thin scrollbar-thumb-zinc-700">
              {paymentMethods.map((pm) => {
                const Icon = getPaymentIcon(pm.name);
                const isSelected = pm.id === value;
                return (
                  <motion.button
                    key={pm.id}
                    type="button"
                    whileHover={{ x: 2 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => {
                      onChange(pm.id);
                      setIsOpen(false);
                    }}
                    className={[
                      "flex w-full items-center justify-between px-3 py-2 rounded-lg text-sm transition-colors cursor-pointer select-none",
                      isSelected
                        ? "bg-primary/15 text-primary font-semibold"
                        : "text-zinc-300 hover:bg-zinc-800/80 hover:text-foreground",
                    ].join(" ")}
                  >
                    <div className="flex items-center gap-2.5">
                      <div
                        className={`p-1 rounded-md ${
                          isSelected
                            ? "bg-primary text-primary-foreground"
                            : "bg-zinc-800 text-zinc-400"
                        }`}
                      >
                        <Icon className="h-3.5 w-3.5" />
                      </div>
                      <span>{pm.name}</span>
                      {pm.is_default && (
                        <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-zinc-800 text-zinc-400">
                          Default
                        </span>
                      )}
                    </div>
                    {isSelected && <Check className="h-4 w-4 text-primary shrink-0" />}
                  </motion.button>
                );
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Validation error */}
      {error && (
        <span className="text-xs text-destructive font-medium animate-in fade-in duration-200">
          {error}
        </span>
      )}
    </div>
  );
};
