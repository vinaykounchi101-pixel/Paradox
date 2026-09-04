"use client";

import React, { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { PieChart, Sparkles, CheckCircle, AlertTriangle, ArrowRight, Loader2, RefreshCw } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { aiApi, FiftyThirtyTwentyResponse } from "@/lib/api/ai";
import { useCurrency } from "@/features/auth/context/CurrencyContext";

export const FiftyThirtyTwentyCard: React.FC = () => {
  const { formatCurrency, currencySymbol } = useCurrency();
  const [data, setData] = useState<FiftyThirtyTwentyResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAnalysis = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const res = await aiApi.getFiftyThirtyTwenty();
      setData(res.data);
    } catch {
      setError("Unable to compute 50/30/20 analysis");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalysis();
  }, []);

  if (isLoading) {
    return (
      <Card className="border-zinc-800 bg-zinc-950/60 p-6 flex items-center justify-center min-h-[220px]">
        <div className="flex items-center gap-2 text-zinc-400 text-xs">
          <Loader2 className="w-4 h-4 animate-spin text-indigo-400" />
          <span>Evaluating 50/30/20 budget framework...</span>
        </div>
      </Card>
    );
  }

  if (error || !data) {
    return null;
  }

  return (
    <Card className="border-zinc-800 bg-zinc-950/60 overflow-hidden relative group">
      {/* Subtle background glow */}
      <div className="absolute top-0 right-0 w-48 h-48 bg-indigo-500/5 rounded-full blur-3xl pointer-events-none" />

      <CardHeader className="pb-3 border-b border-zinc-800/80">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
              <PieChart className="w-4 h-4" />
            </div>
            <div>
              <CardTitle className="text-sm font-semibold text-zinc-100 flex items-center gap-2">
                50/30/20 Wealth Optimizer
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  {data.adherence_score}/100 Score
                </span>
              </CardTitle>
              <CardDescription className="text-xs text-zinc-400">
                Needs (50%) • Wants (30%) • Savings (20%) allocation model
              </CardDescription>
            </div>
          </div>

          <button
            type="button"
            onClick={fetchAnalysis}
            className="p-1.5 rounded-md hover:bg-zinc-800 text-zinc-500 hover:text-zinc-300 transition cursor-pointer"
            title="Refresh 50/30/20 analysis"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
        </div>
      </CardHeader>

      <CardContent className="pt-4 space-y-4">
        {/* Segmented Progress Bar */}
        <div className="space-y-1.5">
          <div className="h-3.5 w-full bg-zinc-900 rounded-full overflow-hidden flex p-0.5 gap-0.5 border border-zinc-800">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${Math.min(data.needs.actual_percentage, 100)}%` }}
              transition={{ duration: 0.8, ease: "easeOut" }}
              className="h-full bg-blue-500 rounded-sm"
              title={`Needs: ${data.needs.actual_percentage}%`}
            />
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${Math.min(data.wants.actual_percentage, 100)}%` }}
              transition={{ duration: 0.8, delay: 0.1, ease: "easeOut" }}
              className="h-full bg-purple-500 rounded-sm"
              title={`Wants: ${data.wants.actual_percentage}%`}
            />
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${Math.min(data.savings.actual_percentage, 100)}%` }}
              transition={{ duration: 0.8, delay: 0.2, ease: "easeOut" }}
              className="h-full bg-emerald-500 rounded-sm"
              title={`Savings: ${data.savings.actual_percentage}%`}
            />
          </div>

          {/* Legend and stats */}
          <div className="grid grid-cols-3 gap-2 pt-1">
            {/* Needs */}
            <div className="p-2.5 rounded-lg border border-blue-500/20 bg-blue-500/5 space-y-1">
              <div className="flex items-center justify-between text-[11px]">
                <span className="font-semibold text-blue-400 flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-blue-500 inline-block" />
                  Needs
                </span>
                <span className="text-zinc-400 text-[10px]">Target: 50%</span>
              </div>
              <div className="text-sm font-bold text-zinc-100">
                {formatCurrency(data.needs.actual_amount)}
              </div>
              <div className="text-[10px] text-zinc-400 flex justify-between">
                <span>{data.needs.actual_percentage}%</span>
                <span className={data.needs.status === "on_track" ? "text-emerald-400 font-medium" : "text-amber-400 font-medium"}>
                  {data.needs.status === "on_track" ? "Balanced" : "Over Target"}
                </span>
              </div>
            </div>

            {/* Wants */}
            <div className="p-2.5 rounded-lg border border-purple-500/20 bg-purple-500/5 space-y-1">
              <div className="flex items-center justify-between text-[11px]">
                <span className="font-semibold text-purple-400 flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-purple-500 inline-block" />
                  Wants
                </span>
                <span className="text-zinc-400 text-[10px]">Target: 30%</span>
              </div>
              <div className="text-sm font-bold text-zinc-100">
                {formatCurrency(data.wants.actual_amount)}
              </div>
              <div className="text-[10px] text-zinc-400 flex justify-between">
                <span>{data.wants.actual_percentage}%</span>
                <span className={data.wants.status === "on_track" ? "text-emerald-400 font-medium" : "text-amber-400 font-medium"}>
                  {data.wants.status === "on_track" ? "In Check" : "Over 30%"}
                </span>
              </div>
            </div>

            {/* Savings */}
            <div className="p-2.5 rounded-lg border border-emerald-500/20 bg-emerald-500/5 space-y-1">
              <div className="flex items-center justify-between text-[11px]">
                <span className="font-semibold text-emerald-400 flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block" />
                  Savings
                </span>
                <span className="text-zinc-400 text-[10px]">Target: 20%</span>
              </div>
              <div className="text-sm font-bold text-zinc-100">
                {formatCurrency(data.savings.actual_amount)}
              </div>
              <div className="text-[10px] text-zinc-400 flex justify-between">
                <span>{data.savings.actual_percentage}%</span>
                <span className={data.savings.status === "on_track" ? "text-emerald-400 font-medium" : "text-amber-400 font-medium"}>
                  {data.savings.status === "on_track" ? "On Target" : "Below 20%"}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* AI Rebalance Recommendations */}
        {data.rebalance_advice && data.rebalance_advice.length > 0 && (
          <div className="p-3 rounded-lg border border-indigo-500/20 bg-indigo-500/5 space-y-1.5">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-indigo-300">
              <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
              <span>Smart Rebalancing Tip</span>
            </div>
            <p className="text-xs text-zinc-300 leading-relaxed">
              {data.rebalance_advice[0]}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
