"use client";

import React, { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { authApi } from "@/lib/api/auth";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useToast } from "@/components/ui/toast";
import {
  Eye,
  EyeOff,
  Lock,
  User as UserIcon,
  ArrowRight,
  Loader2,
  Check,
  AlertCircle,
  CheckCircle2,
} from "lucide-react";

function CompleteRegistrationContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";
  const { addToast } = useToast();
  const { completeRegistration } = useAuth();

  const [isValidating, setIsValidating] = useState(true);
  const [email, setEmail] = useState<string | null>(null);
  const [tokenError, setTokenError] = useState<string | null>(null);

  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const isPasswordValid = password.length >= 8;
  const doPasswordsMatch = password === confirmPassword && confirmPassword.length > 0;

  useEffect(() => {
    if (!token) {
      setTokenError("No registration verification token provided in URL.");
      setIsValidating(false);
      return;
    }

    const validateToken = async () => {
      try {
        const res = await authApi.validateRegistrationToken(token);
        setEmail(res.email);
      } catch (err: any) {
        setTokenError(
          err.message || "This registration link is invalid or has expired. Please try registering again."
        );
      } finally {
        setIsValidating(false);
      }
    };

    validateToken();
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!displayName.trim()) {
      setErrorMessage("Please enter your name.");
      return;
    }

    if (!isPasswordValid) {
      setErrorMessage("Password must be at least 8 characters long.");
      return;
    }

    if (!doPasswordsMatch) {
      setErrorMessage("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);
    try {
      await completeRegistration({
        token,
        display_name: displayName.trim(),
        password,
      });
      addToast("Account setup complete! Welcome to Paradox.", "success");
      router.push("/dashboard");
    } catch (err: any) {
      const msg = err.message || "Failed to complete registration. Please try again.";
      setErrorMessage(msg);
      addToast(msg, "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isValidating) {
    return (
      <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 shadow-2xl backdrop-blur-xl animate-fade-in text-center">
        <Loader2 className="w-8 h-8 animate-spin text-primary mx-auto mb-4" />
        <h2 className="text-lg font-semibold text-foreground">Verifying Link</h2>
        <p className="text-xs text-foreground-muted mt-1">
          Please wait while we validate your registration link...
        </p>
      </div>
    );
  }

  if (tokenError) {
    return (
      <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 shadow-2xl backdrop-blur-xl animate-fade-in text-center">
        <div className="w-14 h-14 rounded-full bg-danger/15 text-danger flex items-center justify-center mx-auto mb-4">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground mb-2">Invalid or Expired Link</h1>
        <p className="text-sm text-foreground-muted mb-6">{tokenError}</p>
        <Link
          href="/register"
          className="inline-flex items-center justify-center gap-2 w-full py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-lg shadow-primary/25 transition-all"
        >
          <span>Start Registration Again</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    );
  }

  return (
    <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 shadow-2xl backdrop-blur-xl animate-fade-in">
      <div className="text-center mb-6">
        <div className="w-12 h-12 rounded-full bg-success/15 text-success flex items-center justify-center mx-auto mb-3">
          <CheckCircle2 className="w-6 h-6" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Complete Your Account</h1>
        <p className="text-xs text-foreground-muted mt-1">
          Email verified: <span className="font-semibold text-primary">{email}</span>
        </p>
      </div>

      {errorMessage && (
        <div className="mb-6 p-4 rounded-xl bg-danger/10 border border-danger/30 text-danger text-sm flex items-start gap-3 animate-slide-up">
          <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-foreground-muted uppercase tracking-wider mb-2">
            Your Name *
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-foreground-muted">
              <UserIcon className="w-4 h-4" />
            </div>
            <input
              type="text"
              required
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="Alex Smith"
              className="w-full pl-10 pr-4 py-2.5 bg-background/50 border border-border/80 rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
              autoComplete="name"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-foreground-muted uppercase tracking-wider mb-2">
            Choose Password *
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-foreground-muted">
              <Lock className="w-4 h-4" />
            </div>
            <input
              type={showPassword ? "text" : "password"}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="At least 8 characters"
              className="w-full pl-10 pr-10 py-2.5 bg-background/50 border border-border/80 rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
              autoComplete="new-password"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-foreground-muted hover:text-foreground transition-colors"
            >
              {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
          <div className="mt-1.5 flex items-center gap-1.5 text-xs">
            <div className={`w-1.5 h-1.5 rounded-full ${isPasswordValid ? "bg-success" : "bg-foreground-muted"}`} />
            <span className={isPasswordValid ? "text-success" : "text-foreground-muted"}>
              Minimum 8 characters
            </span>
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-foreground-muted uppercase tracking-wider mb-2">
            Confirm Password *
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-foreground-muted">
              <Lock className="w-4 h-4" />
            </div>
            <input
              type={showPassword ? "text" : "password"}
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirm your password"
              className="w-full pl-10 pr-10 py-2.5 bg-background/50 border border-border/80 rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
              autoComplete="new-password"
            />
            {doPasswordsMatch && (
              <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-success">
                <Check className="w-4 h-4" />
              </div>
            )}
          </div>
        </div>

        <button
          type="submit"
          disabled={isSubmitting || !displayName.trim() || !isPasswordValid || !doPasswordsMatch}
          className="w-full mt-2 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-lg shadow-primary/25 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-primary/40"
        >
          {isSubmitting ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Completing setup...</span>
            </>
          ) : (
            <>
              <span>Complete Account Setup</span>
              <ArrowRight className="w-4 h-4" />
            </>
          )}
        </button>
      </form>
    </div>
  );
}

export default function CompleteRegistrationPage() {
  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-background via-background to-surface">
      <Suspense
        fallback={
          <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 text-center">
            <Loader2 className="w-8 h-8 animate-spin text-primary mx-auto mb-2" />
            <p className="text-sm text-foreground-muted">Loading...</p>
          </div>
        }
      >
        <CompleteRegistrationContent />
      </Suspense>
    </div>
  );
}
