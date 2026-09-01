"use client";

import React, { useEffect, useRef, useState } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useToast } from "@/components/ui/toast";

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: any) => void;
          renderButton: (parent: HTMLElement, options: any) => void;
          prompt: (momentListener?: (notification: any) => void) => void;
        };
      };
    };
  }
}

interface GoogleSignInButtonProps {
  onSuccess?: () => void;
  text?: "signin_with" | "signup_with" | "continue_with" | "signin";
}

export function GoogleSignInButton({
  onSuccess,
  text = "continue_with",
}: GoogleSignInButtonProps) {
  const { loginWithGoogle } = useAuth();
  const { addToast } = useToast();
  const buttonRef = useRef<HTMLDivElement>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Clean client ID string if quoted or contains whitespace
  const rawClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  const clientId = rawClientId?.trim().replace(/^["']|["']$/g, "");

  useEffect(() => {
    if (!clientId) {
      return;
    }

    let isMounted = true;

    // Load Google Identity Services script if not present
    if (!window.google?.accounts?.id) {
      const existingScript = document.getElementById("google-gsi-script");
      if (!existingScript) {
        const script = document.createElement("script");
        script.id = "google-gsi-script";
        script.src = "https://accounts.google.com/gsi/client";
        script.async = true;
        script.defer = true;
        script.onload = () => {
          if (isMounted) initializeGoogle();
        };
        script.onerror = () => {
          console.error("Failed to load Google Identity Services SDK.");
        };
        document.head.appendChild(script);
      } else {
        existingScript.addEventListener("load", () => {
          if (isMounted) initializeGoogle();
        });
      }
    } else {
      initializeGoogle();
    }

    function initializeGoogle() {
      if (!window.google?.accounts?.id || !buttonRef.current || !clientId || !isMounted) return;

      try {
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: handleCredentialResponse,
          auto_select: false,
          cancel_on_tap_outside: true,
          itp_support: true,
        });

        if (buttonRef.current) {
          buttonRef.current.innerHTML = "";
          window.google.accounts.id.renderButton(buttonRef.current, {
            type: "standard",
            theme: "filled_black",
            size: "large",
            shape: "pill",
            text,
            width: 360,
            logo_alignment: "left",
          });
        }
      } catch (err) {
        console.error("Error initializing Google Sign-In button:", err);
      }
    }

    async function handleCredentialResponse(response: { credential: string }) {
      if (!response.credential || !isMounted) return;

      setIsLoading(true);
      try {
        await loginWithGoogle({ id_token: response.credential });
        addToast("Successfully signed in with Google!", "success");
        onSuccess?.();
      } catch (err: any) {
        const msg = err.message || "Failed to authenticate with Google.";
        addToast(msg, "error");
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    return () => {
      isMounted = false;
    };
  }, [clientId, loginWithGoogle, onSuccess, text, addToast]);

  const handleManualClick = () => {
    if (!clientId) {
      addToast(
        "Google Client ID is missing. Please set NEXT_PUBLIC_GOOGLE_CLIENT_ID in frontend/.env.local and restart frontend server.",
        "error"
      );
    }
  };

  return (
    <div className="w-full flex flex-col items-center justify-center">
      {clientId ? (
        <div className="w-full flex justify-center">
          <div ref={buttonRef} className="min-h-[44px] flex items-center justify-center w-full" />
        </div>
      ) : (
        <button
          type="button"
          onClick={handleManualClick}
          className="w-full flex items-center justify-center gap-3 py-2.5 px-4 rounded-xl border border-border/80 bg-surface/60 hover:bg-surface text-foreground text-sm font-medium transition-all duration-200 shadow-sm hover:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/40"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24">
            <path
              fill="#EA4335"
              d="M12 5c1.6 0 3 .6 4.1 1.7l3.1-3.1C17.3 1.8 14.8 1 12 1 7.5 1 3.7 3.6 1.9 7.3l3.7 2.9C6.5 7.4 9 5 12 5z"
            />
            <path
              fill="#4285F4"
              d="M23.5 12.3c0-.8-.1-1.6-.2-2.3H12v4.5h6.5c-.3 1.5-1.1 2.8-2.4 3.7l3.7 2.9c2.2-2 3.7-5 3.7-8.8z"
            />
            <path
              fill="#FBBC05"
              d="M5.6 14.8c-.2-.7-.4-1.5-.4-2.8s.2-2.1.4-2.8L1.9 6.3C.7 8.7 0 10.8 0 12s.7 3.3 1.9 5.7l3.7-2.9z"
            />
            <path
              fill="#34A853"
              d="M12 23c3.2 0 6-1.1 8-3l-3.7-2.9c-1.1.7-2.5 1.2-4.3 1.2-3 0-5.5-2.4-6.4-5.2L1.9 16c1.8 3.7 5.6 7 10.1 7z"
            />
          </svg>
          Continue with Google
        </button>
      )}
    </div>
  );
}
