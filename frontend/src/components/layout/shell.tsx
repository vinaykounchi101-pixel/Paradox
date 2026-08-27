"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import {
  LayoutDashboard,
  Receipt,
  Tags,
  PiggyBank,
  Wallet,
  Sun,
  Moon,
  Menu,
  X,
  Smartphone,
  ChevronRight,
} from "lucide-react";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { useTheme } from "@/components/common/ThemeProvider";
import { BackgroundGrid } from "@/components/common/BackgroundGrid";

interface ShellProps {
  children: React.ReactNode;
}

const navItems = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard, description: "Overview & Analytics" },
  { label: "Expenses", href: "/expenses", icon: Receipt, description: "Transactions & Records" },
  { label: "Categories", href: "/categories", icon: Tags, description: "Manage Rules & Channels" },
  { label: "Budget", href: "/budget", icon: PiggyBank, description: "Monthly, Weekly & Daily Targets" },
];

export const Shell: React.FC<ShellProps> = ({ children }) => {
  const pathname = usePathname();
  const { theme, toggleTheme } = useTheme();
  const [isOpen, setIsOpen] = useState(false);

  // Close drawer on route change
  useEffect(() => {
    setIsOpen(false);
  }, [pathname]);

  // Prevent background scrolling when menu drawer is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [isOpen]);

  return (
    <div className="relative min-h-screen bg-background flex flex-col">
      {/* Animated 3D perspective grid background */}
      <BackgroundGrid />

      {/* Top Navigation Header with Hamburger Button */}
      <header className="sticky top-0 z-40 w-full bg-card/85 backdrop-blur-md border-b border-border px-4 sm:px-6 py-3.5 flex items-center justify-between shadow-sm">
        {/* Left: Hamburger button + Brand Logo */}
        <div className="flex items-center space-x-3 sm:space-x-4">
          <button
            onClick={() => setIsOpen((prev) => !prev)}
            className="p-2 -ml-1 rounded-lg text-foreground hover:bg-secondary transition-colors cursor-pointer outline-none focus:ring-2 focus:ring-primary"
            aria-label={isOpen ? "Close Menu" : "Open Menu"}
          >
            {isOpen ? <X className="h-6 w-6 text-primary" /> : <Menu className="h-6 w-6" />}
          </button>

          {/* Brand/Logo — 3D spinning cube */}
          <Link href="/dashboard" className="flex items-center space-x-3 cursor-pointer group">
            <div className="logo-cube-scene">
              <div className="logo-cube">
                <div className="logo-cube-face front">
                  <Wallet className="h-4 w-4 text-white" />
                </div>
                <div className="logo-cube-face back">
                  <span className="text-white text-xs font-black">P</span>
                </div>
                <div className="logo-cube-face right" />
                <div className="logo-cube-face left" />
                <div className="logo-cube-face top" />
                <div className="logo-cube-face bottom" />
              </div>
            </div>
            <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-primary to-indigo-400 bg-clip-text text-transparent">
              Paradox
            </span>
          </Link>
        </div>

        {/* Center: Quick navigation links on larger screens */}
        <nav className="hidden lg:flex items-center space-x-1 bg-zinc-900/60 p-1 rounded-xl border border-border">
          {navItems.map((item) => {
            const isActive = pathname.startsWith(item.href);
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={twMerge(
                  "relative flex items-center space-x-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all",
                  isActive
                    ? "text-primary-foreground font-bold"
                    : "text-muted-foreground hover:text-foreground hover:bg-zinc-800/60"
                )}
              >
                {isActive && (
                  <motion.div
                    className="absolute inset-0 bg-primary rounded-lg -z-10 shadow-sm shadow-primary/30"
                    layoutId="activeNavTop"
                    transition={{ type: "spring", stiffness: 380, damping: 30 }}
                  />
                )}
                <Icon className="h-3.5 w-3.5" />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        {/* Right: Theme switcher + User Avatar */}
        <div className="flex items-center space-x-3">
          <button
            onClick={toggleTheme}
            className="p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-secondary transition-colors cursor-pointer"
            title={theme === "dark" ? "Switch to Light Mode" : "Switch to Dark Mode"}
          >
            {theme === "dark" ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>
          <div className="h-8 w-8 rounded-full bg-secondary flex items-center justify-center font-bold text-xs border border-border">
            U
          </div>
        </div>
      </header>

      {/* Hamburger Slide-Over Drawer Navigation */}
      <AnimatePresence>
        {isOpen && (
          <>
            {/* Backdrop Blur Overlay */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={() => setIsOpen(false)}
              className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
            />

            {/* Slide-out Drawer Panel */}
            <motion.div
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "spring", damping: 26, stiffness: 280 }}
              className="fixed top-0 left-0 bottom-0 z-50 w-80 max-w-[85vw] bg-card border-r border-border p-6 flex flex-col shadow-2xl overflow-y-auto"
            >
              {/* Drawer Header */}
              <div className="flex items-center justify-between pb-6 border-b border-border">
                <div className="flex items-center space-x-3">
                  <div className="h-9 w-9 rounded-xl bg-primary flex items-center justify-center shadow-md shadow-primary/30">
                    <Wallet className="h-5 w-5 text-primary-foreground" />
                  </div>
                  <div>
                    <span className="text-lg font-bold tracking-tight bg-gradient-to-r from-primary to-indigo-400 bg-clip-text text-transparent">
                      Paradox
                    </span>
                    <p className="text-[10px] text-muted-foreground font-mono">PWA App Edition</p>
                  </div>
                </div>

                <button
                  onClick={() => setIsOpen(false)}
                  className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-secondary transition-colors cursor-pointer"
                  title="Close navigation"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              {/* Navigation Links in Drawer */}
              <div className="py-6 flex-1 space-y-1.5">
                <p className="text-[11px] font-bold uppercase tracking-wider text-muted-foreground px-3 mb-2">
                  Navigation Menu
                </p>

                {navItems.map((item) => {
                  const isActive = pathname.startsWith(item.href);
                  const Icon = item.icon;

                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={() => setIsOpen(false)}
                      className={twMerge(
                        "relative flex items-center justify-between px-4 py-3 rounded-xl text-sm font-medium transition-all group",
                        isActive
                          ? "bg-primary text-primary-foreground font-bold shadow-md shadow-primary/20"
                          : "text-muted-foreground hover:text-foreground hover:bg-secondary/60"
                      )}
                    >
                      <div className="flex items-center space-x-3">
                        <Icon className={clsx("h-5 w-5 transition-transform group-hover:scale-110", isActive && "text-current")} />
                        <div>
                          <p className="text-sm leading-none font-semibold">{item.label}</p>
                          <p className={clsx("text-[10px] mt-0.5", isActive ? "text-primary-foreground/80" : "text-muted-foreground")}>
                            {item.description}
                          </p>
                        </div>
                      </div>
                      <ChevronRight className={clsx("h-4 w-4 opacity-60", isActive && "text-current opacity-100")} />
                    </Link>
                  );
                })}
              </div>

              {/* Drawer Footer */}
              <div className="border-t border-border pt-4 mt-auto space-y-4">
                {/* Theme Mode Selector */}
                <div className="flex items-center justify-between bg-zinc-900/60 p-3 rounded-xl border border-border">
                  <div className="flex items-center space-x-2">
                    {theme === "dark" ? <Moon className="h-4 w-4 text-primary" /> : <Sun className="h-4 w-4 text-amber-500" />}
                    <span className="text-xs font-semibold capitalize text-foreground">{theme} Theme</span>
                  </div>
                  <button
                    onClick={toggleTheme}
                    className="text-xs px-2.5 py-1 rounded-md bg-secondary hover:bg-zinc-800 text-foreground font-medium transition-colors cursor-pointer border border-border"
                  >
                    Switch
                  </button>
                </div>

                {/* User Session Info */}
                <div className="flex items-center justify-between px-1">
                  <div className="flex items-center space-x-3">
                    <div className="h-9 w-9 rounded-full bg-primary/20 text-primary flex items-center justify-center font-bold text-xs border border-primary/30">
                      U
                    </div>
                    <div>
                      <p className="text-xs font-semibold text-foreground">Primary User</p>
                      <div className="flex items-center space-x-1 text-[10px] text-emerald-400">
                        <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                        <span>Online • PWA Enabled</span>
                      </div>
                    </div>
                  </div>
                  <Smartphone className="h-4 w-4 text-muted-foreground" />
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Main Content Area */}
      <main className="relative z-10 flex-1 flex flex-col min-w-0 pb-16 overflow-y-auto">
        <motion.div
          key={pathname}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, ease: "easeOut" }}
          className="flex-1"
        >
          {children}
        </motion.div>
      </main>
    </div>
  );
};
