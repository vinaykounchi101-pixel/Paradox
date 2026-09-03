"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
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
  ChevronRight,
  LogOut,
  KeyRound,
  ShieldAlert,
  User as UserIcon,
  Check,
  UserPlus,
  Users,
  Loader2,
} from "lucide-react";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { useTheme } from "@/components/common/ThemeProvider";
import { BackgroundGrid } from "@/components/common/BackgroundGrid";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useCurrency, CurrencyCode } from "@/features/auth/context/CurrencyContext";
import { ChangePasswordModal } from "@/features/auth/components/ChangePasswordModal";
import { AddAccountModal } from "@/features/auth/components/AddAccountModal";
import { useToast } from "@/components/ui/toast";

function cn(...inputs: any[]) {
  return twMerge(clsx(inputs));
}

interface ShellProps {
  children: React.ReactNode;
}

const navItems = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard, description: "Overview & Analytics" },
  { label: "Expenses", href: "/expenses", icon: Receipt, description: "Transactions & Records" },
  { label: "Categories", href: "/categories", icon: Tags, description: "Manage Rules & Channels" },
  { label: "Budget", href: "/budget", icon: PiggyBank, description: "Monthly, Weekly & Daily Targets" },
];

const authRoutes = ["/login", "/register", "/forgot-password", "/reset-password"];

export const Shell: React.FC<ShellProps> = ({ children }) => {
  const pathname = usePathname();
  const router = useRouter();
  const { theme, toggleTheme } = useTheme();
  const { user, isAuthenticated, savedAccounts = [], switchAccount, logout, logoutAll } = useAuth();
  const { addToast } = useToast();
  const { currency, setCurrency } = useCurrency();

  const [isOpen, setIsOpen] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [isAddAccountOpen, setIsAddAccountOpen] = useState(false);
  const [isSwitching, setIsSwitching] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement>(null);

  const isAuthRoute = authRoutes.some((route) => pathname.startsWith(route));

  // Close drawer on route change
  useEffect(() => {
    setIsOpen(false);
    setIsProfileMenuOpen(false);
  }, [pathname]);

  // Click outside to close profile dropdown
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (profileMenuRef.current && !profileMenuRef.current.contains(event.target as Node)) {
        setIsProfileMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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

  const handleLogout = async () => {
    await logout();
    addToast("Logged out successfully.", "info");
    router.push("/login");
  };

  const handleLogoutAll = async () => {
    await logoutAll();
    addToast("Logged out from all devices.", "info");
    router.push("/login");
  };

  // On standalone auth pages, render children with background grid and no header
  if (isAuthRoute) {
    return (
      <div className="relative min-h-screen bg-background flex flex-col">
        <BackgroundGrid />
        <main className="relative z-10 flex-1 flex flex-col min-w-0">{children}</main>
      </div>
    );
  }

  const userInitial = user?.display_name ? user.display_name.charAt(0).toUpperCase() : (user?.email?.charAt(0).toUpperCase() || "U");

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

        {/* Right: Currency switcher + Theme switcher + User Profile Menu */}
        <div className="flex items-center space-x-2.5">
          {/* Currency Switcher Dropdown */}
          <div className="relative">
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value as CurrencyCode)}
              className="bg-zinc-900/80 hover:bg-zinc-800 text-foreground border border-border/80 rounded-lg px-2.5 py-1 text-xs font-bold cursor-pointer outline-none focus:ring-1 focus:ring-primary appearance-none pr-5 transition-colors"
              title="Select display currency"
            >
              <option value="INR">₹ INR</option>
              <option value="USD">$ USD</option>
              <option value="EUR">€ EUR</option>
              <option value="GBP">£ GBP</option>
            </select>
            <span className="pointer-events-none absolute right-1.5 top-1/2 -translate-y-1/2 text-[9px] text-muted-foreground">▼</span>
          </div>

          <button
            onClick={toggleTheme}
            className="p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-secondary transition-colors cursor-pointer"
            title={theme === "dark" ? "Switch to Light Mode" : "Switch to Dark Mode"}
          >
            {theme === "dark" ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>

          {isAuthenticated ? (
            <div className="relative" ref={profileMenuRef}>
              <button
                onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
                className="h-8 w-8 rounded-full bg-primary/20 text-primary hover:bg-primary/30 flex items-center justify-center font-bold text-xs border border-primary/40 transition-colors focus:outline-none focus:ring-2 focus:ring-primary/40 overflow-hidden"
              >
                {user?.avatar_url ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={user.avatar_url} alt={user.display_name} className="w-full h-full object-cover" />
                ) : (
                  <span>{userInitial}</span>
                )}
              </button>

              <AnimatePresence>
                {isProfileMenuOpen && (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.95, y: -4 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95, y: -4 }}
                    transition={{ duration: 0.15 }}
                    className="absolute right-0 mt-2 w-64 p-2 rounded-2xl bg-surface border border-border shadow-2xl z-50 animate-fade-in"
                  >
                    <div className="px-3 py-2 border-b border-border/60 mb-1">
                      <p className="text-xs font-semibold text-foreground truncate">{user?.display_name || "User"}</p>
                      <p className="text-[11px] text-foreground-muted truncate">{user?.email}</p>
                    </div>

                    {/* Multi-Account Switcher Section */}
                    <div className="py-1.5 border-b border-border/60 mb-1 space-y-1">
                      <div className="flex items-center justify-between px-3 text-[10px] font-bold text-muted-foreground uppercase tracking-wider">
                        <span className="flex items-center gap-1">
                          <Users className="w-3 h-3 text-primary" /> Accounts
                        </span>
                        <button
                          type="button"
                          onClick={() => {
                            setIsProfileMenuOpen(false);
                            setIsAddAccountOpen(true);
                          }}
                          className="text-primary hover:text-primary-hover flex items-center gap-1 text-[10px] font-semibold cursor-pointer"
                        >
                          <UserPlus className="w-2.5 h-2.5" /> Add
                        </button>
                      </div>

                      {savedAccounts.map((acc) => {
                        const isCurrent = acc.user.id === user?.id;
                        return (
                          <button
                            key={acc.user.id}
                            type="button"
                            disabled={isSwitching}
                            onClick={async () => {
                              if (isCurrent) {
                                addToast(`You are already using ${acc.user.email}`, "info");
                                setIsProfileMenuOpen(false);
                                return;
                              }
                              setIsSwitching(true);
                              try {
                                await switchAccount(acc.user.id);
                              } catch (err: any) {
                                addToast(err?.message || "Failed to switch account", "error");
                                setIsSwitching(false);
                              }
                            }}
                            className={cn(
                              "w-full flex items-center justify-between px-3 py-1.5 text-xs rounded-xl transition-colors text-left",
                              isCurrent
                                ? "bg-primary/10 text-primary font-semibold"
                                : "hover:bg-surface-hover text-foreground-muted hover:text-foreground cursor-pointer"
                            )}
                          >
                            <div className="flex items-center gap-2 truncate pr-2">
                              <div className="w-5 h-5 rounded-full bg-primary/20 text-primary flex items-center justify-center text-[10px] font-bold shrink-0 overflow-hidden">
                                {acc.user.avatar_url ? (
                                  // eslint-disable-next-line @next/next/no-img-element
                                  <img src={acc.user.avatar_url} alt="" className="w-full h-full object-cover" />
                                ) : (
                                  (acc.user.display_name?.[0] || acc.user.email[0]).toUpperCase()
                                )}
                              </div>
                              <span className="truncate text-[11px] font-medium">{acc.user.email}</span>
                            </div>
                            {isCurrent ? (
                              <Check className="w-3.5 h-3.5 text-primary shrink-0" />
                            ) : isSwitching ? (
                              <Loader2 className="w-3 h-3 text-primary animate-spin shrink-0" />
                            ) : (
                              <span className="text-[10px] text-muted-foreground hover:text-primary shrink-0">
                                Switch
                              </span>
                            )}
                          </button>
                        );
                      })}
                    </div>

                    <button
                      onClick={() => {
                        setIsProfileMenuOpen(false);
                        setIsChangePasswordOpen(true);
                      }}
                      className="w-full flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground-muted hover:text-foreground hover:bg-surface-hover rounded-xl transition-colors"
                    >
                      <KeyRound className="w-3.5 h-3.5 text-primary" />
                      <span>Change Password</span>
                    </button>

                    <button
                      onClick={() => {
                        setIsProfileMenuOpen(false);
                        handleLogout();
                      }}
                      className="w-full flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-foreground-muted hover:text-foreground hover:bg-surface-hover rounded-xl transition-colors"
                    >
                      <LogOut className="w-3.5 h-3.5 text-warning" />
                      <span>Sign Out</span>
                    </button>

                    <button
                      onClick={() => {
                        setIsProfileMenuOpen(false);
                        handleLogoutAll();
                      }}
                      className="w-full flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-danger hover:bg-danger/10 rounded-xl transition-colors"
                    >
                      <ShieldAlert className="w-3.5 h-3.5" />
                      <span>Sign Out from All Devices</span>
                    </button>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          ) : (
            <Link
              href="/login"
              className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-primary hover:bg-primary-hover text-white transition-colors shadow-sm"
            >
              Sign In
            </Link>
          )}
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
                {isAuthenticated ? (
                  <div className="p-3 rounded-xl bg-surface border border-border/80 space-y-2">
                    <div className="flex items-center space-x-3">
                      <div className="h-9 w-9 rounded-full bg-primary/20 text-primary flex items-center justify-center font-bold text-xs border border-primary/30 overflow-hidden">
                        {user?.avatar_url ? (
                          // eslint-disable-next-line @next/next/no-img-element
                          <img src={user.avatar_url} alt={user.display_name} className="w-full h-full object-cover" />
                        ) : (
                          <span>{userInitial}</span>
                        )}
                      </div>
                      <div className="overflow-hidden">
                        <p className="text-xs font-semibold text-foreground truncate">{user?.display_name || "User"}</p>
                        <p className="text-[10px] text-foreground-muted truncate">{user?.email}</p>
                      </div>
                    </div>

                    <div className="pt-2 border-t border-border/60 flex items-center justify-between">
                      <button
                        onClick={() => {
                          setIsOpen(false);
                          setIsChangePasswordOpen(true);
                        }}
                        className="text-[11px] text-foreground-muted hover:text-foreground flex items-center gap-1"
                      >
                        <KeyRound className="w-3 h-3 text-primary" />
                        <span>Password</span>
                      </button>
                      <button
                        onClick={() => {
                          setIsOpen(false);
                          handleLogout();
                        }}
                        className="text-[11px] text-danger hover:underline flex items-center gap-1"
                      >
                        <LogOut className="w-3 h-3" />
                        <span>Sign Out</span>
                      </button>
                    </div>
                  </div>
                ) : (
                  <Link
                    href="/login"
                    onClick={() => setIsOpen(false)}
                    className="w-full py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-sm font-semibold flex items-center justify-center shadow-md transition-colors"
                  >
                    Sign In to Paradox
                  </Link>
                )}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Change Password Modal */}
      <ChangePasswordModal isOpen={isChangePasswordOpen} onClose={() => setIsChangePasswordOpen(false)} />

      {/* Add Another Account Modal */}
      <AddAccountModal isOpen={isAddAccountOpen} onClose={() => setIsAddAccountOpen(false)} />

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
