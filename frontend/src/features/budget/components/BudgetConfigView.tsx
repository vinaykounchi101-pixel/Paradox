"use client";

import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { PiggyBank, Save, AlertTriangle, CheckCircle, Info, Trash2 } from "lucide-react";
import { useBudget, useBudgetMutation } from "../hooks/useBudget";
import { useDashboard } from "@/features/dashboard/hooks/useDashboard";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";

export default function BudgetConfigView() {
  const { success, error: toastError } = useToast();
  
  // Queries & Mutations
  const { data: budget, isLoading: loadingBudget } = useBudget();
  const { data: dashboard, isLoading: loadingDashboard } = useDashboard("current_month");
  const { upsertBudget, isSaving } = useBudgetMutation();

  // Local Form State
  const [amount, setAmount] = useState("");
  const [error, setError] = useState("");

  // Sync loaded budget value to form
  useEffect(() => {
    if (budget && budget.amount !== null) {
      setAmount(budget.amount);
    }
  }, [budget]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount < 0) {
      setError("Budget must be a non-negative number");
      return;
    }

    try {
      await upsertBudget(parsedAmount);
      success("Monthly budget saved successfully");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to save budget";
      toastError(msg);
    }
  };

  const handleClear = async () => {
    try {
      await upsertBudget(0);
      setAmount("0");
      success("Budget cleared");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to clear budget";
      toastError(msg);
    }
  };

  const isLoading = loadingBudget || loadingDashboard;

  if (isLoading) {
    return (
      <div className="page-container space-y-6 animate-pulse">
        <div className="h-8 w-48 bg-zinc-800 rounded" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-1 h-80 bg-zinc-800 rounded-lg" />
          <div className="md:col-span-2 h-80 bg-zinc-800 rounded-lg" />
        </div>
      </div>
    );
  }

  const currentLimit = budget?.amount ? parseFloat(budget.amount) : 0;
  const currentSpent = dashboard?.total_spent ? parseFloat(dashboard.total_spent) : 0;
  const isOverBudget = currentLimit > 0 && currentSpent > currentLimit;
  const isNearBudget = currentLimit > 0 && !isOverBudget && currentSpent >= currentLimit * 0.8;
  const pct = currentLimit > 0 ? Math.min((currentSpent / currentLimit) * 100, 100) : 0;

  return (
    <div className="page-container space-y-6">
      {/* Title */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-glow">Budget</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Set targets and limits to manage your monthly allowance.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Configuration Form Card */}
        <Card variant="glass" className="md:col-span-1 h-fit">
          <CardHeader>
            <div className="flex items-center space-x-2">
              <PiggyBank className="h-5 w-5 text-primary" />
              <CardTitle>Target Limit</CardTitle>
            </div>
            <CardDescription>Configure your single monthly spending limit</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                label="Monthly Limit ($)"
                type="number"
                step="1"
                min="0"
                placeholder="e.g. 1000"
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value);
                  setError("");
                }}
                error={error}
                required
              />

              {/* Slider for quick edits */}
              <div className="space-y-1 pt-1">
                <label className="text-[10px] font-semibold uppercase text-muted-foreground">
                  Quick Adjust: ${amount || 0}
                </label>
                <input
                  type="range"
                  min="0"
                  max="5000"
                  step="50"
                  value={amount ? parseInt(amount) || 0 : 0}
                  onChange={(e) => {
                    setAmount(e.target.value);
                    setError("");
                  }}
                  className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-primary"
                />
                <div className="flex justify-between text-[10px] text-muted-foreground">
                  <span>$0</span>
                  <span>$2,500</span>
                  <span>$5,000</span>
                </div>
              </div>

              {/* Action buttons — use native buttons to ensure type="submit" is honoured */}
              <div className="flex gap-3 pt-1">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="flex-1 inline-flex items-center justify-center gap-2 h-10 px-4 text-sm font-medium rounded-lg bg-primary text-primary-foreground hover:bg-indigo-500 transition-colors disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                >
                  {isSaving ? (
                    <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  Save Budget
                </button>
                {currentLimit > 0 && (
                  <button
                    type="button"
                    onClick={handleClear}
                    disabled={isSaving}
                    className="inline-flex items-center justify-center gap-1.5 h-10 px-3 text-sm font-medium rounded-lg border border-destructive/40 text-destructive hover:bg-destructive/10 transition-colors disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                    title="Clear budget limit"
                  >
                    <Trash2 className="h-4 w-4" />
                    Clear
                  </button>
                )}
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Current status metrics */}
        <Card variant="glass" className="md:col-span-2">
          <CardHeader>
            <CardTitle>Spending Status Summary</CardTitle>
            <CardDescription>Live audit of your active target</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {currentLimit === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 text-center text-muted-foreground">
                <Info className="h-10 w-10 mb-2 opacity-40 text-primary" />
                <p className="text-sm font-semibold text-foreground">No monthly limit set</p>
                <p className="text-xs mt-1 max-w-xs">
                  Fill in the configuration form on the left to activate your monthly warning limits.
                </p>
              </div>
            ) : (
              <div className="space-y-6">
                {/* Visual warning boxes */}
                {isOverBudget ? (
                  <div className="flex items-center space-x-3 text-destructive bg-destructive/10 p-4 rounded-lg border border-destructive/20">
                    <AlertTriangle className="h-6 w-6 flex-shrink-0" />
                    <div>
                      <p className="text-sm font-bold">Over Budget limit!</p>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        You have exceeded your limit by <strong>${(currentSpent - currentLimit).toFixed(2)}</strong>. Consider auditing custom expense items.
                      </p>
                    </div>
                  </div>
                ) : isNearBudget ? (
                  <div className="flex items-center space-x-3 text-amber-500 bg-amber-500/10 p-4 rounded-lg border border-amber-500/20">
                    <AlertTriangle className="h-6 w-6 flex-shrink-0" />
                    <div>
                      <p className="text-sm font-bold">Approaching budget limit</p>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        You have consumed over 80% of your target allowance. You have <strong>${(currentLimit - currentSpent).toFixed(2)}</strong> remaining.
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center space-x-3 text-emerald-500 bg-emerald-500/10 p-4 rounded-lg border border-emerald-500/20">
                    <CheckCircle className="h-6 w-6 flex-shrink-0" />
                    <div>
                      <p className="text-sm font-bold">Budget on track</p>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        Your spending is currently safe under your target limit. You have <strong>${(currentLimit - currentSpent).toFixed(2)}</strong> remaining.
                      </p>
                    </div>
                  </div>
                )}

                {/* Spent metrics grid */}
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-6 pt-2">
                  <div className="space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      Budget Limit
                    </p>
                    <p className="text-2xl font-extrabold tracking-tight">${currentLimit.toFixed(2)}</p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      Total Spent
                    </p>
                    <p className="text-2xl font-extrabold tracking-tight">${currentSpent.toFixed(2)}</p>
                  </div>
                  <div className="col-span-2 sm:col-span-1 space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      Allowance Remaining
                    </p>
                    <p className="text-2xl font-extrabold tracking-tight text-emerald-500">
                      ${(currentLimit - currentSpent > 0 ? currentLimit - currentSpent : 0).toFixed(2)}
                    </p>
                  </div>
                </div>

                {/* Progress bar */}
                <div className="space-y-2 pt-2">
                  <div className="flex justify-between text-xs text-muted-foreground">
                    <span>Spent Meter</span>
                    <span className="font-semibold text-foreground">{pct.toFixed(0)}% consumed</span>
                  </div>
                  <div className="h-3 w-full bg-zinc-800 rounded-full overflow-hidden">
                    <motion.div
                      className={`h-full rounded-full ${
                        isOverBudget
                          ? "bg-destructive"
                          : isNearBudget
                          ? "bg-amber-500"
                          : "bg-emerald-500"
                      }`}
                      initial={{ width: 0 }}
                      animate={{ width: `${pct}%` }}
                      transition={{ duration: 0.6 }}
                    />
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
