"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/api/auth";
import { GoogleSignInButton } from "./GoogleSignInButton";
import { useToast } from "@/components/ui/toast";
import { Mail, ArrowRight, Loader2, AlertCircle, CheckCircle2, RefreshCw } from "lucide-react";

export function RegisterForm() {
  const router = useRouter();
  const { addToast } = useToast();

  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isResending, setIsResending] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
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
      addToast("Verification link sent! Check your inbox.", "success");
    } catch (err: any) {
      const msg = err.message || "Failed to send verification link. Please try again.";
      setErrorMessage(msg);
      addToast(msg, "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    setIsResending(true);
    try {
      await authApi.initiateRegistration({ email });
      addToast("A fresh verification link has been sent to your email.", "success");
    } catch (err: any) {
      addToast(err.message || "Failed to resend link.", "error");
    } finally {
      setIsResending(false);
    }
  };

  const handleGoogleSuccess = () => {
    router.push("/dashboard");
  };

  if (isSubmitted) {
    return (
      <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 shadow-2xl backdrop-blur-xl animate-fade-in text-center">
        <div className="w-14 h-14 rounded-full bg-success/15 text-success flex items-center justify-center mx-auto mb-4">
          <CheckCircle2 className="w-8 h-8" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground mb-2">Check Your Email</h1>
        <p className="text-sm text-foreground-muted mb-6">
          We sent a verification link to <span className="font-semibold text-foreground">{email}</span>.
          Click the link in the email to verify your address and complete your account setup.
        </p>

        <div className="p-4 rounded-xl bg-surface border border-border/60 mb-6 text-xs text-foreground-muted text-left space-y-1.5">
          <p className="font-semibold text-foreground">Next steps:</p>
          <p>1. Open your email inbox (or spam folder).</p>
          <p>2. Click <strong>"Complete Registration"</strong>.</p>
          <p>3. Choose your display name & password.</p>
        </div>

        <div className="space-y-3">
          <button
            type="button"
            onClick={handleResend}
            disabled={isResending}
            className="w-full py-2.5 px-4 rounded-xl bg-surface hover:bg-surface-hover border border-border text-foreground text-sm font-semibold flex items-center justify-center gap-2 transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isResending ? "animate-spin" : ""}`} />
            <span>{isResending ? "Resending Link..." : "Resend Verification Link"}</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setIsSubmitted(false);
              setEmail("");
            }}
            className="w-full text-xs text-foreground-muted hover:text-foreground transition-colors py-1.5"
          >
            Entered wrong email? Change email
          </button>
        </div>
      </div>
    );
  }

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

      <form onSubmit={handleSubmit} className="space-y-4">
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
              <span>Sending Verification Link...</span>
            </>
          ) : (
            <>
              <span>Send Verification Link</span>
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
