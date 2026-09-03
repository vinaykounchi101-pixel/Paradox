"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/api/auth";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { GoogleSignInButton } from "./GoogleSignInButton";
import { useToast } from "@/components/ui/toast";
import {
  Mail,
  ArrowRight,
  Loader2,
  AlertCircle,
  CheckCircle2,
  RefreshCw,
  ArrowLeft,
  KeyRound,
  Lock,
  User as UserIcon,
  Smartphone,
} from "lucide-react";

export function RegisterForm() {
  const router = useRouter();
  const { addToast } = useToast();
  const { verifyOtpAndRegister } = useAuth();

  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isResending, setIsResending] = useState(false);

  // OTP and final registration fields
  const [otp, setOtp] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [isVerifyingOtp, setIsVerifyingOtp] = useState(false);
  const [isMobileVerified, setIsMobileVerified] = useState(false);

  // Background polling for cross-device approval
  useEffect(() => {
    if (!isSubmitted || !email) return;

    let isMounted = true;
    const interval = setInterval(async () => {
      try {
        const res = await authApi.checkRegistrationStatus(email);
        if (!isMounted) return;

        if (res.status === "completed") {
          addToast("Registration completed via phone link! Redirecting...", "success");
          router.push("/dashboard");
        } else if (res.status === "verified") {
          setIsMobileVerified(true);
        }
      } catch {
        // Silently ignore polling errors
      }
    }, 3000);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [isSubmitted, email, router, addToast]);

  const handleSubmitEmail = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!email) {
      setErrorMessage("Please enter a valid email address.");
      return;
    }

    setIsSubmitting(true);
    try {
      await authApi.initiateRegistration({ email });
      setIsSubmitted(true);
      addToast("Verification code & link sent to your email!", "success");
    } catch (err: any) {
      const msg = err.message || "Failed to send verification code. Please try again.";
      setErrorMessage(msg);
      addToast(msg, "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (otp.trim().length !== 6) {
      setErrorMessage("Please enter the 6-digit code from your email.");
      return;
    }

    if (!password || password.length < 8) {
      setErrorMessage("Password must be at least 8 characters long.");
      return;
    }

    setIsVerifyingOtp(true);
    try {
      await verifyOtpAndRegister({
        email,
        otp: otp.trim(),
        password,
        display_name: displayName.trim() || undefined,
      });
      addToast("Account created successfully! Welcome to Paradox.", "success");
      router.push("/dashboard");
    } catch (err: any) {
      const msg = err.message || "Invalid or expired code. Please try again.";
      setErrorMessage(msg);
      addToast(msg, "error");
    } finally {
      setIsVerifyingOtp(false);
    }
  };

  const handleResend = async () => {
    setIsResending(true);
    try {
      await authApi.initiateRegistration({ email });
      addToast("A fresh 6-digit code and link have been dispatched.", "success");
    } catch (err: any) {
      addToast(err.message || "Failed to resend code.", "error");
    } finally {
      setIsResending(false);
    }
  };

  const handleGoogleSuccess = () => {
    router.push("/dashboard");
  };

  // Step 2: Verification Code & Password Setup
  if (isSubmitted) {
    return (
      <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 shadow-2xl backdrop-blur-xl animate-fade-in">
        <div className="text-center mb-6">
          <div className="w-14 h-14 rounded-full bg-primary/15 text-primary flex items-center justify-center mx-auto mb-3 shadow-inner">
            <KeyRound className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Enter Verification Code</h1>
          <p className="text-xs text-foreground-muted mt-1.5 leading-relaxed">
            We sent a 6-digit code to <span className="font-semibold text-foreground">{email}</span>.
          </p>
        </div>

        {isMobileVerified && (
          <div className="mb-5 p-3 rounded-xl bg-success/15 border border-success/30 text-success text-xs flex items-center gap-2.5 animate-slide-up">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>Mobile link clicked! Set your password below to finish.</span>
          </div>
        )}

        {errorMessage && (
          <div className="mb-5 p-3 rounded-xl bg-danger/10 border border-danger/30 text-danger text-xs flex items-start gap-2 animate-slide-up">
            <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleVerifyOtp} className="space-y-3.5">
          <div>
            <label className="block text-[11px] font-semibold text-foreground-muted uppercase tracking-wider mb-1.5">
              6-Digit Code *
            </label>
            <input
              type="text"
              required
              maxLength={6}
              autoFocus
              value={otp}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, "");
                setOtp(val);
                if (errorMessage) setErrorMessage(null);
              }}
              placeholder="123456"
              className="w-full text-center tracking-[0.4em] font-mono text-xl font-bold py-2.5 bg-background/60 border border-border/80 rounded-xl text-foreground focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
            />
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-foreground-muted uppercase tracking-wider mb-1.5">
              Your Name (Optional)
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-foreground-muted">
                <UserIcon className="w-3.5 h-3.5" />
              </div>
              <input
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="Alex Mercer"
                className="w-full pl-9 pr-3.5 py-2 bg-background/50 border border-border/80 rounded-xl text-foreground text-xs focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
              />
            </div>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-foreground-muted uppercase tracking-wider mb-1.5">
              Set Password (Min. 8 characters) *
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-foreground-muted">
                <Lock className="w-3.5 h-3.5" />
              </div>
              <input
                type="password"
                required
                minLength={8}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (errorMessage) setErrorMessage(null);
                }}
                placeholder="••••••••••••"
                className="w-full pl-9 pr-3.5 py-2 bg-background/50 border border-border/80 rounded-xl text-foreground text-xs focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isVerifyingOtp || otp.length !== 6 || password.length < 8}
            className="w-full mt-2 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-lg shadow-primary/25 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isVerifyingOtp ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Creating Account...</span>
              </>
            ) : (
              <>
                <span>Complete Registration</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>

        {/* Real-time cross-device banner */}
        <div className="mt-4 p-2.5 rounded-xl bg-surface border border-border/60 flex items-center justify-center gap-2 text-[11px] text-foreground-muted">
          <Smartphone className="w-3.5 h-3.5 text-primary animate-pulse" />
          <span>Or tap the link on your phone to auto-sync</span>
        </div>

        <div className="space-y-2 mt-4">
          <button
            type="button"
            onClick={handleResend}
            disabled={isResending}
            className="w-full py-2 px-3 rounded-xl bg-surface hover:bg-surface-hover border border-border text-foreground text-xs font-semibold flex items-center justify-center gap-1.5 transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isResending ? "animate-spin" : ""}`} />
            <span>{isResending ? "Resending..." : "Resend Code"}</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setIsSubmitted(false);
              setEmail("");
              setOtp("");
              setPassword("");
            }}
            className="w-full text-[11px] text-foreground-muted hover:text-foreground transition-colors py-1"
          >
            Entered wrong email? Change email
          </button>

          <div className="pt-2 border-t border-border/60">
            <Link
              href="/login"
              className="w-full py-2 px-3 rounded-xl bg-primary/10 hover:bg-primary/20 text-primary border border-primary/20 text-xs font-semibold flex items-center justify-center gap-1.5 transition-all shadow-sm"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Go Back to Login</span>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Step 1: Email Entry Form
  return (
    <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 shadow-2xl backdrop-blur-xl animate-fade-in">
      <div className="text-center mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Create an Account</h1>
        <p className="text-sm text-foreground-muted mt-2">
          Verify your email to get started tracking expenses with Paradox.
        </p>
      </div>

      {errorMessage && (
        <div className="mb-6 p-4 rounded-xl bg-danger/10 border border-danger/30 text-danger text-sm flex items-start gap-3 animate-slide-up">
          <AlertCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      <form onSubmit={handleSubmitEmail} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-foreground-muted uppercase tracking-wider mb-2">
            Email Address *
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-foreground-muted">
              <Mail className="w-4 h-4" />
            </div>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (errorMessage) setErrorMessage(null);
              }}
              placeholder="alex@example.com"
              className="w-full pl-10 pr-4 py-2.5 bg-background/50 border border-border/80 rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
              autoComplete="email"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={isSubmitting || !email}
          className="w-full mt-2 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-lg shadow-primary/25 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-primary/40"
        >
          {isSubmitting ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Sending Verification...</span>
            </>
          ) : (
            <>
              <span>Continue with Email</span>
              <ArrowRight className="w-4 h-4" />
            </>
          )}
        </button>
      </form>

      <div className="relative my-6">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-border/60" />
        </div>
        <div className="relative flex justify-center text-xs">
          <span className="bg-surface px-3 text-foreground-muted uppercase tracking-wider font-medium">
            Or sign up with
          </span>
        </div>
      </div>

      <GoogleSignInButton onSuccess={handleGoogleSuccess} text="signup_with" />

      <p className="text-center text-xs text-foreground-muted mt-6">
        Already have an account?{" "}
        <Link
          href="/login"
          className="font-semibold text-primary hover:text-primary-hover transition-colors"
        >
          Sign in
        </Link>
      </p>
    </div>
  );
}
