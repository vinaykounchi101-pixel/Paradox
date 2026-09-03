"use client";

import React, { useState } from "react";
import Link from "next/link";
import { motion, AnimatePresence } from "framer-motion";
import {
  TrendingUp,
  Receipt,
  AlertTriangle,
  Plus,
  ArrowRight,
  TrendingDown,
  Percent,
  Tags,
  FileText,
  Repeat,
  Printer,
  Sparkles,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useCurrency } from "@/features/auth/context/CurrencyContext";
import { expensesApi } from "@/lib/api/expenses";
import { useDashboard } from "../hooks/useDashboard";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { BarChart3D } from "./BarChart3D";
import { TrendGraph } from "./TrendGraph";
import { TiltCard } from "@/components/common/TiltCard";
import { FinancialCopilotCard } from "./FinancialCopilotCard";
import { PurchaseSimulatorDialog } from "./PurchaseSimulatorDialog";
import { SafeToSpendCard } from "./SafeToSpendCard";
import { LeakHunterDialog } from "./LeakHunterDialog";
import { SubscriptionAuditDialog } from "./SubscriptionAuditDialog";
import { ExpenseFormDialog } from "@/features/expenses/components/ExpenseFormDialog";
import { Search } from "lucide-react";

export default function DashboardView() {
  const { formatCurrency, currency, currencySymbol } = useCurrency();
  const [period, setPeriod] = useState<"current_month" | "last_30_days" | "current_week">("current_month");
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [isSimulatorOpen, setIsSimulatorOpen] = useState(false);
  const [isLeakHunterOpen, setIsLeakHunterOpen] = useState(false);
  const [isSubAuditOpen, setIsSubAuditOpen] = useState(false);
  const [isExpenseDialogOpen, setIsExpenseDialogOpen] = useState(false);

  const handleSimulatorAddExpense = () => {
    setIsExpenseDialogOpen(true);
  };
  const { data, isLoading, isError, error } = useDashboard(period);
  const { data: recurringData } = useQuery({
    queryKey: ["recurring_expenses"],
    queryFn: expensesApi.getRecurring,
  });
  const recurringTotal = recurringData?.data?.total_monthly_commitment || "0.00";
  const recurringList = recurringData?.data?.recurring_expenses || [];

  if (isLoading) {
    return (
      <div className="page-container flex flex-col space-y-6 animate-pulse">
        <div className="flex justify-between items-center">
          <div className="h-8 w-48 bg-zinc-800 rounded" />
          <div className="h-10 w-32 bg-zinc-800 rounded" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="h-44 bg-zinc-800 rounded-lg" />
          <div className="h-44 bg-zinc-800 rounded-lg" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-80 bg-zinc-800 rounded-lg" />
          <div className="h-80 bg-zinc-800 rounded-lg" />
        </div>
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="page-container flex flex-col items-center justify-center min-h-[60vh] space-y-4">
        <AlertTriangle className="h-12 w-12 text-destructive" />
        <h3 className="text-xl font-bold">Failed to load dashboard</h3>
        <p className="text-sm text-muted-foreground">
          {error instanceof Error ? error.message : "Ensure the backend is running at http://127.0.0.1:8000"}
        </p>
        <Button onClick={() => window.location.reload()}>Retry</Button>
      </div>
    );
  }

  const { total_spent, budget, category_breakdown, recent_expenses } = data;

  // Derive budget percentages and styles
  const limit = budget.amount ? parseFloat(budget.amount) : null;
  const spent = parseFloat(budget.spent);
  const isOverBudget = limit !== null && spent > limit;
  const isNearBudget = limit !== null && !isOverBudget && spent >= limit * 0.8;
  const pct = limit ? Math.min((spent / limit) * 100, 100) : 0;

  return (
    <div className="page-container space-y-6">
      {/* Welcome Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-glow">Dashboard</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Real-time insights on your financial health.
          </p>
        </div>

        <div className="flex items-center gap-3 flex-wrap">
          {/* Can I Afford This? Simulator Button */}
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsSimulatorOpen(true)}
            className="cursor-pointer text-xs bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30"
            title="Simulate a planned purchase before spending"
          >
            <Sparkles className="h-3.5 w-3.5 mr-1.5 text-amber-400" />
            Can I Afford This?
          </Button>

          {/* Leak Hunter Button */}
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsLeakHunterOpen(true)}
            className="cursor-pointer text-xs bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/30"
            title="Detect micro-spending drains and leaks"
          >
            <Search className="h-3.5 w-3.5 mr-1.5 text-rose-400" />
            Leak Hunter
          </Button>

          {/* Monthly Health Report Button */}
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsReportModalOpen(true)}
            className="cursor-pointer text-xs"
            title="Generate monthly financial health report"
          >
            <FileText className="h-3.5 w-3.5 mr-1.5 text-indigo-400" />
            Health Report
          </Button>

          {/* Period selection tabs */}
          <div className="flex bg-zinc-900 border border-border p-1 rounded-lg">
            {(["current_month", "last_30_days", "current_week"] as const).map((p) => (
              <button
                key={p}
                onClick={() => setPeriod(p)}
                className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all cursor-pointer ${
                  period === p
                    ? "bg-primary text-primary-foreground shadow"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {p === "current_month" && "Current Month"}
                {p === "last_30_days" && "Last 30 Days"}
                {p === "current_week" && "Current Week"}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* AI Financial Copilot Insights Card */}
      <FinancialCopilotCard period={period} />

      {/* Top row cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:items-stretch">
        {/* Spent card */}
        <TiltCard className="h-full">
          <Card variant="glass" className="relative overflow-hidden group h-full flex flex-col">
            <div className="absolute right-0 top-0 translate-x-4 -translate-y-4 h-24 w-24 bg-primary/10 rounded-full blur-2xl group-hover:bg-primary/20 transition-colors" />
            <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
              <CardTitle className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Total Spent
              </CardTitle>
              <TrendingDown className="h-4 w-4 text-emerald-500" />
            </CardHeader>
            <CardContent className="pt-2 flex flex-col flex-1">
              {/* 3D flip-counter on value change */}
              <div className="text-4xl font-extrabold tracking-tight overflow-hidden h-12 flex items-center">
                <AnimatePresence mode="wait" initial={false}>
                  <motion.span
                    key={total_spent + period}
                    initial={{ rotateX: -90, opacity: 0, y: 20 }}
                    animate={{ rotateX: 0, opacity: 1, y: 0 }}
                    exit={{ rotateX: 90, opacity: 0, y: -20 }}
                    transition={{ duration: 0.35, ease: "easeOut" }}
                    style={{ display: "inline-block", transformStyle: "preserve-3d" }}
                  >
                    {formatCurrency(total_spent)}
                  </motion.span>
                </AnimatePresence>
              </div>
              <p className="text-xs text-muted-foreground mt-auto pt-4">
                For the selected {period.replace("_", " ")}
              </p>
            </CardContent>
          </Card>
        </TiltCard>

        {/* Budget Status card */}
        <TiltCard className="h-full">
          <Card
            variant="glass"
            className={`relative overflow-hidden h-full ${
              isOverBudget
                ? "border-destructive/30 shadow-[0_0_15px_rgba(239,68,68,0.1)]"
                : isNearBudget
                ? "border-amber-500/30"
                : ""
            }`}
          >
            <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
              <CardTitle className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Monthly Budget
              </CardTitle>
              {limit === null ? (
                <Percent className="h-4 w-4 text-muted-foreground" />
              ) : isOverBudget ? (
                <AlertTriangle className="h-4 w-4 text-destructive" />
              ) : (
                <TrendingUp className="h-4 w-4 text-primary" />
              )}
            </CardHeader>
            <CardContent className="pt-2">
              {limit === null ? (
                <div className="space-y-3">
                  <div className="text-lg font-bold text-muted-foreground">No budget set</div>
                  <Link href="/budget">
                    <Button size="sm" variant="secondary" className="cursor-pointer">
                      Set Budget
                    </Button>
                  </Link>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="flex items-baseline justify-between">
                    <span className="text-3xl font-extrabold tracking-tight">{formatCurrency(spent)}</span>
                    <span className="text-sm text-muted-foreground">of {formatCurrency(limit)} limit</span>
                  </div>
                  
                  {/* Progress bar */}
                  <div className="h-2 w-full bg-zinc-800 rounded-full overflow-hidden">
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

                  <div className="flex items-center justify-between text-xs">
                    <span
                      className={
                        isOverBudget
                          ? "text-destructive font-semibold"
                          : isNearBudget
                          ? "text-amber-500 font-semibold"
                          : "text-emerald-500 font-semibold"
                      }
                    >
                      {isOverBudget
                        ? `Budget Exceeded by ${formatCurrency(spent - limit)}`
                        : isNearBudget
                        ? `Approaching Limit (${pct.toFixed(0)}% spent)`
                        : `On Track (${pct.toFixed(0)}% spent)`}
                    </span>
                    <span className="text-muted-foreground">
                      {formatCurrency(limit - spent > 0 ? limit - spent : 0)} left
                    </span>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </TiltCard>
      </div>

      {/* Safe-to-Spend Speedometer Widget */}
      <SafeToSpendCard onOpenSimulator={() => setIsSimulatorOpen(true)} />

      {/* Grid: Analytics Section (Trend Curve & Donut Chart) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Trend Graph (2 columns) */}
        <Card variant="glass" className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center space-x-2">
              <TrendingUp className="h-5 w-5 text-primary" />
              <CardTitle>Spending Trends</CardTitle>
            </div>
            <CardDescription>Visual review of weekly transaction aggregates</CardDescription>
          </CardHeader>
          <CardContent>
            <TrendGraph data={data.trend} />
          </CardContent>
        </Card>

        {/* Category Breakdown 3D Bar Chart (1 column) */}
        <Card variant="glass" className="lg:col-span-1">
          <CardHeader>
            <div className="flex items-center space-x-2">
              <Tags className="h-5 w-5 text-primary" />
              <CardTitle>Spending Categories</CardTitle>
            </div>
            <CardDescription>Breakdown by category totals</CardDescription>
          </CardHeader>
          <CardContent>
            <BarChart3D data={category_breakdown} />
          </CardContent>
        </Card>
      </div>

      {/* Subscriptions & Fixed Commitments Widget */}
      <Card variant="glass" className="p-5">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <Repeat className="h-4 w-4" />
            </div>
            <div>
              <CardTitle className="text-base">Subscriptions & Fixed Bills</CardTitle>
              <CardDescription>Committed recurring expenses automatically tracked</CardDescription>
            </div>
          </div>
          <div className="flex items-center gap-2.5">
            <span className="text-xs text-muted-foreground">Monthly Fixed Burden:</span>
            <span className="text-lg font-bold text-foreground font-mono">{formatCurrency(recurringTotal)}</span>
            <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 font-semibold border border-indigo-500/30">
              {recurringList.length} Active
            </span>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setIsSubAuditOpen(true)}
              className="text-xs h-7 gap-1 text-purple-300 border-purple-500/30 hover:bg-purple-500/10 ml-1 cursor-pointer"
              title="Audit subscriptions and recurring commitments"
            >
              <Sparkles className="w-3 h-3 text-purple-400" /> Audit
            </Button>
          </div>
        </div>

        {recurringList.length > 0 ? (
          <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 pt-3 border-t border-border/50">
            {recurringList.map((r) => (
              <div key={r.id} className="p-3 rounded-xl bg-zinc-900/60 border border-border flex items-center justify-between">
                <div className="min-w-0 pr-2">
                  <p className="text-xs font-semibold text-foreground truncate">{r.description || "Recurring Bill"}</p>
                  <p className="text-[10px] text-muted-foreground capitalize">{r.recurring_frequency || "monthly"} • {r.category?.name || "General"}</p>
                </div>
                <div className="font-mono text-xs font-bold text-foreground shrink-0">
                  {formatCurrency(r.amount)}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="mt-3 text-xs text-muted-foreground">
            No recurring subscriptions marked yet. When recording an expense, toggle "🔁 Recurring Subscription" to track your recurring burdens here.
          </p>
        )}
      </Card>

      {/* Grid: Latest Transactions */}
      <div className="grid grid-cols-1 gap-6">
        {/* Recent expenses */}
        <Card variant="glass" className="w-full">
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle>Recent Expenses</CardTitle>
              <CardDescription>Your latest transactions</CardDescription>
            </div>
            <Link href="/expenses">
              <Button size="sm" variant="ghost" className="group text-xs cursor-pointer">
                View All
                <ArrowRight className="h-3.5 w-3.5 ml-1 transition-transform group-hover:translate-x-0.5" />
              </Button>
            </Link>
          </CardHeader>
          <CardContent>
            {recent_expenses.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
                <Receipt className="h-8 w-8 mb-2 opacity-50" />
                <p className="text-sm">No recent expenses found</p>
                <Link href="/expenses" className="mt-4">
                  <Button size="sm" className="cursor-pointer">
                    <Plus className="h-4 w-4 mr-1" /> Add Expense
                  </Button>
                </Link>
              </div>
            ) : (
              <div className="divide-y divide-border">
                {recent_expenses.map((e) => (
                  <div key={e.id} className="flex justify-between items-center py-3.5 first:pt-0 last:pb-0">
                    <div className="space-y-1 pr-4 min-w-0">
                      <p className="font-semibold text-sm truncate flex items-center gap-1.5">
                        <span>{e.description || "Unspecified Expense"}</span>
                        {e.is_recurring && (
                          <span className="inline-flex items-center px-1.5 py-0.2 rounded text-[9px] font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                            🔁 {e.recurring_frequency || "monthly"}
                          </span>
                        )}
                      </p>
                      <div className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                        <span className="bg-zinc-800/80 px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase text-zinc-300">
                          {e.category?.name || "Uncategorized"}
                        </span>
                        <span className="text-[10px] bg-zinc-800/80 px-1.5 py-0.5 rounded text-zinc-300">
                          {e.payment_method?.name || "Other"}
                        </span>
                        <span>•</span>
                        <span>{new Date(e.date).toLocaleDateString(undefined, { month: "short", day: "numeric" })}</span>
                      </div>
                    </div>
                    <div className="font-mono text-sm font-bold text-foreground shrink-0">
                      -{formatCurrency(e.amount)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Monthly Financial Health Report Modal */}
      <Dialog
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        title="Monthly Financial Health Report"
      >
        <div className="space-y-4 print:p-0">
          <div className="flex items-center justify-between pb-3 border-b border-border">
            <div>
              <p className="text-xs font-semibold text-muted-foreground">Statement Period</p>
              <p className="text-sm font-bold text-foreground capitalize">{period.replace("_", " ")}</p>
            </div>
            <Button
              size="sm"
              variant="secondary"
              onClick={() => window.print()}
              className="cursor-pointer text-xs"
            >
              <Printer className="h-3.5 w-3.5 mr-1.5 text-indigo-400" />
              Print / Save PDF
            </Button>
          </div>

          {/* Key Metrics Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div className="p-3 rounded-xl bg-zinc-900/70 border border-border">
              <p className="text-[11px] text-muted-foreground font-medium">Total Spend</p>
              <p className="text-lg font-bold text-foreground mt-0.5">{formatCurrency(total_spent)}</p>
            </div>

            <div className="p-3 rounded-xl bg-zinc-900/70 border border-border">
              <p className="text-[11px] text-muted-foreground font-medium">Budget Target</p>
              <p className="text-lg font-bold text-foreground mt-0.5">
                {limit !== null ? formatCurrency(limit) : "None"}
              </p>
            </div>

            <div className="p-3 rounded-xl bg-zinc-900/70 border border-border col-span-2 sm:col-span-1">
              <p className="text-[11px] text-muted-foreground font-medium">Monthly Commitments</p>
              <p className="text-lg font-bold text-indigo-400 mt-0.5">{formatCurrency(recurringTotal)}</p>
            </div>
          </div>

          {/* Category Breakdown Table in Report */}
          <div className="space-y-2">
            <p className="text-xs font-semibold text-foreground uppercase tracking-wider">Top Spending Categories</p>
            <div className="divide-y divide-border/60 border border-border rounded-xl overflow-hidden bg-zinc-900/40">
              {category_breakdown.slice(0, 5).map((c) => (
                <div key={c.category_name} className="flex justify-between items-center px-3 py-2 text-xs">
                  <span className="font-medium text-foreground">{c.category_name}</span>
                  <div className="flex items-center gap-2 font-mono">
                    <span className="font-bold text-foreground">{formatCurrency(c.total)}</span>
                    <span className="text-[10px] text-muted-foreground">({c.percentage}%)</span>
                  </div>
                </div>
              ))}
              {category_breakdown.length === 0 && (
                <p className="p-3 text-xs text-muted-foreground text-center">No categories recorded yet</p>
              )}
            </div>
          </div>

          {/* Health Assessment Summary */}
          <div className="p-3.5 rounded-xl bg-indigo-500/10 border border-indigo-500/25 space-y-1">
            <div className="flex items-center gap-1.5 text-xs font-bold text-indigo-300">
              <Sparkles className="h-3.5 w-3.5 text-indigo-400" />
              Financial Health Summary
            </div>
            <p className="text-xs text-zinc-300 leading-relaxed">
              {isOverBudget
                ? `You have exceeded your budgeted monthly spending target. Consider reviewing non-essential spending in your top categories.`
                : isNearBudget
                ? `You have utilized ${pct.toFixed(1)}% of your monthly budget. Monitor remaining discretionary purchases carefully.`
                : `Your finances are in excellent standing with healthy savings headroom remaining.`}
            </p>
          </div>

          <div className="flex justify-end pt-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setIsReportModalOpen(false)}
              className="cursor-pointer"
            >
              Close
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Can I Afford This? Simulator Dialog */}
      <PurchaseSimulatorDialog
        isOpen={isSimulatorOpen}
        onClose={() => setIsSimulatorOpen(false)}
        onAddAsExpense={handleSimulatorAddExpense}
      />

      {/* Micro-Spending Leak Hunter Dialog */}
      <LeakHunterDialog
        isOpen={isLeakHunterOpen}
        onClose={() => setIsLeakHunterOpen(false)}
      />

      {/* Subscription & Recurring Commitments Audit Dialog */}
      <SubscriptionAuditDialog
        isOpen={isSubAuditOpen}
        onClose={() => setIsSubAuditOpen(false)}
      />

      {/* Expense Form Dialog */}
      <ExpenseFormDialog
        isOpen={isExpenseDialogOpen}
        onClose={() => setIsExpenseDialogOpen(false)}
      />
    </div>
  );
}
