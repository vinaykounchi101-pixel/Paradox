"use client";

import React, { useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import {
  TrendingUp,
  Receipt,
  AlertTriangle,
  Plus,
  ArrowRight,
  TrendingDown,
  Percent,
  Tags,
} from "lucide-react";
import { useDashboard } from "../hooks/useDashboard";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { BarChart } from "./BarChart";
import { TrendGraph } from "./TrendGraph";

export default function DashboardView() {
  const [period, setPeriod] = useState<"current_month" | "last_30_days" | "current_week">("current_month");
  const { data, isLoading, isError, error } = useDashboard(period);

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
            Real-time insights on your monthly spending.
          </p>
        </div>

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

      {/* Top row cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Spent card */}
        <Card variant="glass" className="relative overflow-hidden group">
          <div className="absolute right-0 top-0 translate-x-4 -translate-y-4 h-24 w-24 bg-primary/10 rounded-full blur-2xl group-hover:bg-primary/20 transition-colors" />
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Total Spent
            </CardTitle>
            <TrendingDown className="h-4 w-4 text-emerald-500" />
          </CardHeader>
          <CardContent className="pt-2">
            <div className="text-4xl font-extrabold tracking-tight">${total_spent}</div>
            <p className="text-xs text-muted-foreground mt-2">
              For the selected {period.replace("_", " ")}
            </p>
          </CardContent>
        </Card>

        {/* Budget Status card */}
        <Card
          variant="glass"
          className={`relative overflow-hidden ${
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
                  <span className="text-3xl font-extrabold tracking-tight">${spent.toFixed(2)}</span>
                  <span className="text-sm text-muted-foreground">of ${limit.toFixed(2)} limit</span>
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
                      ? `Budget Exceeded by $${(spent - limit).toFixed(2)}`
                      : isNearBudget
                      ? `Approaching Limit (${pct.toFixed(0)}% spent)`
                      : `On Track (${pct.toFixed(0)}% spent)`}
                  </span>
                  <span className="text-muted-foreground">
                    ${(limit - spent > 0 ? limit - spent : 0).toFixed(2)} left
                  </span>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

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

        {/* Category Breakdown Bar Chart (1 column) */}
        <Card variant="glass" className="lg:col-span-1">
          <CardHeader>
            <div className="flex items-center space-x-2">
              <Tags className="h-5 w-5 text-primary" />
              <CardTitle>Spending Categories</CardTitle>
            </div>
            <CardDescription>Breakdown by category totals</CardDescription>
          </CardHeader>
          <CardContent>
            <BarChart data={category_breakdown} />
          </CardContent>
        </Card>
      </div>

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
                      <p className="font-semibold text-sm truncate">{e.description || "Unspecified Expense"}</p>
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
                      -${e.amount}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
