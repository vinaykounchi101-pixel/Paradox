"use client";

import React, { useEffect, useState } from "react";
import { aiApi, SafeToSpendResponse } from "@/lib/api/ai";
import { useCurrency } from "@/features/auth/context/CurrencyContext";
import { Gauge, Flame, Calendar, AlertCircle, CheckCircle2, AlertTriangle, ArrowUpRight } from "lucide-react";
import Link from "next/link";

interface SafeToSpendCardProps {
  onOpenSimulator?: () => void;
}

export function SafeToSpendCard({ onOpenSimulator }: SafeToSpendCardProps) {
  const { formatCurrency } = useCurrency();
  const [data, setData] = useState<SafeToSpendResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    let isMounted = true;
    const fetchData = async () => {
      try {
        const res = await aiApi.getSafeToSpend();
        if (isMounted) setData(res.data);
      } catch (err) {
        // Silently keep null
      } finally {
        if (isMounted) setLoading(false);
      }
    };
    fetchData();
    return () => {
      isMounted = false;
    };
  }, []);

  if (loading) {
    return (
      <div className="bg-zinc-900/50 backdrop-blur-xl border border-zinc-800/80 rounded-2xl p-6 animate-pulse space-y-4">
        <div className="h-4 bg-zinc-800 rounded w-1/3" />
        <div className="h-8 bg-zinc-800 rounded w-1/2" />
        <div className="h-12 bg-zinc-800 rounded" />
      </div>
    );
  }

  if (!data) return null;

  const isOptimal = data.status === "optimal";
  const isWarning = data.status === "warning";
  const isDanger = data.status === "danger";

  return (
    <div className="bg-zinc-900/60 backdrop-blur-xl border border-zinc-800/80 hover:border-zinc-700/80 transition-all rounded-2xl p-5 md:p-6 shadow-xl relative overflow-hidden flex flex-col justify-between">
      {/* Glow Effect */}
      <div
        className={`absolute top-0 right-0 w-36 h-36 rounded-full blur-3xl pointer-events-none opacity-20 ${
          isOptimal ? "bg-emerald-500" : isWarning ? "bg-amber-500" : "bg-rose-500"
        }`}
      />

      <div>
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
              <Gauge className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-medium text-zinc-300">Safe-to-Spend Speedometer</h3>
              <p className="text-[11px] text-zinc-500">Real-time daily budget pacing</p>
            </div>
          </div>

          <span
            className={`px-2.5 py-1 rounded-full text-xs font-medium flex items-center gap-1 border ${
              isOptimal
                ? "bg-emerald-950/40 border-emerald-500/30 text-emerald-400"
                : isWarning
                ? "bg-amber-950/40 border-amber-500/30 text-amber-400"
                : "bg-rose-950/40 border-rose-500/30 text-rose-400"
            }`}
          >
            {isOptimal ? (
              <CheckCircle2 className="w-3 h-3" />
            ) : isWarning ? (
              <AlertTriangle className="w-3 h-3" />
            ) : (
              <AlertCircle className="w-3 h-3" />
            )}
            {data.status.toUpperCase()}
          </span>
        </div>

        {/* Primary Metric: Safe Daily Allowance */}
        <div className="space-y-1 mb-5">
          <div className="text-xs text-zinc-400 font-medium">Safe Daily Allowance</div>
          <div className="text-2xl md:text-3xl font-bold tracking-tight text-white flex items-baseline gap-1.5">
            {formatCurrency(data.safe_daily_allowance)}
            <span className="text-xs text-zinc-500 font-normal">/ day</span>
          </div>
        </div>

        {/* Secondary Comparison Grid */}
        <div className="grid grid-cols-2 gap-2.5 mb-4 text-xs">
          <div className="p-2.5 bg-zinc-950/40 rounded-xl border border-zinc-800/60 flex items-center gap-2">
            <div className="p-1.5 rounded-lg bg-rose-500/10 text-rose-400">
              <Flame className="w-3.5 h-3.5" />
            </div>
            <div>
              <div className="text-[10px] text-zinc-500">Actual Burn</div>
              <div className="font-semibold text-zinc-200">
                {formatCurrency(data.current_daily_burn_rate)}/d
              </div>
            </div>
          </div>

          <div className="p-2.5 bg-zinc-950/40 rounded-xl border border-zinc-800/60 flex items-center gap-2">
            <div className="p-1.5 rounded-lg bg-blue-500/10 text-blue-400">
              <Calendar className="w-3.5 h-3.5" />
            </div>
            <div>
              <div className="text-[10px] text-zinc-500">Days Left</div>
              <div className="font-semibold text-zinc-200">
                {data.days_remaining} days
              </div>
            </div>
          </div>
        </div>

        {/* Depletion Notice or Status Message */}
        <p className="text-xs text-zinc-400 leading-relaxed bg-zinc-950/30 p-2.5 rounded-xl border border-zinc-800/50 mb-4">
          {data.burn_status_message}
        </p>
      </div>

      {/* Quick Actions */}
      <div className="flex items-center gap-2 pt-2 border-t border-zinc-800/60 text-xs">
        {onOpenSimulator && (
          <button
            type="button"
            onClick={onOpenSimulator}
            className="text-indigo-400 hover:text-indigo-300 font-medium flex items-center gap-1 transition-colors"
          >
            Simulate Purchase <ArrowUpRight className="w-3.5 h-3.5" />
          </button>
        )}
        <Link
          href="/budget"
          className="ml-auto text-zinc-500 hover:text-zinc-300 transition-colors"
        >
          Adjust Budget →
        </Link>
      </div>
    </div>
  );
}
