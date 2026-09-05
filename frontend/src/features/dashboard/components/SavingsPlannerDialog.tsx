"use client";

import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Target,
  Sparkles,
  Calendar,
  DollarSign,
  TrendingDown,
  CheckCircle2,
  AlertCircle,
  Loader2,
  X,
  ArrowRight,
  ShieldCheck,
} from "lucide-react";
import { aiApi, SavingsPlanResponse } from "@/lib/api/ai";
import { useCurrency } from "@/features/auth/context/CurrencyContext";

interface SavingsPlannerDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SavingsPlannerDialog: React.FC<SavingsPlannerDialogProps> = ({
  isOpen,
  onClose,
}) => {
  const { formatCurrency, currencySymbol } = useCurrency();
  const [goalName, setGoalName] = useState("Emergency Fund");
  const [targetAmount, setTargetAmount] = useState("25000");
  const [targetMonths, setTargetMonths] = useState("6");
  const [planResult, setPlanResult] = useState<SavingsPlanResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGeneratePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    const amt = parseFloat(targetAmount);
    const months = parseInt(targetMonths, 10);

    if (isNaN(amt) || amt <= 0) {
      setError("Please enter a valid positive target savings amount.");
      return;
    }
    if (isNaN(months) || months < 1 || months > 120) {
      setError("Timeframe must be between 1 and 120 months.");
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      const res = await aiApi.createSavingsPlan({
        goal_name: goalName.trim() || "Savings Goal",
        target_amount: amt,
        target_months: months,
      });
      setPlanResult(res.data);
    } catch {
      setError("Could not generate savings plan. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const getFeasibilityBadge = (feasibility: string) => {
    switch (feasibility) {
      case "highly_achievable":
        return {
          label: "Highly Achievable",
          color: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
        };
      case "achievable":
        return {
          label: "Achievable",
          color: "bg-indigo-500/15 text-indigo-400 border-indigo-500/30",
        };
      case "challenging":
        return {
          label: "Challenging",
          color: "bg-amber-500/15 text-amber-400 border-amber-500/30",
        };
      case "unrealistic":
      default:
        return {
          label: "Aggressive Goal",
          color: "bg-rose-500/15 text-rose-400 border-rose-500/30",
        };
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-xs">
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 10 }}
        className="relative w-full max-w-lg rounded-2xl border border-indigo-500/30 bg-zinc-950 p-5 sm:p-6 shadow-2xl overflow-hidden max-h-[90vh] flex flex-col"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-zinc-800">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/15 text-emerald-400">
              <Target className="h-4 w-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-zinc-100">Goal-Based Savings Planner</h3>
              <p className="text-[11px] text-zinc-400">Target your milestone with AI category optimization</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 text-zinc-400 hover:text-zinc-200 rounded-lg hover:bg-zinc-800 transition cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Scrollable Body */}
        <div className="flex-1 overflow-y-auto pt-4 space-y-4 pr-1">
          {/* Form */}
          <form onSubmit={handleGeneratePlan} className="space-y-3 bg-zinc-900/50 p-3.5 rounded-xl border border-zinc-800/80">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              <div>
                <label className="text-[11px] font-medium text-zinc-300 block mb-1">Goal Name</label>
                <input
                  type="text"
                  value={goalName}
                  onChange={(e) => setGoalName(e.target.value)}
                  placeholder="e.g. New Laptop"
                  className="w-full rounded-lg border border-zinc-800 bg-zinc-950 px-2.5 py-1.5 text-xs text-zinc-100 focus:outline-hidden focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="text-[11px] font-medium text-zinc-300 block mb-1">Target Amount ({currencySymbol})</label>
                <input
                  type="number"
                  value={targetAmount}
                  onChange={(e) => setTargetAmount(e.target.value)}
                  placeholder="e.g. 25000"
                  className="w-full rounded-lg border border-zinc-800 bg-zinc-950 px-2.5 py-1.5 text-xs text-zinc-100 focus:outline-hidden focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="text-[11px] font-medium text-zinc-300 block mb-1">Timeframe (Months)</label>
                <input
                  type="number"
                  min="1"
                  max="120"
                  value={targetMonths}
                  onChange={(e) => setTargetMonths(e.target.value)}
                  className="w-full rounded-lg border border-zinc-800 bg-zinc-950 px-2.5 py-1.5 text-xs text-zinc-100 focus:outline-hidden focus:border-indigo-500"
                />
              </div>
            </div>

            {error && <p className="text-xs text-rose-400">{error}</p>}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full mt-2 flex items-center justify-center gap-2 rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-500 transition cursor-pointer disabled:opacity-50"
            >
              {isLoading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Sparkles className="h-3.5 w-3.5" />}
              <span>{isLoading ? "Analyzing Trimmable Categories..." : "Calculate Savings Roadmap"}</span>
            </button>
          </form>

          {/* Result Card */}
          {planResult && (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="space-y-3.5 pt-1"
            >
              {/* Top summary banner */}
              <div className="rounded-xl border border-indigo-500/25 bg-gradient-to-r from-indigo-950/40 via-purple-950/20 to-zinc-900 p-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-indigo-300">Required Monthly Savings</span>
                  {(() => {
                    const badge = getFeasibilityBadge(planResult.feasibility);
                    return (
                      <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded-full border ${badge.color}`}>
                        {badge.label}
                      </span>
                    );
                  })()}
                </div>
                <p className="text-2xl font-black text-foreground mt-1">
                  {formatCurrency(planResult.required_monthly_savings)}
                  <span className="text-xs text-muted-foreground font-normal"> / month for {planResult.target_months} months</span>
                </p>
                <p className="text-[11px] text-zinc-400 mt-1">
                  To reach your target of <strong className="text-zinc-200">{formatCurrency(planResult.target_amount)}</strong> for {planResult.goal_name}.
                </p>
              </div>

              {/* Category Cuts Table */}
              {planResult.category_cuts.length > 0 && (
                <div className="rounded-xl border border-zinc-800 bg-zinc-900/40 p-3.5 space-y-2">
                  <h4 className="text-xs font-bold text-zinc-200 flex items-center gap-1.5">
                    <TrendingDown className="h-3.5 w-3.5 text-amber-400" />
                    Recommended Category Spending Cuts
                  </h4>
                  <div className="space-y-2 pt-1">
                    {planResult.category_cuts.map((cut, idx) => (
                      <div key={idx} className="rounded-lg bg-zinc-950/60 border border-zinc-800/60 p-2.5 text-xs">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-zinc-200">{cut.category_name}</span>
                          <span className="text-amber-400 font-bold">-{cut.cut_percentage}%</span>
                        </div>
                        <div className="flex items-center justify-between text-[11px] text-zinc-400 mt-1">
                          <span>Current: {formatCurrency(cut.current_monthly_spend)}/mo</span>
                          <ArrowRight className="h-3 w-3 text-zinc-600" />
                          <span className="text-emerald-400 font-medium">Target: {formatCurrency(cut.suggested_monthly_spend)}/mo</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Action Steps */}
              {planResult.action_steps.length > 0 && (
                <div className="rounded-xl border border-zinc-800/80 bg-zinc-900/30 p-3.5 space-y-2">
                  <h4 className="text-xs font-bold text-zinc-200 flex items-center gap-1.5">
                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" />
                    Action Roadmap
                  </h4>
                  <ul className="space-y-1.5 pt-0.5">
                    {planResult.action_steps.map((step, idx) => (
                      <li key={idx} className="flex items-start gap-2 text-xs text-zinc-300">
                        <span className="flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 text-[10px] font-bold">
                          {idx + 1}
                        </span>
                        <span>{step}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </motion.div>
          )}
        </div>
      </motion.div>
    </div>
  );
};
