import type { Metadata, Viewport } from "next";
import React from "react";
import QueryProvider from "@/components/common/QueryProvider";
import { ThemeProvider } from "@/components/common/ThemeProvider";
import { ToastProvider } from "@/components/ui/toast";
import { Shell } from "@/components/layout/shell";
import { PwaRegister } from "@/components/common/PwaRegister";
import "@/styles/globals.css";

export const viewport: Viewport = {
  themeColor: "#6366f1",
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
};

export const metadata: Metadata = {
  title: "Paradox — Personal Expense Tracker",
  description: "A clean, premium, environment-driven personal expense tracker that puts you in control of your financial destiny.",
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "black-translucent",
    title: "Paradox",
  },
  icons: {
    icon: "/icons/icon-192.svg",
    apple: "/icons/icon-192.svg",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <head>
        <link rel="manifest" href="/manifest.json" />
        <meta name="mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
      </head>
      <body className="antialiased">
        <PwaRegister />
        <QueryProvider>
          <ThemeProvider>
            <ToastProvider>
              <Shell>{children}</Shell>
            </ToastProvider>
          </ThemeProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
