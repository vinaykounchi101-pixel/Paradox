"use client";

import { useEffect } from "react";

export function PwaRegister() {
  useEffect(() => {
    if (
      typeof window !== "undefined" &&
      "serviceWorker" in navigator &&
      window.location.protocol === "https:"
    ) {
      navigator.serviceWorker
        .register("/sw.js")
        .then((reg) => {
          // Check for service worker updates
          reg.update().catch(() => {});
        })
        .catch((error) => {
          console.warn("PWA registration non-critical note:", error);
        });
    }
  }, []);

  return null;
}
