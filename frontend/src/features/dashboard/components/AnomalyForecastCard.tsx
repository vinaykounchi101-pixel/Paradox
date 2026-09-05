"use client";

import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
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
      <div className="rounded-2xl border border-border/50 bg-zinc-950/70 p-5 animate-pulse space-y-3">
        <div className="flex justify-between items-center">
          <div className="h-5 w-48 bg-zinc-800 rounded-md" />
          <div className="h-6 w-24 bg-zinc-800 rounded-full" />
        </div>
        <div className="h-28 bg-zinc-900/60 rounded-xl" />
      </div>
    );
  }

  const anomaliesCount = anomaliesData?.total_anomalies || 0;

  return (
    <div className="relative overflow-hidden rounded-2xl border border-border/60 bg-zinc-950/80 p-5 shadow-lg backdrop-blur-sm">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-border/40">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/15 text-amber-400">
            <Activity className="h-4 w-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-foreground flex items-center gap-2">
              Anomaly Guard & Spend Forecast
              {anomaliesCount > 0 && (
                <span className="flex items-center gap-1 text-[10px] font-semibold text-rose-400 bg-rose-500/15 border border-rose-500/30 px-1.5 py-0.5 rounded-full">
                  <ShieldAlert className="h-3 w-3" />
                  {anomaliesCount} flagged
                </span>
              )}
            </h3>
            <p className="text-[11px] text-muted-foreground">
              Statistical outlier screening and 30-day velocity projections
            </p>
          </div>
        </div>

        {/* Tab switcher + Refresh */}
        <div className="flex items-center gap-1.5 self-start sm:self-auto">
          <div className="flex rounded-lg bg-zinc-900 p-1 border border-zinc-800 text-xs">
            <button
              type="button"
              onClick={() => setActiveTab("anomalies")}
              className={`px-2.5 py-1 rounded-md font-medium transition cursor-pointer ${
                activeTab === "anomalies"
                  ? "bg-zinc-800 text-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              Anomalies ({anomaliesCount})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("forecast")}
              className={`px-2.5 py-1 rounded-md font-medium transition cursor-pointer ${
                activeTab === "forecast"
                  ? "bg-zinc-800 text-foreground shadow-xs"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              30-Day Forecast
            </button>
          </div>

          <button
            type="button"
            onClick={() => fetchData(true)}
            disabled={isRefreshing}
            title="Refresh Analysis"
            className="p-1.5 text-muted-foreground hover:text-foreground rounded-lg hover:bg-zinc-800/60 transition cursor-pointer"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isRefreshing ? "animate-spin text-indigo-400" : ""}`} />
          </button>
        </div>
      </div>

      {/* Content Body */}
      <div className="mt-4">
        {activeTab === "anomalies" ? (
          /* Anomalies View */
          <div className="space-y-3">
            {anomaliesData?.anomalies && anomaliesData.anomalies.length > 0 ? (
              <>
                <p className="text-xs text-muted-foreground">
                  {anomaliesData.summary}
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 max-h-60 overflow-y-auto pr-1">
                  {anomaliesData.anomalies.map((item) => (
                    <motion.div
                      key={item.id}
                      initial={{ opacity: 0, y: 4 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="rounded-xl border border-zinc-800/80 bg-zinc-900/50 p-3 flex flex-col justify-between gap-1.5"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <span className="text-xs font-semibold text-foreground">
                            {item.description || item.category_name}
                          </span>
                          <p className="text-[10px] text-muted-foreground">{item.date} • {item.category_name}</p>
                        </div>
                        <div className="text-right">
                          <span className="text-xs font-bold text-rose-400">
                            {formatCurrency(item.amount)}
                          </span>
                          <div className="text-[9px] uppercase font-bold text-amber-400 bg-amber-400/10 px-1 rounded w-fit ml-auto mt-0.5">
                            {item.severity}
                          </div>
                        </div>
                      </div>
                      <p className="text-[11px] text-zinc-400 leading-snug border-t border-zinc-800/50 pt-1.5">
                        {item.reason}
                      </p>
                    </motion.div>
                  ))}
                </div>
              </>
            ) : (
              <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-center">
                <p className="text-xs font-medium text-emerald-400">
                  ✨ No statistical anomalies detected. Your spending is consistent across all categories!
                </p>
              </div>
            )}
          </div>
        ) : (
          /* Forecast View */
          <div className="space-y-3">
            {forecastData ? (
              <>
                <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-indigo-950/30 border border-indigo-500/20 p-3">
                  <div>
                    <span className="text-[11px] text-indigo-300 font-medium">Projected Next Month Total</span>
                    <p className="text-lg font-bold text-foreground">
                      {formatCurrency(forecastData.total_projected_next_month)}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] text-zinc-400 font-medium">Trend Velocity</span>
                    <div className="flex items-center gap-1 text-xs font-bold text-indigo-300">
                      {forecastData.growth_rate_pct >= 0 ? (
                        <TrendingUp className="h-3.5 w-3.5 text-amber-400" />
                      ) : (
                        <TrendingDown className="h-3.5 w-3.5 text-emerald-400" />
                      )}
                      <span>
                        {forecastData.growth_rate_pct > 0 ? `+${forecastData.growth_rate_pct}%` : `${forecastData.growth_rate_pct}%`}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Category Forecast List */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {forecastData.category_forecasts.map((cat, idx) => (
                    <div
                      key={idx}
                      className="rounded-lg border border-zinc-800/70 bg-zinc-900/40 px-3 py-2 flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-2 truncate">
                        <span className="font-medium text-zinc-200 truncate">{cat.category_name}</span>
                        {cat.trend_direction === "up" && (
                          <span title="Trending higher">
                            <TrendingUp className="h-3 w-3 text-amber-400 shrink-0" />
                          </span>
                        )}
                        {cat.trend_direction === "down" && (
                          <span title="Trending lower">
                            <TrendingDown className="h-3 w-3 text-emerald-400 shrink-0" />
                          </span>
                        )}
                      </div>
                      <span className="font-bold text-zinc-100 shrink-0 ml-2">
                        {formatCurrency(cat.projected_next_month)}
                      </span>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <p className="text-xs text-muted-foreground text-center py-4">
                Forecast data compiling...
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
