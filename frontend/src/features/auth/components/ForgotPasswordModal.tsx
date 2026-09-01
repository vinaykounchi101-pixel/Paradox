"use client";

import React, { useState } from "react";
import { authApi } from "@/lib/api/auth";
import { useToast } from "@/components/ui/toast";
import { Mail, ArrowRight, Loader2, X, CheckCircle2, AlertCircle } from "lucide-react";

interface ForgotPasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function ForgotPasswordModal({ isOpen, onClose }: ForgotPasswordModalProps) {
  const { addToast } = useToast();
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;

    setErrorMessage(null);
    setIsSubmitting(true);
    try {
      await authApi.forgotPassword({ email });
      setIsSubmitted(true);
      addToast("Password reset instructions sent!", "success");
    } catch (err: any) {
      const msg = err.message || "No account found with this email address.";
      setErrorMessage(msg);
      addToast(msg, "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    setEmail("");
    setIsSubmitted(false);
    setErrorMessage(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-md p-6 rounded-2xl bg-surface border border-border shadow-2xl relative">
        <button
          onClick={handleClose}
          className="absolute top-4 right-4 text-foreground-muted hover:text-foreground p-1 rounded-lg hover:bg-surface-hover transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {isSubmitted ? (
          <div className="text-center py-6">
            <div className="w-12 h-12 rounded-full bg-success/15 text-success flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <h2 className="text-xl font-bold text-foreground mb-2">Check Your Email</h2>
            <p className="text-sm text-foreground-muted mb-6">
              We have sent instructions to reset your password to <span className="font-semibold text-foreground">{email}</span>.
            </p>
            <button
              onClick={handleClose}
              className="w-full py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold transition-all shadow-lg shadow-primary/25"
            >
              Done
            </button>
          </div>
        ) : (
          <div>
            <h2 className="text-xl font-bold text-foreground mb-2">Reset Password</h2>
            <p className="text-sm text-foreground-muted mb-4">
              Enter your email address and we will send you a link to reset your password.
            </p>

            {errorMessage && (
              <div className="mb-4 p-3 rounded-xl bg-danger/10 border border-danger/20 flex items-start gap-2.5 text-danger text-sm animate-fade-in">
                <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <span className="font-medium text-xs leading-relaxed">{errorMessage}</span>
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
                    className="w-full pl-10 pr-4 py-2.5 bg-background border border-border rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-lg shadow-primary/25 transition-all duration-200 disabled:opacity-50"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Sending link...</span>
                  </>
                ) : (
                  <>
                    <span>Send Reset Link</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
