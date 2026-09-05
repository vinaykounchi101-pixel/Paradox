"use client";

import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Activity,
  ShieldAlert,
  Calendar,
  Sparkles,
  RefreshCw,
  Zap,
  ArrowRight,
  Filter,
} from "lucide-react";
import {
  aiApi,
  AnomaliesResponse,
  SpendingForecastResponse,
} from "@/lib/api/ai";
import { useCurrency } from "@/features/auth/context/CurrencyContext";

export const AnomalyForecastCard: React.FC = () => {
  const { formatCurrency } = useCurrency();
  const [activeTab, setActiveTab] = useState<"anomalies" | "forecast">("anomalies");
  const [severityFilter, setSeverityFilter] = useState<"all" | "critical" | "high" | "moderate">("all");
  const [anomaliesData, setAnomaliesData] = useState<AnomaliesResponse | null>(null);
  const [forecastData, setForecastData] = useState<SpendingForecastResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  const fetchData = async (isManual = false) => {
    try {
      if (isManual) setIsRefreshing(true);
      else setIsLoading(true);

      const [anomRes, foreRes] = await Promise.all([
        aiApi.getAnomalies(),
        aiApi.getForecast(),
      ]);

      setAnomaliesData(anomRes.data);
      setForecastData(foreRes.data);
    } catch {
      // Graceful fallback
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (isLoading) {
    return (
      <div className="rounded-3xl border border-border/50 bg-zinc-950/70 p-5 sm:p-6 animate-pulse space-y-4">
        <div className="flex justify-between items-center">
          <div className="h-6 w-52 bg-zinc-800 rounded-lg" />
          <div className="h-8 w-36 bg-zinc-800 rounded-full" />
        </div>
        <div className="h-32 bg-zinc-900/60 rounded-2xl" />
      </div>
    );
  }

  const allAnomalies = anomaliesData?.anomalies || [];
  const filteredAnomalies =
    severityFilter === "all"
      ? allAnomalies
      : allAnomalies.filter((a) => a.severity.toLowerCase() === severityFilter);

  const anomaliesCount = allAnomalies.length;

  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="relative overflow-hidden rounded-3xl border border-zinc-800/80 bg-gradient-to-br from-zinc-950 via-zinc-900/90 to-zinc-950 p-5 sm:p-6 shadow-xl backdrop-blur-md"
      style={{
        boxShadow: "0 20px 40px -15px rgba(0, 0, 0, 0.6), inset 0 1px 1px rgba(255, 255, 255, 0.08)",
        transformStyle: "preserve-3d",
      }}
    >
      {/* Ambient background glow */}
      <div className="absolute -top-20 -right-20 w-56 h-56 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3.5 pb-4 border-b border-zinc-800/70">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-500/15 text-amber-400 border border-amber-500/30">
            <Activity className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm sm:text-base font-bold text-foreground flex items-center gap-2">
              <span>Anomaly Guard & Spending Forecast</span>
              {anomaliesCount > 0 ? (
                <span className="flex items-center gap-1 text-[10px] font-bold text-rose-300 bg-rose-500/20 border border-rose-500/35 px-2 py-0.5 rounded-full animate-pulse">
                  <ShieldAlert className="h-3 w-3 text-rose-400" />
                  {anomaliesCount} Outlier{anomaliesCount > 1 ? "s" : ""}
                </span>
              ) : (
                <span className="text-[10px] font-semibold text-emerald-300 bg-emerald-500/15 border border-emerald-500/30 px-2 py-0.5 rounded-full">
                  🛡️ Normal
                </span>
              )}
            </h3>
            <p className="text-[11px] text-muted-foreground">
              Statistical outlier surveillance & rolling 30-day velocity projections
            </p>
          </div>
        </div>

        {/* Tab switcher + Refresh */}
        <div className="flex items-center gap-2 self-start sm:self-auto">
          <div className="relative flex rounded-xl bg-zinc-900 p-1 border border-zinc-800 text-xs">
            <button
              type="button"
              onClick={() => setActiveTab("anomalies")}
              className={`relative z-10 px-3 py-1.5 rounded-lg font-medium transition cursor-pointer flex items-center gap-1.5 ${
                activeTab === "anomalies" ? "text-white" : "text-zinc-400 hover:text-zinc-200"
              }`}
            >
              <span>🚨 Anomalies ({anomaliesCount})</span>
              {activeTab === "anomalies" && (
                <motion.div
                  layoutId="tabPill"
                  className="absolute inset-0 bg-gradient-to-r from-indigo-600 to-purple-600 rounded-lg shadow-md -z-10"
                  transition={{ type: "spring", stiffness: 400, damping: 30 }}
                />
              )}
            </button>

            <button
              type="button"
              onClick={() => setActiveTab("forecast")}
              className={`relative z-10 px-3 py-1.5 rounded-lg font-medium transition cursor-pointer flex items-center gap-1.5 ${
                activeTab === "forecast" ? "text-white" : "text-zinc-400 hover:text-zinc-200"
              }`}
            >
              <span>🔮 30-Day Forecast</span>
              {activeTab === "forecast" && (
                <motion.div
                  layoutId="tabPill"
                  className="absolute inset-0 bg-gradient-to-r from-indigo-600 to-purple-600 rounded-lg shadow-md -z-10"
                  transition={{ type: "spring", stiffness: 400, damping: 30 }}
                />
              )}
            </button>
          </div>

          <motion.button
            type="button"
            whileHover={{ scale: 1.1, rotate: 180 }}
            whileTap={{ scale: 0.9 }}
            transition={{ duration: 0.25 }}
            onClick={() => fetchData(true)}
            disabled={isRefreshing}
            title="Refresh Intelligence"
            className="p-2 text-zinc-400 hover:text-zinc-200 rounded-xl bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 transition cursor-pointer"
          >
            <RefreshCw className={`h-4 w-4 ${isRefreshing ? "animate-spin text-indigo-400" : ""}`} />
          </motion.button>
        </div>
      </div>

      {/* Content Body with Animation */}
      <AnimatePresence mode="wait">
        {activeTab === "anomalies" ? (
          /* Anomalies View */
          <motion.div
            key="anomalies"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="mt-4 space-y-3"
          >
            {allAnomalies.length > 0 ? (
              <>
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <p className="text-xs text-zinc-400 leading-relaxed">
                    {anomaliesData?.summary}
                  </p>

                  {/* Filter Pills */}
                  <div className="flex items-center gap-1 shrink-0 self-start sm:self-auto">
                    {(["all", "critical", "high", "moderate"] as const).map((lvl) => (
                      <button
                        key={lvl}
                        type="button"
                        onClick={() => setSeverityFilter(lvl)}
                        className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-md border transition cursor-pointer ${
                          severityFilter === lvl
                            ? "bg-zinc-800 text-white border-zinc-700"
                            : "text-zinc-500 border-transparent hover:text-zinc-300"
                        }`}
                      >
                        {lvl}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Anomalies Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-64 overflow-y-auto pr-1">
                  {filteredAnomalies.map((item) => (
                    <motion.div
                      key={item.id}
                      whileHover={{ scale: 1.015, y: -2 }}
                      className="rounded-2xl border border-zinc-800/90 bg-zinc-900/60 p-3.5 flex flex-col justify-between gap-2 shadow-sm"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <span className="text-xs font-bold text-foreground flex items-center gap-1.5">
                            <span>{item.description || item.category_name}</span>
                          </span>
                          <p className="text-[10px] text-muted-foreground mt-0.5 font-mono">
                            📅 {item.date} • {item.category_name}
                          </p>
                        </div>
                        <div className="text-right shrink-0">
                          <span className="text-xs font-mono font-bold text-rose-400">
                            {formatCurrency(item.amount)}
                          </span>
                          <div
                            className={`text-[9px] uppercase font-bold px-1.5 py-0.5 rounded-md w-fit ml-auto mt-0.5 border ${
                              item.severity === "critical"
                                ? "bg-rose-500/20 text-rose-300 border-rose-500/40"
                                : item.severity === "high"
                                ? "bg-amber-500/20 text-amber-300 border-amber-500/40"
                                : "bg-blue-500/20 text-blue-300 border-blue-500/40"
                            }`}
                          >
                            {item.severity}
                          </div>
                        </div>
                      </div>
                      <p className="text-[11px] text-zinc-400 leading-snug border-t border-zinc-800/60 pt-2">
                        ⚠️ {item.reason}
                      </p>
                    </motion.div>
                  ))}
                </div>
              </>
            ) : (
              <div className="rounded-2xl border border-emerald-500/25 bg-emerald-500/10 p-5 text-center space-y-1">
                <span className="text-xl">🛡️</span>
                <p className="text-xs font-semibold text-emerald-300">
                  Zero Anomalies Detected!
                </p>
                <p className="text-[11px] text-zinc-400">
                  All your expenses sit comfortably within statistical normality. Keep up the consistent financial rhythm!
                </p>
              </div>
            )}
          </motion.div>
        ) : (
          /* Forecast View */
          <motion.div
            key="forecast"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="mt-4 space-y-3.5"
          >
            {forecastData ? (
              <>
                {/* Projected Top Banner */}
                <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-gradient-to-r from-indigo-950/40 via-purple-950/25 to-zinc-900 border border-indigo-500/25 p-4 shadow-md">
                  <div>
                    <span className="text-[11px] text-indigo-300 font-semibold flex items-center gap-1">
                      <span>🔮 Projected Next Month Spend</span>
                    </span>
                    <p className="text-xl sm:text-2xl font-black text-foreground font-mono mt-0.5">
                      {formatCurrency(forecastData.total_projected_next_month)}
                    </p>
                  </div>

                  <div className="text-right">
                    <span className="text-[10px] text-zinc-400 font-medium">Trajectory Velocity</span>
                    <div className="flex items-center justify-end gap-1.5 text-xs font-bold mt-0.5">
                      {forecastData.growth_rate_pct >= 0 ? (
                        <div className="flex items-center gap-1 text-amber-400 bg-amber-500/15 px-2 py-0.5 rounded-full border border-amber-500/30">
                          <TrendingUp className="h-3.5 w-3.5" />
                          <span>+{forecastData.growth_rate_pct}% Projected Rise</span>
                        </div>
                      ) : (
                        <div className="flex items-center gap-1 text-emerald-400 bg-emerald-500/15 px-2 py-0.5 rounded-full border border-emerald-500/30">
                          <TrendingDown className="h-3.5 w-3.5" />
                          <span>{forecastData.growth_rate_pct}% Projected Drop</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Category Forecast Comparative Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 max-h-64 overflow-y-auto pr-1">
                  {forecastData.category_forecasts.map((cat, idx) => {
                    const currentAmt = parseFloat(String(cat.current_spent));
                    const projectedAmt = parseFloat(String(cat.projected_next_month));
                    const isUp = cat.trend_direction === "up";

                    return (
                      <motion.div
                        key={idx}
                        whileHover={{ scale: 1.01 }}
                        className="rounded-2xl border border-zinc-800/80 bg-zinc-900/50 p-3 flex flex-col justify-between text-xs space-y-2 shadow-xs"
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-zinc-200 truncate flex items-center gap-1">
                            <span>{cat.category_name}</span>
                            {isUp ? (
                              <span className="text-[10px] text-amber-400 font-normal">📈</span>
                            ) : (
                              <span className="text-[10px] text-emerald-400 font-normal">📉</span>
                            )}
                          </span>
                          <span className="font-mono font-bold text-foreground">
                            {formatCurrency(cat.projected_next_month)}
                          </span>
                        </div>

                        {/* Comparative Visual Bar */}
                        <div className="space-y-1">
                          <div className="h-1.5 w-full bg-zinc-950 rounded-full overflow-hidden flex border border-zinc-800/60">
                            <div
                              className={`h-full rounded-full ${isUp ? "bg-amber-500" : "bg-emerald-500"}`}
                              style={{ width: `${Math.min(Math.max((currentAmt / (projectedAmt || 1)) * 100, 15), 100)}%` }}
                            />
                          </div>
                          <div className="flex items-center justify-between text-[10px] text-zinc-400">
                            <span>Current: {formatCurrency(cat.current_spent)}</span>
                            <span className={isUp ? "text-amber-400" : "text-emerald-400"}>
                              {isUp ? "Rising Pace" : "Easing Pace"}
                            </span>
                          </div>
                        </div>
                      </motion.div>
                    );
                  })}
                </div>
              </>
            ) : (
              <p className="text-xs text-muted-foreground text-center py-6">
                Forecast model computing rolling patterns...
              </p>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};
