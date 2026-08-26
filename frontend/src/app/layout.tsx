import type { Metadata } from "next";
import React from "react";
import QueryProvider from "@/components/common/QueryProvider";
import { ToastProvider } from "@/components/ui/toast";
import { Shell } from "@/components/layout/shell";
import "@/styles/globals.css";

export const metadata: Metadata = {
  title: "Paradox — Personal Expense Tracker",
  description: "A clean, premium, environment-driven personal expense tracker that puts you in control of your financial destiny.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body className="antialiased">
        <QueryProvider>
          <ToastProvider>
            <Shell>{children}</Shell>
          </ToastProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
