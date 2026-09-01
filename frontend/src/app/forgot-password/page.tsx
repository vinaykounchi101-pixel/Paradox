"use client";

import React, { useState } from "react";
import Link from "next/link";
import { authApi } from "@/lib/api/auth";
import { useToast } from "@/components/ui/toast";
import { Mail, ArrowRight, Loader2, CheckCircle2, ArrowLeft, AlertCircle } from "lucide-react";

export default function ForgotPasswordPage() {
  const { addToast } = useToast();
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;

    setErrorMessage(null);
    setIsSubmitting(true);
    try {
      await authApi.forgotPassword({ email });
      setIsSubmitted(true);
      addToast("Password reset instructions sent to your email!", "success");
    } catch (err: any) {
      const msg = err.message || "No account found with this email address.";
      setErrorMessage(msg);
      addToast(msg, "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-background via-background to-surface">
      <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 shadow-2xl backdrop-blur-xl animate-fade-in">
        {isSubmitted ? (
          <div className="text-center py-4">
            <div className="w-14 h-14 rounded-full bg-success/15 text-success flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground mb-2">Check Your Email</h1>
            <p className="text-sm text-foreground-muted mb-8">
              We have sent password reset instructions to <span className="font-semibold text-foreground">{email}</span>.
              Please check your inbox and spam folder.
            </p>
            <Link
              href="/login"
              className="inline-flex items-center justify-center gap-2 w-full py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-lg shadow-primary/25 transition-all"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back to Login</span>
            </Link>
          </div>
        ) : (
          <div>
            <div className="text-center mb-8">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">Reset Password</h1>
              <p className="text-sm text-foreground-muted mt-2">
                Enter your registered email address to receive password reset instructions.
              </p>
            </div>

            {errorMessage && (
              <div className="mb-5 p-3.5 rounded-xl bg-danger/10 border border-danger/20 flex items-start gap-3 text-danger text-sm animate-fade-in">
                <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <p className="font-medium">{errorMessage}</p>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-foreground-muted uppercase tracking-wider mb-2">
                  Email Address
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
                    placeholder="you@example.com"
                    className="w-full pl-10 pr-4 py-2.5 bg-background/50 border border-border/80 rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full mt-2 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-lg shadow-primary/25 transition-all duration-200 disabled:opacity-50"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Sending Instructions...</span>
                  </>
                ) : (
                  <>
                    <span>Send Reset Instructions</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>

            <div className="text-center mt-6">
              <Link
                href="/login"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-foreground-muted hover:text-foreground transition-colors"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Back to Sign In</span>
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
