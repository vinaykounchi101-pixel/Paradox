"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { GoogleSignInButton } from "./GoogleSignInButton";
import { useToast } from "@/components/ui/toast";
import { Eye, EyeOff, Lock, Mail, User as UserIcon, ArrowRight, Loader2, Check, AlertCircle } from "lucide-react";

export function RegisterForm() {
  const router = useRouter();
  const { register } = useAuth();
  const { addToast } = useToast();

  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const isPasswordValid = password.length >= 8;
  const doPasswordsMatch = password === confirmPassword && confirmPassword.length > 0;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!email || !password) {
      setErrorMessage("Please fill in all required fields.");
      return;
    }

    if (!isPasswordValid) {
      setErrorMessage("Password must be at least 8 characters long.");
      return;
    }

    if (password !== confirmPassword) {
      setErrorMessage("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);
    try {
      await register({
        email,
        password,
        display_name: displayName.trim() || undefined,
      });
      addToast("Account created successfully!", "success");
      router.push("/dashboard");
    } catch (err: any) {
      const msg = err.message || "Failed to create account. Please try again.";
      setErrorMessage(msg);
      addToast(msg, "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleGoogleSuccess = () => {
    router.push("/dashboard");
  };

  return (
    <div className="w-full max-w-md p-8 rounded-2xl bg-surface/80 border border-border/80 shadow-2xl backdrop-blur-xl animate-fade-in">
      <div className="text-center mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Create an Account</h1>
        <p className="text-sm text-foreground-muted mt-2">
          Start tracking expenses and managing your budget with Paradox.
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
            Your Name (Optional)
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-foreground-muted">
              <UserIcon className="w-4 h-4" />
            </div>
            <input
              type="text"
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
              onChange={(e) => setEmail(e.target.value)}
              placeholder="alex@example.com"
              className="w-full pl-10 pr-4 py-2.5 bg-background/50 border border-border/80 rounded-xl text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-all duration-200"
              autoComplete="email"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-foreground-muted uppercase tracking-wider mb-2">
            Password *
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
          disabled={isSubmitting || !isPasswordValid || (confirmPassword.length > 0 && !doPasswordsMatch)}
          className="w-full mt-2 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold shadow-lg shadow-primary/25 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-primary/40"
        >
          {isSubmitting ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Creating account...</span>
            </>
          ) : (
            <>
              <span>Create Account</span>
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
