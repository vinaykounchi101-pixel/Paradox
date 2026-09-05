"use client";

import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Target,
  Sparkles,
  Calendar,
  TrendingDown,
  CheckCircle2,
  AlertCircle,
  Loader2,
  X,
  ArrowRight,
  ShieldCheck,
  Zap,
  Check,
} from "lucide-react";
import { aiApi, SavingsPlanResponse } from "@/lib/api/ai";
import { useCurrency } from "@/features/auth/context/CurrencyContext";

interface SavingsPlannerDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

const PRESET_GOALS = [
  { label: "🏖️ Vacation Trip", name: "Vacation Trip", amount: "25000", months: "3" },
  { label: "💻 Tech Upgrade", name: "New Laptop", amount: "75000", months: "6" },
  { label: "🛡️ Emergency Fund", name: "Emergency Buffer", amount: "50000", months: "6" },
  { label: "🛵 Vehicle Fund", name: "Vehicle Downpayment", amount: "60000", months: "8" },
];

export const SavingsPlannerDialog: React.FC<SavingsPlannerDialogProps> = ({
  isOpen,
  onClose,
}) => {
  const { formatCurrency, currencySymbol } = useCurrency();
  const [goalName, setGoalName] = useState("Vacation Trip");
  const [targetAmount, setTargetAmount] = useState("25000");
  const [targetMonths, setTargetMonths] = useState("3");
  const [planResult, setPlanResult] = useState<SavingsPlanResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [appliedBudgets, setAppliedBudgets] = useState(false);

  const handleApplyPreset = (preset: typeof PRESET_GOALS[0]) => {
    setGoalName(preset.name);
    setTargetAmount(preset.amount);
    setTargetMonths(preset.months);
    setPlanResult(null);
    setAppliedBudgets(false);
  };

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
    setAppliedBudgets(false);

    try {
      const res = await aiApi.createSavingsPlan({
        goal_name: goalName.trim() || "Savings Goal",
        target_amount: amt,
        target_months: months,
      });
      setPlanResult(res.data);
    } catch {
      setError("Could not generate savings plan. Please verify your connection and try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleApplyBudgetTargets = () => {
    setAppliedBudgets(true);
  };

  const getFeasibilityBadge = (feasibility: string) => {
    switch (feasibility) {
      case "highly_achievable":
        return {
          label: "Highly Achievable 🎯",
          color: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
        };
      case "achievable":
        return {
          label: "Achievable ✨",
          color: "bg-indigo-500/15 text-indigo-400 border-indigo-500/30",
        };
      case "challenging":
        return {
          label: "Challenging ⚡",
          color: "bg-amber-500/15 text-amber-400 border-amber-500/30",
        };
      case "unrealistic":
      default:
        return {
          label: "Aggressive Target 🔥",
          color: "bg-rose-500/15 text-rose-400 border-rose-500/30",
        };
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/75 backdrop-blur-md">
      <motion.div
        initial={{ opacity: 0, scale: 0.94, y: 15 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.94, y: 15 }}
        transition={{ type: "spring", damping: 25, stiffness: 320 }}
        className="relative w-full max-w-lg rounded-3xl border border-indigo-500/30 bg-zinc-950 p-5 sm:p-6 shadow-2xl overflow-hidden max-h-[90dvh] flex flex-col"
        style={{
          boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.7), 0 0 35px rgba(99, 102, 241, 0.15)",
        }}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-zinc-800/80">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
              <Target className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-bold text-zinc-100 flex items-center gap-1.5">
                <span>Goal-Based Savings Planner</span>
                <span className="text-xs">🎯</span>
              </h3>
              <p className="text-[11px] text-zinc-400">Target milestones with AI category cut optimization</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 text-zinc-400 hover:text-zinc-200 rounded-lg hover:bg-zinc-800/80 transition cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Scrollable Body */}
        <div className="flex-1 overflow-y-auto pt-3.5 space-y-4 pr-1">
          {/* Quick Preset Chips */}
          <div className="space-y-1.5">
            <span className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">Quick Presets</span>
            <div className="flex flex-wrap gap-1.5">
              {PRESET_GOALS.map((preset) => (
                <motion.button
                  key={preset.name}
                  type="button"
                  whileHover={{ scale: 1.03 }}
                  whileTap={{ scale: 0.97 }}
                  onClick={() => handleApplyPreset(preset)}
                  className={`text-[11px] px-2.5 py-1 rounded-full border transition cursor-pointer font-medium ${
                    goalName === preset.name
                      ? "bg-indigo-500/25 border-indigo-500/60 text-indigo-200"
                      : "bg-zinc-900 border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:border-zinc-700"
                  }`}
                >
                  {preset.label}
                </motion.button>
              ))}
            </div>
          </div>

          {/* Input Form */}
          <form onSubmit={handleGeneratePlan} className="space-y-3 bg-zinc-900/60 p-3.5 sm:p-4 rounded-2xl border border-zinc-800/90">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              <div>
                <label className="text-[11px] font-medium text-zinc-300 block mb-1">Goal Name</label>
                <input
                  type="text"
                  value={goalName}
                  onChange={(e) => setGoalName(e.target.value)}
                  placeholder="e.g. Goa Trip"
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-100 focus:outline-hidden focus:border-indigo-500 transition"
                />
              </div>

              <div>
                <label className="text-[11px] font-medium text-zinc-300 block mb-1">Target Amount ({currencySymbol})</label>
                <input
                  type="number"
                  value={targetAmount}
                  onChange={(e) => setTargetAmount(e.target.value)}
                  placeholder="e.g. 25000"
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-100 focus:outline-hidden focus:border-indigo-500 transition font-mono"
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
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-100 focus:outline-hidden focus:border-indigo-500 transition font-mono"
                />
              </div>
            </div>

            {error && <p className="text-xs text-rose-400 bg-rose-500/10 p-2 rounded-lg border border-rose-500/20">{error}</p>}

            <motion.button
              type="submit"
              whileHover={{ scale: 1.01 }}
              whileTap={{ scale: 0.99 }}
              disabled={isLoading}
              className="w-full mt-2 flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-4 py-2.5 text-xs font-bold text-white hover:from-emerald-500 hover:to-teal-500 transition cursor-pointer disabled:opacity-50 shadow-md shadow-emerald-950"
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Analyzing Spend & Computing Cuts...</span>
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4" />
                  <span>Calculate Milestone Roadmap</span>
                </>
              )}
            </motion.button>
          </form>

          {/* Result Card */}
          {planResult && (
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
              className="space-y-3.5 pt-1"
            >
              {/* Top summary banner with 3D Depth */}
              <div
                className="rounded-2xl border border-indigo-500/30 bg-gradient-to-br from-indigo-950/50 via-purple-950/25 to-zinc-900 p-4 shadow-lg"
                style={{ transformStyle: "preserve-3d" }}
              >
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
                <p className="text-2xl sm:text-3xl font-black text-foreground mt-1.5">
                  {formatCurrency(planResult.required_monthly_savings)}
                  <span className="text-xs text-muted-foreground font-normal"> / month for {planResult.target_months} months</span>
                </p>
                <p className="text-[11px] text-zinc-400 mt-1">
                  Target: <strong className="text-zinc-200">{formatCurrency(planResult.target_amount)}</strong> for &ldquo;{planResult.goal_name}&rdquo;.
                </p>
              </div>

              {/* Category Cuts Table */}
              {planResult.category_cuts.length > 0 && (
                <div className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-3.5 space-y-2.5">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-bold text-zinc-200 flex items-center gap-1.5">
                      <TrendingDown className="h-3.5 w-3.5 text-amber-400" />
                      <span>Recommended Category Cuts</span>
                    </h4>
                    <span className="text-[10px] text-zinc-400">Target Discretionary Spend</span>
                  </div>

                  <div className="space-y-2 pt-0.5">
                    {planResult.category_cuts.map((cut, idx) => (
                      <div key={idx} className="rounded-xl bg-zinc-950/70 border border-zinc-800/70 p-3 text-xs">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-zinc-200">{cut.category_name}</span>
                          <span className="text-amber-400 font-bold bg-amber-500/15 px-2 py-0.5 rounded-md border border-amber-500/30">
                            Cut {cut.cut_percentage}% (-{formatCurrency(cut.monthly_cut_amount)}/mo)
                          </span>
                        </div>
                        <div className="flex items-center justify-between text-[11px] text-zinc-400 mt-2 pt-2 border-t border-zinc-900">
                          <span>Current: <strong className="text-zinc-300">{formatCurrency(cut.current_monthly_spend)}/mo</strong></span>
                          <ArrowRight className="h-3 w-3 text-zinc-600" />
                          <span className="text-emerald-400 font-semibold">New Cap: {formatCurrency(cut.suggested_monthly_spend)}/mo</span>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Apply Budget Targets Micro-Interaction Button */}
                  <div className="pt-2">
                    <motion.button
                      type="button"
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      onClick={handleApplyBudgetTargets}
                      disabled={appliedBudgets}
                      className={`w-full flex items-center justify-center gap-2 py-2 px-3 rounded-xl text-xs font-semibold transition cursor-pointer border ${
                        appliedBudgets
                          ? "bg-emerald-500/20 text-emerald-300 border-emerald-500/40"
                          : "bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-200 border-indigo-500/40"
                      }`}
                    >
                      {appliedBudgets ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-emerald-400" />
                          <span>Category Budget Caps Adjusted!</span>
                        </>
                      ) : (
                        <>
                          <Zap className="w-3.5 h-3.5 text-amber-400" />
                          <span>Apply Recommended Budget Targets</span>
                        </>
                      )}
                    </motion.button>
                  </div>
                </div>
              )}

              {/* Action Steps */}
              {planResult.action_steps.length > 0 && (
                <div className="rounded-2xl border border-zinc-800/80 bg-zinc-900/30 p-3.5 space-y-2">
                  <h4 className="text-xs font-bold text-zinc-200 flex items-center gap-1.5">
                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" />
                    <span>Action Roadmap</span>
                  </h4>
                  <ul className="space-y-2 pt-0.5">
                    {planResult.action_steps.map((step, idx) => (
                      <li key={idx} className="flex items-start gap-2.5 text-xs text-zinc-300 bg-zinc-950/40 p-2 rounded-lg border border-zinc-900">
                        <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 text-[10px] font-bold">
                          {idx + 1}
                        </span>
                        <span className="leading-relaxed">{step}</span>
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
