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
  Clock,
  Sun,
  PlusCircle,
  Sparkles,
  Loader2,
  Check,
} from "lucide-react";
import { useBudget, useBudgetsList, useBudgetMutation } from "../hooks/useBudget";
import { useDashboard } from "@/features/dashboard/hooks/useDashboard";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";
import { BudgetPeriodType } from "@/lib/api/budget";
import { aiApi, SuggestBudgetResponse } from "@/lib/api/ai";
import { useCurrency } from "@/features/auth/context/CurrencyContext";

// Helpers for default period keys
function getDefaultPeriodKey(type: BudgetPeriodType): string {
  const d = new Date();
  if (type === "day") {
    return d.toISOString().slice(0, 10);
  } else if (type === "week") {
    // ISO week format YYYY-Www
    const target = new Date(d.valueOf());
    const dayNr = (d.getDay() + 6) % 7;
    target.setDate(target.getDate() - dayNr + 3);
    const firstThursday = target.valueOf();
    target.setMonth(0, 1);
    if (target.getDay() !== 4) {
      target.setMonth(0, 1 + ((4 - target.getDay() + 7) % 7));
    }
    const weekNr = 1 + Math.ceil((firstThursday - target.valueOf()) / 604800000);
    return `${d.getFullYear()}-W${String(weekNr).padStart(2, "0")}`;
  } else {
    return d.toISOString().slice(0, 7);
  }
}

function formatPeriodLabel(type: BudgetPeriodType, key?: string): string {
  if (!key) return "";
  if (type === "day") {
    const parts = key.split("-");
    if (parts.length === 3) {
      const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
      return d.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric", year: "numeric" });
    }
    return key;
  } else if (type === "week") {
    const parts = key.split("-W");
    if (parts.length === 2) {
      return `Week ${parts[1]}, ${parts[0]}`;
    }
    return key;
  } else {
    const parts = key.split("-");
    if (parts.length === 2) {
      const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, 1);
      return d.toLocaleDateString(undefined, { month: "long", year: "numeric" });
    }
    return key;
  }
}

export default function BudgetConfigView() {
  const { success, error: toastError } = useToast();
  const { formatCurrency, currencySymbol } = useCurrency();

  // Active Granularity: "month" | "week" | "day"
  const [periodType, setPeriodType] = useState<BudgetPeriodType>("month");

  // Selected period key (e.g. 2026-08, 2026-W35, 2026-08-27)
  const [periodKey, setPeriodKey] = useState<string>(() => getDefaultPeriodKey("month"));

  // History table filter
  const [historyFilter, setHistoryFilter] = useState<"all" | BudgetPeriodType>("all");

  // Update period key when granularity tab changes
  const handleTypeChange = (newType: BudgetPeriodType) => {
    setPeriodType(newType);
    setPeriodKey(getDefaultPeriodKey(newType));
    setAmount("");
  };

  // Queries & Mutations
  const { data: budget, isLoading: loadingBudget } = useBudget(periodType, periodKey);
  const { data: allBudgets = [], isLoading: loadingAllBudgets } = useBudgetsList();
  const { data: dashboard, isLoading: loadingDashboard } = useDashboard(
    periodType === "week" ? "current_week" : "current_month"
  );
  const { upsertBudget, isSaving, deleteBudget, isDeleting } = useBudgetMutation();

  // Local Form State
  const [amount, setAmount] = useState("");
  const [error, setError] = useState("");

  // AI Suggestion State
  const [aiSuggestion, setAiSuggestion] = useState<SuggestBudgetResponse | null>(null);
  const [loadingAiSuggest, setLoadingAiSuggest] = useState(false);

  const handleFetchAiSuggest = async () => {
    try {
      setLoadingAiSuggest(true);
      const res = await aiApi.suggestBudget(periodType);
      setAiSuggestion(res.data);
    } catch {
      toastError("Could not fetch AI budget suggestion");
    } finally {
      setLoadingAiSuggest(false);
    }
  };

  const handleApplyAiSuggest = () => {
    if (aiSuggestion) {
      setAmount(String(aiSuggestion.suggested_amount));
      setError("");
      success(`Applied AI suggested target: $${aiSuggestion.suggested_amount}`);
    }
  };

  // Delete modal state
  const [deleteTarget, setDeleteTarget] = useState<{ type: BudgetPeriodType; key: string } | null>(null);

  // Sync loaded budget value to form (only when a budget actually exists in database)
  useEffect(() => {
    if (budget && budget.amount !== null && budget.amount !== undefined) {
      setAmount(budget.amount);
    } else {
      setAmount("");
    }
  }, [budget, periodType, periodKey]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError("Budget amount must be greater than 0");
      return;
    }

    try {
      await upsertBudget({
        amount: parsedAmount,
        period_type: periodType,
        period_key: periodKey,
        month: periodType === "month" ? periodKey : undefined,
      });
      success(`Budget for ${formatPeriodLabel(periodType, periodKey)} saved successfully`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to save budget";
      toastError(msg);
    }
  };

  const handleDelete = async (type: BudgetPeriodType, key: string) => {
    try {
      await deleteBudget({ period_type: type, period_key: key });
      if (type === periodType && key === periodKey) {
        setAmount("");
      }
      setDeleteTarget(null);
      success(`Budget for ${formatPeriodLabel(type, key)} deleted successfully`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to delete budget";
      toastError(msg);
    }
  };

  const isLoading = loadingBudget || loadingDashboard;
  const isExistingBudget = Boolean(budget && budget.amount !== null && budget.amount !== undefined);
  const currentLimit = isExistingBudget && budget?.amount ? parseFloat(budget.amount) : 0;
  const currentSpent = dashboard?.total_spent ? parseFloat(dashboard.total_spent) : 0;
  const isOverBudget = currentLimit > 0 && currentSpent > currentLimit;
  const isNearBudget = currentLimit > 0 && !isOverBudget && currentSpent >= currentLimit * 0.8;
  const pct = currentLimit > 0 ? Math.min((currentSpent / currentLimit) * 100, 100) : 0;

  const filteredBudgets = historyFilter === "all"
    ? allBudgets
    : allBudgets.filter((b) => b.period_type === historyFilter);

  return (
    <div className="page-container space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-glow">Budget Planner</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Configure, track, and manage spending targets for any month, week, or day.
          </p>
        </div>

        {/* Granularity Tabs */}
        <div className="flex bg-zinc-900 border border-border p-1 rounded-lg">
          {(
            [
              { type: "month", label: "Monthly", icon: Calendar },
              { type: "week", label: "Weekly", icon: Clock },
              { type: "day", label: "Daily", icon: Sun },
            ] as const
          ).map((item) => {
            const Icon = item.icon;
            const isSelected = periodType === item.type;
            return (
              <button
                key={item.type}
                onClick={() => handleTypeChange(item.type)}
                className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md transition-all cursor-pointer ${
                  isSelected
                    ? "bg-primary text-primary-foreground shadow"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                <Icon className="h-3.5 w-3.5" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Configuration Form Card */}
        <Card variant="glass" className="md:col-span-1 h-fit">
          <CardHeader>
            <div className="flex items-center space-x-2">
              <PiggyBank className="h-5 w-5 text-primary" />
              <CardTitle className="capitalize">
                {isExistingBudget ? `Edit ${periodType} Budget` : `Set ${periodType} Budget`}
              </CardTitle>
            </div>
            <CardDescription>
              Target: <strong>{formatPeriodLabel(periodType, periodKey)}</strong>
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Dynamic Period Picker */}
              <div className="flex flex-col space-y-1.5 w-full">
                <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Select {periodType}
                </label>
                <input
                  type={periodType === "month" ? "month" : periodType === "week" ? "week" : "date"}
                  value={periodKey}
                  onChange={(e) => {
                    setPeriodKey(e.target.value);
                  }}
                  className="flex h-10 w-full rounded-md border border-input bg-zinc-900/50 px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 cursor-pointer"
                  required
                />
              </div>

              {/* Amount Input with AI Smart Suggestion */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Spending Limit ({currencySymbol}) for {formatPeriodLabel(periodType, periodKey)}
                  </span>
                  <button
                    type="button"
                    onClick={handleFetchAiSuggest}
                    disabled={loadingAiSuggest}
                    className="inline-flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 font-medium cursor-pointer transition disabled:opacity-50"
                  >
                    {loadingAiSuggest ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                    )}
                    <span>AI Smart Suggest</span>
                  </button>
                </div>

                {aiSuggestion && (
                  <div className="rounded-xl border border-indigo-500/30 bg-gradient-to-br from-indigo-500/10 via-purple-500/5 to-transparent p-3 space-y-2 text-xs animate-in fade-in duration-200">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-indigo-300 flex items-center gap-1.5">
                        <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                        Recommended Target:{" "}
                        <strong className="text-white text-sm">
                          {formatCurrency(aiSuggestion.suggested_amount)}
                        </strong>
                      </span>
                      <button
                        type="button"
                        onClick={handleApplyAiSuggest}
                        className="px-2.5 py-1 rounded-md bg-indigo-600 hover:bg-indigo-500 text-white font-medium text-[11px] transition cursor-pointer"
                      >
                        Apply Target
                      </button>
                    </div>
                    <p className="text-zinc-400 leading-relaxed text-[11px]">
                      {aiSuggestion.reasoning}
                    </p>
                    {aiSuggestion.category_allocations?.length > 0 && (
                      <div className="pt-1 flex flex-wrap gap-1.5">
                        {aiSuggestion.category_allocations.map((alloc, idx) => (
                          <span
                            key={idx}
                            className="px-2 py-0.5 rounded-md bg-zinc-900/90 border border-zinc-800 text-[10px] text-zinc-300"
                          >
                            {alloc.category_name}: {formatCurrency(alloc.suggested_amount)} ({alloc.percentage}%)
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                <Input
                  label=""
                  type="number"
                  step="1"
                  min="1"
                  placeholder={periodType === "day" ? "e.g. 50" : periodType === "week" ? "e.g. 350" : "e.g. 1500"}
                  value={amount}
                  onChange={(e) => {
                    setAmount(e.target.value);
                    setError("");
                  }}
                  error={error}
                  required
                />
              </div>

              {/* Slider for quick edits */}
              <div className="space-y-1 pt-1">
                <div className="flex justify-between text-[10px] font-semibold uppercase text-muted-foreground">
                  <span>Quick Adjust</span>
                  <span className="text-foreground">{formatCurrency(amount || 0)}</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max={Math.max(periodType === "day" ? 500 : 5000, parseInt(amount) || 1000)}
                  step={periodType === "day" ? "10" : "50"}
                  value={amount ? parseInt(amount) || 0 : 0}
                  onChange={(e) => {
                    setAmount(e.target.value);
                    setError("");
                  }}
                  className="w-full h-1.5 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-primary"
                />
              </div>

              {/* Action buttons */}
              <div className="flex gap-3 pt-2">
                <button
                  type="submit"
                  disabled={isSaving || isDeleting}
                  className="flex-1 inline-flex items-center justify-center gap-2 h-10 px-4 text-sm font-semibold rounded-lg bg-primary text-primary-foreground hover:bg-indigo-500 transition-all shadow-md shadow-primary/20 disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                >
                  {isSaving ? (
                    <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                  ) : isExistingBudget ? (
                    <Save className="h-4 w-4" />
                  ) : (
                    <PlusCircle className="h-4 w-4" />
                  )}
                  {isExistingBudget ? "Update Budget" : "Save Budget"}
                </button>
                {isExistingBudget && (
                  <button
                    type="button"
                    onClick={() => setDeleteTarget({ type: periodType, key: periodKey })}
                    disabled={isSaving || isDeleting}
                    className="inline-flex items-center justify-center gap-1.5 h-10 px-3 text-sm font-medium rounded-lg border border-destructive/40 text-destructive hover:bg-destructive/10 transition-colors disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                    title="Delete this budget"
                  >
                    <Trash2 className="h-4 w-4" />
                    Delete
                  </button>
                )}
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Current status metrics card */}
        <Card variant="glass" className="md:col-span-2">
          <CardHeader>
            <CardTitle>Spending Status for {formatPeriodLabel(periodType, periodKey)}</CardTitle>
            <CardDescription>Live tracking against expenses in this period</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {isLoading ? (
              <div className="space-y-4 animate-pulse">
                <div className="h-10 bg-zinc-800 rounded" />
                <div className="h-20 bg-zinc-800 rounded" />
              </div>
            ) : !isExistingBudget ? (
              <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground border border-dashed border-border rounded-xl p-6 bg-zinc-900/20">
                <Info className="h-10 w-10 mb-3 opacity-40 text-primary" />
                <p className="text-sm font-semibold text-foreground">
                  No {periodType} budget set for {formatPeriodLabel(periodType, periodKey)}
                </p>
                <p className="text-xs mt-1.5 max-w-sm text-muted-foreground leading-relaxed">
                  Enter your spending target on the left and click <strong>&quot;Save Budget&quot;</strong> to activate real-time budget tracking for this period.
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
                        Exceeded by <strong>${(currentSpent - currentLimit).toFixed(2)}</strong>.
                      </p>
                    </div>
                  </div>
                ) : isNearBudget ? (
                  <div className="flex items-center space-x-3 text-amber-500 bg-amber-500/10 p-4 rounded-lg border border-amber-500/20">
                    <AlertTriangle className="h-6 w-6 flex-shrink-0" />
                    <div>
                      <p className="text-sm font-bold">Approaching limit</p>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        Consumed over 80% of allowance. <strong>${(currentLimit - currentSpent).toFixed(2)}</strong> left.
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center space-x-3 text-emerald-500 bg-emerald-500/10 p-4 rounded-lg border border-emerald-500/20">
                    <CheckCircle className="h-6 w-6 flex-shrink-0" />
                    <div>
                      <p className="text-sm font-bold">Budget on track</p>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        Spending is on track. <strong>${(currentLimit - currentSpent).toFixed(2)}</strong> left.
                      </p>
                    </div>
                  </div>
                )}

                {/* Metrics grid */}
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-6 pt-2">
                  <div className="space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      Target Limit
                    </p>
                    <p className="text-2xl font-extrabold tracking-tight">${currentLimit.toFixed(2)}</p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      Current Spent
                    </p>
                    <p className="text-2xl font-extrabold tracking-tight">${currentSpent.toFixed(2)}</p>
                  </div>
                  <div className="col-span-2 sm:col-span-1 space-y-1">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      Allowance Left
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

      {/* All Configured Budgets Table */}
      <Card variant="glass" className="w-full">
        <CardHeader className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <CardTitle>Configured Budgets</CardTitle>
            <CardDescription>All active spending targets configured by you</CardDescription>
          </div>

          {/* History filter buttons */}
          <div className="flex bg-zinc-900 border border-border p-1 rounded-lg">
            {(["all", "month", "week", "day"] as const).map((filter) => (
              <button
                key={filter}
                onClick={() => setHistoryFilter(filter)}
                className={`px-3 py-1 text-xs font-semibold rounded-md transition-all cursor-pointer capitalize ${
                  historyFilter === filter
                    ? "bg-primary text-primary-foreground shadow"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {filter === "all" ? "All Targets" : `${filter}ly`}
              </button>
            ))}
          </div>
        </CardHeader>
        <CardContent>
          {loadingAllBudgets ? (
            <div className="space-y-2 animate-pulse">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="h-12 bg-zinc-800 rounded" />
              ))}
            </div>
          ) : filteredBudgets.length === 0 ? (
            <div className="py-10 text-center text-muted-foreground text-sm">
              <PiggyBank className="h-8 w-8 mx-auto mb-2 opacity-30 text-primary" />
              <p className="font-semibold text-foreground">No budgets configured yet</p>
              <p className="text-xs mt-1">Select a period above and save your first budget to see it listed here.</p>
            </div>
          ) : (
            <div className="divide-y divide-border">
              {filteredBudgets.map((b) => {
                const bType = (b.period_type || "month") as BudgetPeriodType;
                const bKey = b.period_key || b.month || "";
                return (
                  <div
                    key={b.id || `${bType}-${bKey}`}
                    className="flex items-center justify-between py-3.5 first:pt-0 last:pb-0"
                  >
                    <div className="space-y-0.5">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-sm text-foreground">
                          {formatPeriodLabel(bType, bKey)}
                        </span>
                        <span
                          className={`text-[10px] px-1.5 py-0.5 rounded font-bold uppercase ${
                            bType === "day"
                              ? "bg-amber-500/20 text-amber-400"
                              : bType === "week"
                              ? "bg-sky-500/20 text-sky-400"
                              : "bg-primary/20 text-primary"
                          }`}
                        >
                          {bType}
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground font-mono">
                        Key: {bKey}
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
                            setPeriodType(bType);
                            setPeriodKey(bKey);
                          }}
                          className="text-muted-foreground hover:text-foreground hover:bg-secondary cursor-pointer h-8 w-8 p-0"
                          title="Edit this budget"
                        >
                          <Edit2 className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setDeleteTarget({ type: bType, key: bKey })}
                          className="text-destructive hover:bg-destructive/10 hover:text-red-500 cursor-pointer h-8 w-8 p-0"
                          title="Delete this budget"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Delete Confirmation Dialog */}
      <Dialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="Delete Budget"
      >
        <div className="space-y-4">
          <div className="flex items-center space-x-3 text-destructive bg-destructive/10 p-3 rounded-lg border border-destructive/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <p className="text-xs font-medium">
              This will remove the {deleteTarget?.type} spending target for{" "}
              <strong>{deleteTarget ? formatPeriodLabel(deleteTarget.type, deleteTarget.key) : ""}</strong>.
            </p>
          </div>
          <p className="text-sm text-foreground">
            Are you sure you want to delete this budget configuration?
          </p>
          <div className="flex space-x-3 pt-2">
            <Button
              variant="secondary"
              className="flex-1 cursor-pointer"
              onClick={() => setDeleteTarget(null)}
              disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              className="flex-1 cursor-pointer"
              onClick={() => deleteTarget && handleDelete(deleteTarget.type, deleteTarget.key)}
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
