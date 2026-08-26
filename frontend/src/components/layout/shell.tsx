"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion } from "framer-motion";
import { LayoutDashboard, Receipt, Tags, PiggyBank, Wallet } from "lucide-react";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

interface ShellProps {
  children: React.ReactNode;
}

const navItems = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Expenses", href: "/expenses", icon: Receipt },
  { label: "Categories", href: "/categories", icon: Tags },
  { label: "Budget", href: "/budget", icon: PiggyBank },
];

export const Shell: React.FC<ShellProps> = ({ children }) => {
  const pathname = usePathname();

  return (
    <div className="min-h-screen bg-background flex flex-col md:flex-row">
      {/* Desktop Sidebar Navigation */}
      <aside className="hidden md:flex flex-col w-64 bg-card border-r border-border p-6 flex-shrink-0">
        {/* Brand/Logo */}
        <div className="flex items-center space-x-3 mb-8">
          <div className="h-9 w-9 rounded-lg bg-primary flex items-center justify-center shadow-lg shadow-primary/20">
            <Wallet className="h-5 w-5 text-primary-foreground" />
          </div>
          <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-primary to-indigo-400 bg-clip-text text-transparent">
            Paradox
          </span>
        </div>

        {/* Navigation links */}
        <nav className="flex-1 space-y-1">
          {navItems.map((item) => {
            const isActive = pathname.startsWith(item.href);
            const Icon = item.icon;

            return (
              <Link
                key={item.href}
                href={item.href}
                className={twMerge(
                  "relative flex items-center space-x-3 px-4 py-3 rounded-lg text-sm font-medium transition-all group",
                  isActive
                    ? "text-primary-foreground font-semibold"
                    : "text-muted-foreground hover:text-foreground hover:bg-zinc-900/50"
                )}
              >
                {/* Active highlight background */}
                {isActive && (
                  <motion.div
                    className="absolute inset-0 bg-primary rounded-lg -z-10 shadow-md shadow-primary/20"
                    layoutId="activeNavDesktop"
                    transition={{ type: "spring", stiffness: 380, damping: 30 }}
                  />
                )}
                <Icon className={clsx("h-5 w-5 transition-transform duration-200 group-hover:scale-105", isActive && "text-current")} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        {/* Footer/User state placeholder */}
        <div className="border-t border-border pt-4 mt-auto">
          <div className="flex items-center space-x-3">
            <div className="h-9 w-9 rounded-full bg-zinc-800 flex items-center justify-center font-bold text-xs">
              U
            </div>
            <div>
              <p className="text-xs font-semibold text-foreground">Primary User</p>
              <p className="text-[10px] text-muted-foreground">V1 MVP Session</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Mobile Header / Brand */}
      <header className="md:hidden flex items-center justify-between bg-card border-b border-border px-6 py-4 sticky top-0 z-40">
        <div className="flex items-center space-x-2">
          <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center">
            <Wallet className="h-4.5 w-4.5 text-primary-foreground" />
          </div>
          <span className="text-lg font-bold tracking-tight bg-gradient-to-r from-primary to-indigo-400 bg-clip-text text-transparent">
            Paradox
          </span>
        </div>
        <div className="h-8 w-8 rounded-full bg-zinc-800 flex items-center justify-center font-bold text-xs">
          U
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col min-w-0 pb-20 md:pb-0 overflow-y-auto">
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

      {/* Mobile Bottom Navigation Bar */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-card/85 backdrop-blur-md border-t border-border flex justify-around py-3 px-4 shadow-[0_-4px_24px_rgba(0,0,0,0.4)]">
        {navItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={twMerge(
                "relative flex flex-col items-center space-y-1 py-1 px-3 text-[10px] font-medium transition-all",
                isActive ? "text-primary font-semibold" : "text-muted-foreground hover:text-foreground"
              )}
            >
              {isActive && (
                <motion.div
                  className="absolute -top-3 w-10 h-1 bg-primary rounded-full"
                  layoutId="activeNavMobileIndicator"
                  transition={{ type: "spring", stiffness: 380, damping: 30 }}
                />
              )}
              <Icon className="h-5 w-5" />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
};
