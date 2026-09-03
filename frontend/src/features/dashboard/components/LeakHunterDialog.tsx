"use client";

import React, { useState, useEffect } from "react";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useCurrency } from "@/features/auth/context/CurrencyContext";
import { aiApi, LeakAnalysisResponse } from "@/lib/api/ai";
import { useToast } from "@/components/ui/toast";
import { AlertCircle, ArrowDownRight, Loader2, Sparkles, TrendingDown } from "lucide-react";

interface LeakHunterDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

export function LeakHunterDialog({ isOpen, onClose }: LeakHunterDialogProps) {
  const { formatCurrency, currencySymbol } = useCurrency();
  const { error: toastError } = useToast();

  const [threshold, setThreshold] = useState<number>(150);
  const [data, setData] = useState<LeakAnalysisResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const fetchLeaks = async (thresh: number) => {
    setLoading(true);
    try {
      const res = await aiApi.getLeakAnalysis(thresh);
      setData(res.data);
    } catch (err: any) {
      toastError(err?.message || "Could not complete leak analysis.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchLeaks(threshold);
    }
  }, [isOpen, threshold]);

  return (
    <Dialog isOpen={isOpen} onClose={onClose} title="🔍 Micro-Spending Leak Hunter">
      <div className="space-y-6">
        <p className="text-sm text-zinc-400 leading-relaxed">
          Small, repeated expenses (sub-{currencySymbol}150 snacks, tea, delivery surcharges) feel
          negligible individually, but create serious annual capital drain. Paradox surfaces these patterns
          so you can plug the leaks.
        </p>

        {/* Threshold Picker */}
        <div className="flex items-center gap-2 text-xs">
          <span className="text-zinc-400 font-medium">Filter threshold:</span>
          {[100, 150, 250, 500].map((val) => (
            <button
              key={val}
              type="button"
              onClick={() => setThreshold(val)}
              className={`px-2.5 py-1 rounded-lg border transition-all ${
                threshold === val
                  ? "bg-indigo-600/30 border-indigo-500 text-indigo-300 font-semibold"
                  : "bg-zinc-900 border-zinc-800 text-zinc-400 hover:text-zinc-200"
              }`}
            >
              ≤ {currencySymbol}{val}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-3 text-zinc-400">
            <Loader2 className="w-6 h-6 animate-spin text-indigo-400" />
            <span className="text-sm">Scanning past 90 days of transactions...</span>
          </div>
        ) : data ? (
          <div className="space-y-5">
            {/* Top Stat Banner */}
            <div className="grid grid-cols-2 gap-3">
              <div className="p-4 rounded-xl bg-rose-950/20 border border-rose-500/20 space-y-1">
                <div className="text-[11px] font-medium text-rose-400 flex items-center gap-1">
                  <TrendingDown className="w-3.5 h-3.5" /> Monthly Leakage
                </div>
                <div className="text-xl font-bold text-rose-300">
                  {formatCurrency(data.total_monthly_leak)}
                  <span className="text-xs font-normal text-rose-400/80"> / mo</span>
                </div>
              </div>

              <div className="p-4 rounded-xl bg-amber-950/20 border border-amber-500/20 space-y-1">
                <div className="text-[11px] font-medium text-amber-400 flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" /> Annual Capital Drain
                </div>
                <div className="text-xl font-bold text-amber-300">
                  {formatCurrency(data.total_annual_leak)}
                  <span className="text-xs font-normal text-amber-400/80"> / yr</span>
                </div>
              </div>
            </div>

            {/* Summary Message */}
            <div className="p-3 bg-zinc-900/60 rounded-xl border border-zinc-800/80 text-xs text-zinc-300 flex items-start gap-2.5">
              <Sparkles className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
              <span>{data.summary}</span>
            </div>

            {/* Leaks List */}
            <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1">
              <h4 className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">
                Detected Habits ({data.leaks.length})
              </h4>
              {data.leaks.length === 0 ? (
                <div className="text-center py-6 text-sm text-zinc-500">
                  🎉 No repetitive micro-spending leaks detected under {currencySymbol}{threshold}!
                </div>
              ) : (
                data.leaks.map((leak, idx) => (
                  <div
                    key={idx}
                    className="p-3 bg-zinc-900/40 hover:bg-zinc-900/70 transition-all rounded-xl border border-zinc-800/80 space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-semibold text-sm text-zinc-200">
                          {leak.merchant_or_pattern}
                        </div>
                        <div className="text-[11px] text-zinc-500">
                          ~{leak.frequency_per_month}x / month • Avg {formatCurrency(leak.avg_amount)}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-bold text-sm text-rose-400">
                          -{formatCurrency(leak.annualized_drain)}
                          <span className="text-[10px] text-zinc-500 font-normal"> / yr</span>
                        </div>
                        <span className="inline-block px-1.5 py-0.5 rounded text-[10px] bg-zinc-800 text-zinc-400">
                          {leak.category_name}
                        </span>
                      </div>
                    </div>

                    <div className="text-xs text-indigo-300 bg-indigo-950/30 p-2 rounded-lg border border-indigo-500/20">
                      💡 {leak.savings_tip}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        ) : null}

        <div className="pt-2 flex justify-end">
          <Button
            type="button"
            onClick={onClose}
            className="bg-zinc-800 hover:bg-zinc-700 text-white"
          >
            Got It
          </Button>
        </div>
      </div>
    </Dialog>
  );
}
