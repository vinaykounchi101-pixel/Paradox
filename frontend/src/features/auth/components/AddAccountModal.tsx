"use client";

import React, { useState } from "react";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useToast } from "@/components/ui/toast";
import { GoogleSignInButton } from "./GoogleSignInButton";
import { UserPlus, Loader2, Mail, Lock } from "lucide-react";

interface AddAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function AddAccountModal({ isOpen, onClose }: AddAccountModalProps) {
  const { login } = useAuth();
  const { success, error: toastError } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      toastError("Please enter both email and password.");
      return;
    }

    setLoading(true);
    try {
      await login({ email, password });
      success("Account added and switched successfully!");
      onClose();
      if (typeof window !== "undefined") {
        window.location.href = "/dashboard";
      }
    } catch (err: any) {
      toastError(err?.message || "Failed to authenticate account.");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = () => {
    success("Google account added and switched successfully!");
    onClose();
    if (typeof window !== "undefined") {
      window.location.href = "/dashboard";
    }
  };

  return (
    <Dialog isOpen={isOpen} onClose={onClose} title="Add Another Account" className="max-w-sm">
      <div className="space-y-4 pt-1">
        <div className="flex items-center gap-2 p-3 rounded-xl bg-primary/10 border border-primary/20 text-xs text-primary">
          <UserPlus className="w-4 h-4 shrink-0" />
          <span>Sign in with another account. Your current session will remain saved.</span>
        </div>

        {/* Google OAuth Option */}
        <div className="flex flex-col items-center justify-center pt-1">
          <GoogleSignInButton onSuccess={handleGoogleSuccess} text="signin_with" />
        </div>

        <div className="relative flex py-1 items-center">
          <div className="flex-grow border-t border-border" />
          <span className="flex-shrink mx-3 text-[11px] uppercase tracking-wider text-muted-foreground font-semibold">
            Or with email
          </span>
          <div className="flex-grow border-t border-border" />
        </div>

        {/* Email/Password Form */}
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Email Address</label>
            <div className="relative">
              <Input
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="pl-8"
              />
              <Mail className="w-3.5 h-3.5 text-muted-foreground absolute left-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Password</label>
            <div className="relative">
              <Input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="pl-8"
              />
              <Lock className="w-3.5 h-3.5 text-muted-foreground absolute left-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            </div>
          </div>

          <div className="flex gap-2.5 pt-2">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={onClose}
              disabled={loading}
              className="flex-1 cursor-pointer"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={loading}
              className="flex-1 cursor-pointer"
            >
              {loading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : "Sign In & Add"}
            </Button>
          </div>
        </form>
      </div>
    </Dialog>
  );
}
