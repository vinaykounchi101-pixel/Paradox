"use client";

import React, { useState, useEffect, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Sparkles,
  ShieldCheck,
  AlertTriangle,
  AlertOctagon,
  TrendingUp,
  Lightbulb,
  RefreshCw,
  Flame,
  CheckCircle2,
} from "lucide-react";
import { useCurrency } from "@/features/auth/context/CurrencyContext";
import { aiApi, AIInsightsResponse } from "@/lib/api/ai";

interface FinancialCopilotCardProps {
  period: "current_month" | "last_30_days" | "current_week";
}

export const FinancialCopilotCard: React.FC<FinancialCopilotCardProps> = ({ period }) => {
  const { formatCurrency } = useCurrency();
  const [insights, setInsights] = useState<AIInsightsResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchInsights = useCallback(async (isManual = false) => {
    try {
      if (isManual) setIsRefreshing(true);
      else setIsLoading(true);
      setError(null);
      const res = await aiApi.getInsights(period);
      setInsights(res.data);
    } catch {
      setError("AI Copilot currently unavailable");
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [period]);

  useEffect(() => {
    fetchInsights();
  }, [fetchInsights]);

  if (isLoading) {
    return (
      <div className="rounded-2xl border border-indigo-500/20 bg-zinc-950/70 p-5 animate-pulse space-y-4">
        <div className="flex justify-between items-center">
          <div className="h-5 w-40 bg-zinc-800 rounded-md" />
          <div className="h-6 w-20 bg-zinc-800 rounded-full" />
        </div>
        <div className="h-4 w-3/4 bg-zinc-800/80 rounded" />
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
          <div className="h-16 bg-zinc-900 rounded-xl" />
          <div className="h-16 bg-zinc-900 rounded-xl" />
        </div>
      </div>
    );
  }

  if (error || !insights) {
    return null; // Gracefully degrade if offline/error
  }

  const { health_status, headline, alerts, saving_tips, projected_spend, daily_burn_rate, provider_used } = insights;

  const getStatusBadge = () => {
    switch (health_status) {
      case "critical":
        return {
          icon: AlertOctagon,
          label: "Critical Pace",
          classes: "bg-rose-500/10 text-rose-400 border-rose-500/30",
        };
      case "cautious":
        return {
          icon: AlertTriangle,
          label: "Cautious Pace",
          classes: "bg-amber-500/10 text-amber-400 border-amber-500/30",
        };
      case "healthy":
      default:
        return {
          icon: ShieldCheck,
          label: "Healthy Pace",
          classes: "bg-emerald-500/10 text-emerald-400 border-emerald-500/30",
        };
    }
  };

  const status = getStatusBadge();
  const StatusIcon = status.icon;

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="relative overflow-hidden rounded-2xl border border-indigo-500/25 bg-gradient-to-br from-indigo-950/20 via-zinc-900/60 to-purple-950/20 p-5 shadow-[0_0_30px_rgba(99,102,241,0.08)] backdrop-blur-sm"
    >
      {/* Ambient background glow */}
      <div className="pointer-events-none absolute -right-10 -top-10 h-32 w-32 rounded-full bg-indigo-500/10 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-10 -left-10 h-32 w-32 rounded-full bg-purple-500/10 blur-3xl" />

      {/* Header */}
      <div className="flex items-center justify-between gap-2 border-b border-border/40 pb-3">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/15 text-indigo-400">
            <Sparkles className="h-4 w-4" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold tracking-tight text-foreground">
                Paradox Financial Copilot
              </h3>
              <span className="text-[10px] uppercase font-semibold tracking-wider text-muted-foreground bg-zinc-800/80 px-1.5 py-0.5 rounded">
                {provider_used}
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${status.classes}`}
          >
            <StatusIcon className="h-3.5 w-3.5" />
            <span>{status.label}</span>
          </div>

          <button
            type="button"
            onClick={() => fetchInsights(true)}
            disabled={isRefreshing}
            title="Refresh Copilot Insights"
            className="p-1.5 text-muted-foreground hover:text-foreground rounded-lg hover:bg-zinc-800/60 transition cursor-pointer"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isRefreshing ? "animate-spin text-indigo-400" : ""}`} />
          </button>
        </div>
      </div>

      {/* Headline banner */}
      <div className="mt-3.5">
        <p className="text-sm font-medium text-foreground/90 leading-snug">
          {headline}
        </p>
      </div>

      {/* Burn Rate & Projection Metrics */}
      {(daily_burn_rate || projected_spend) && (
        <div className="mt-4 grid grid-cols-2 gap-3">
          {daily_burn_rate && (
            <div className="rounded-xl border border-border/50 bg-zinc-900/60 p-3">
              <div className="flex items-center gap-1.5 text-[11px] font-medium text-muted-foreground">
                <Flame className="h-3.5 w-3.5 text-amber-400" />
                <span>Daily Burn Velocity</span>
              </div>
              <p className="mt-1 text-base font-bold text-foreground">
                {formatCurrency(daily_burn_rate)}
                <span className="text-xs text-muted-foreground font-normal"> / day</span>
              </p>
            </div>
          )}

          {projected_spend && (
            <div className="rounded-xl border border-border/50 bg-zinc-900/60 p-3">
              <div className="flex items-center gap-1.5 text-[11px] font-medium text-muted-foreground">
                <TrendingUp className="h-3.5 w-3.5 text-indigo-400" />
                <span>Projected Period Total</span>
              </div>
              <p className="mt-1 text-base font-bold text-foreground">
                {formatCurrency(projected_spend)}
              </p>
            </div>
          )}
        </div>
      )}

      {/* Actionable Alerts & Tips Grid */}
      <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3">
        {/* Alerts */}
        {alerts && alerts.length > 0 && (
          <div className="rounded-xl border border-border/40 bg-zinc-950/40 p-3 space-y-2">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
              <AlertTriangle className="h-3.5 w-3.5 text-amber-400" />
              Observations & Alerts
            </span>
            <ul className="space-y-1.5 text-xs text-zinc-300">
              {alerts.map((alert, idx) => (
                <li key={idx} className="flex items-start gap-2 leading-relaxed">
                  <span className="text-indigo-400 font-bold">•</span>
                  <span>{alert}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Saving Recommendations */}
        {saving_tips && saving_tips.length > 0 && (
          <div className="rounded-xl border border-border/40 bg-zinc-950/40 p-3 space-y-2">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground flex items-center gap-1.5">
              <Lightbulb className="h-3.5 w-3.5 text-emerald-400" />
              Smart Saving Opportunities
            </span>
            <ul className="space-y-1.5 text-xs text-zinc-300">
              {saving_tips.map((tip, idx) => (
                <li key={idx} className="flex items-start gap-2 leading-relaxed">
                  <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400 shrink-0 mt-0.5" />
                  <span>{tip}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </motion.div>
  );
};
