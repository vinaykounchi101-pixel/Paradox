"use client";

import React, { Suspense } from "react";
import { RegisterForm } from "@/features/auth/components/RegisterForm";
import { Loader2 } from "lucide-react";

export default function RegisterPage() {
  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-background via-background to-surface">
      <Suspense
        fallback={
          <div className="flex items-center justify-center p-12">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        }
      >
        <RegisterForm />
      </Suspense>
    </div>
  );
}
