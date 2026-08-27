"use client";

import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import {
  PiggyBank,
  Save,
  AlertTriangle,
  CheckCircle,
  Info,
  Trash2,
  Calendar,
  Edit2,
  Plus,
} from "lucide-react";
import { useBudget, useBudgetsList, useBudgetMutation } from "../hooks/useBudget";
import { useDashboard } from "@/features/dashboard/hooks/useDashboard";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";
import { BudgetRead } from "@/lib/api/budget";

export default function BudgetConfigView() {
  const { success, error: toastError } = useToast();

  // Helper for current month in YYYY-MM
  const currentMonthStr = new Date().toISOString().slice(0, 7);

  // Selected month state
  const [selectedMonth, setSelectedMonth] = useState<string>(currentMonthStr);

  // Queries & Mutations
  const { data: budget, isLoading: loadingBudget } = useBudget(selectedMonth);
  const { data: allBudgets = [], isLoading: loadingAllBudgets } = useBudgetsList();
  const { data: dashboard, isLoading: loadingDashboard } = useDashboard("current_month");
  const { upsertBudget, isSaving, deleteBudget, isDeleting } = useBudgetMutation();

  // Local Form State
  const [amount, setAmount] = useState("");
  const [error, setError] = useState("");

  // Delete modal state
  const [deleteTargetMonth, setDeleteTargetMonth] = useState<string | null>(null);

  // Sync loaded budget value when selectedMonth or budget data changes
  useEffect(() => {
    if (budget && budget.amount !== null && budget.amount !== undefined) {
      setAmount(budget.amount);
    } else {
      setAmount("");
    }
  }, [budget, selectedMonth]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError("Budget must be greater than 0");
      return;
    }

    try {
      await upsertBudget({ amount: parsedAmount, month: selectedMonth });
      success(`Budget for ${formatMonthLabel(selectedMonth)} saved successfully`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to save budget";
      toastError(msg);
    }
  };

  const handleDelete = async (monthToDelete: string) => {
    try {
      await deleteBudget(monthToDelete);
      if (monthToDelete === selectedMonth) {
        setAmount("");
      }
      setDeleteTargetMonth(null);
      success(`Budget for ${formatMonthLabel(monthToDelete)} deleted successfully`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to delete budget";
      toastError(msg);
    }
  };

  const formatMonthLabel = (monthStr?: string | null) => {
    if (!monthStr) return "";
    const [y, m] = monthStr.split("-");
    const d = new Date(parseInt(y), parseInt(m) - 1, 1);
    return d.toLocaleDateString(undefined, { month: "long", year: "numeric" });
  };

  const isLoading = loadingBudget || loadingDashboard;

  const currentLimit = budget?.amount ? parseFloat(budget.amount) : 0;
  const currentSpent = dashboard?.total_spent ? parseFloat(dashboard.total_spent) : 0;
  const isCurrentMonth = selectedMonth === currentMonthStr;
  const isOverBudget = currentLimit > 0 && currentSpent > currentLimit;
  const isNearBudget = currentLimit > 0 && !isOverBudget && currentSpent >= currentLimit * 0.8;
  const pct = currentLimit > 0 ? Math.min((currentSpent / currentLimit) * 100, 100) : 0;

  return (
    <div className="page-container space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-glow">Budget Planner</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Configure, manage, and track spending targets for any month.
          </p>
        </div>

        {/* Month Selector Picker */}
        <div className="flex items-center gap-2 bg-card border border-border px-3 py-2 rounded-lg shadow-sm">
          <Calendar className="h-4 w-4 text-primary" />
          <span className="text-xs font-semibold text-muted-foreground">Target Month:</span>
          <input
            type="month"
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(e.target.value)}
            className="bg-transparent text-sm font-semibold text-foreground outline-none cursor-pointer"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Configuration Form Card */}
        <Card variant="glass" className="md:col-span-1 h-fit">
          <CardHeader>
            <div className="flex items-center space-x-2">
              <PiggyBank className="h-5 w-5 text-primary" />
              <CardTitle>Set Budget</CardTitle>
            </div>
            <CardDescription>
              Configuring budget for <strong>{formatMonthLabel(selectedMonth)}</strong>
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                label={`Limit for ${formatMonthLabel(selectedMonth)} ($)`}
                type="number"
                step="1"
                min="0"
                placeholder="e.g. 1500"
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
                  max={Math.max(5000, parseInt(amount) || 5000)}
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
                  <span>${(Math.max(5000, parseInt(amount) || 5000) / 2).toLocaleString()}</span>
                  <span>${Math.max(5000, parseInt(amount) || 5000).toLocaleString()}</span>
                </div>
              </div>

              {/* Action buttons */}
              <div className="flex gap-3 pt-1">
                <button
                  type="submit"
                  disabled={isSaving || isDeleting}
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
                    onClick={() => setDeleteTargetMonth(selectedMonth)}
                    disabled={isSaving || isDeleting}
                    className="inline-flex items-center justify-center gap-1.5 h-10 px-3 text-sm font-medium rounded-lg border border-destructive/40 text-destructive hover:bg-destructive/10 transition-colors disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                    title="Delete this month's budget"
                  >
                    <Trash2 className="h-4 w-4" />
                    Delete
                  </button>
                )}
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Current status metrics */}
        <Card variant="glass" className="md:col-span-2">
          <CardHeader>
            <CardTitle>Spending Status for {formatMonthLabel(selectedMonth)}</CardTitle>
            <CardDescription>Live audit of your active target</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {isLoading ? (
              <div className="space-y-4 animate-pulse">
                <div className="h-10 bg-zinc-800 rounded" />
                <div className="h-20 bg-zinc-800 rounded" />
              </div>
            ) : currentLimit === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 text-center text-muted-foreground">
                <Info className="h-10 w-10 mb-2 opacity-40 text-primary" />
                <p className="text-sm font-semibold text-foreground">
                  No budget set for {formatMonthLabel(selectedMonth)}
                </p>
                <p className="text-xs mt-1 max-w-xs">
                  Use the configuration form on the left to set a monthly allowance limit.
                </p>
              </div>
            ) : (
              <div className="space-y-6">
                {/* Visual warning boxes for current month */}
                {isCurrentMonth && (
                  <>
                    {isOverBudget ? (
                      <div className="flex items-center space-x-3 text-destructive bg-destructive/10 p-4 rounded-lg border border-destructive/20">
                        <AlertTriangle className="h-6 w-6 flex-shrink-0" />
                        <div>
                          <p className="text-sm font-bold">Over Budget limit!</p>
                          <p className="text-xs text-muted-foreground mt-0.5">
                            You have exceeded your limit by <strong>${(currentSpent - currentLimit).toFixed(2)}</strong>.
                          </p>
                        </div>
                      </div>
                    ) : isNearBudget ? (
                      <div className="flex items-center space-x-3 text-amber-500 bg-amber-500/10 p-4 rounded-lg border border-amber-500/20">
                        <AlertTriangle className="h-6 w-6 flex-shrink-0" />
                        <div>
                          <p className="text-sm font-bold">Approaching budget limit</p>
                          <p className="text-xs text-muted-foreground mt-0.5">
                            You have consumed over 80% of your allowance. You have <strong>${(currentLimit - currentSpent).toFixed(2)}</strong> remaining.
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="flex items-center space-x-3 text-emerald-500 bg-emerald-500/10 p-4 rounded-lg border border-emerald-500/20">
                        <CheckCircle className="h-6 w-6 flex-shrink-0" />
                        <div>
                          <p className="text-sm font-bold">Budget on track</p>
                          <p className="text-xs text-muted-foreground mt-0.5">
                            Your spending is safe under your target. You have <strong>${(currentLimit - currentSpent).toFixed(2)}</strong> remaining.
                          </p>
                        </div>
                      </div>
                    )}
                  </>
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
                      {isCurrentMonth ? "Total Spent" : "Month Target"}
                    </p>
                    <p className="text-2xl font-extrabold tracking-tight">
                      {isCurrentMonth ? `$${currentSpent.toFixed(2)}` : formatMonthLabel(selectedMonth)}
                    </p>
                  </div>
                  <div className="col-span-2 sm:col-span-1 space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      {isCurrentMonth ? "Allowance Remaining" : "Status"}
                    </p>
                    <p className="text-2xl font-extrabold tracking-tight text-emerald-500">
                      {isCurrentMonth
                        ? `$${(currentLimit - currentSpent > 0 ? currentLimit - currentSpent : 0).toFixed(2)}`
                        : "Active"}
                    </p>
                  </div>
                </div>

                {/* Progress bar for current month */}
                {isCurrentMonth && (
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
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* All Configured Monthly Budgets History Table */}
      <Card variant="glass" className="w-full">
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>All Configured Monthly Budgets</CardTitle>
            <CardDescription>History and upcoming monthly allowances</CardDescription>
          </div>
          <Button
            size="sm"
            variant="secondary"
            onClick={() => setSelectedMonth(currentMonthStr)}
            className="text-xs cursor-pointer"
          >
            <Plus className="h-3.5 w-3.5 mr-1" /> Current Month
          </Button>
        </CardHeader>
        <CardContent>
          {loadingAllBudgets ? (
            <div className="space-y-2 animate-pulse">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="h-12 bg-zinc-800 rounded" />
              ))}
            </div>
          ) : allBudgets.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground text-sm">
              No budgets configured yet. Use the form above to add your first monthly budget.
            </div>
          ) : (
            <div className="divide-y divide-border">
              {allBudgets.map((b) => (
                <div
                  key={b.id || b.month}
                  className="flex items-center justify-between py-3.5 first:pt-0 last:pb-0"
                >
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-sm text-foreground">
                        {formatMonthLabel(b.month)}
                      </span>
                      {b.month === currentMonthStr && (
                        <span className="text-[10px] bg-primary/20 text-primary px-1.5 py-0.5 rounded font-bold uppercase">
                          Current
                        </span>
                      )}
                      {b.month === selectedMonth && (
                        <span className="text-[10px] bg-emerald-500/20 text-emerald-400 px-1.5 py-0.5 rounded font-bold uppercase">
                          Selected
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground font-mono">
                      Month code: {b.month}
                    </p>
                  </div>

                  <div className="flex items-center gap-4">
                    <span className="font-extrabold text-base text-foreground">
                      ${parseFloat(b.amount || "0").toFixed(2)}
                    </span>
                    <div className="flex items-center space-x-1">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => {
                          if (b.month) setSelectedMonth(b.month);
                        }}
                        className="text-muted-foreground hover:text-foreground hover:bg-secondary cursor-pointer h-8 w-8 p-0"
                        title="Edit this budget"
                      >
                        <Edit2 className="h-4 w-4" />
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => {
                          if (b.month) setDeleteTargetMonth(b.month);
                        }}
                        className="text-destructive hover:bg-destructive/10 hover:text-red-500 cursor-pointer h-8 w-8 p-0"
                        title="Delete this budget"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Delete Confirmation Dialog */}
      <Dialog
        isOpen={!!deleteTargetMonth}
        onClose={() => setDeleteTargetMonth(null)}
        title="Delete Monthly Budget"
      >
        <div className="space-y-4">
          <div className="flex items-center space-x-3 text-destructive bg-destructive/10 p-3 rounded-lg border border-destructive/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <p className="text-xs font-medium">
              This will remove the spending target limit for <strong>{formatMonthLabel(deleteTargetMonth)}</strong>.
            </p>
          </div>
          <p className="text-sm text-foreground">
            Are you sure you want to delete the budget configuration for <strong>"{formatMonthLabel(deleteTargetMonth)}"</strong>?
          </p>
          <div className="flex space-x-3 pt-2">
            <Button
              variant="secondary"
              className="flex-1 cursor-pointer"
              onClick={() => setDeleteTargetMonth(null)}
              disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              className="flex-1 cursor-pointer"
              onClick={() => deleteTargetMonth && handleDelete(deleteTargetMonth)}
              isLoading={isDeleting}
            >
              Delete
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
