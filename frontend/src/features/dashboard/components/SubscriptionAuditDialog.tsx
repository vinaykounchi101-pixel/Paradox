"use client";

import React, { useState, useEffect } from "react";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useCurrency } from "@/features/auth/context/CurrencyContext";
import { aiApi, SubscriptionAuditResponse } from "@/lib/api/ai";
import { useToast } from "@/components/ui/toast";
import { AlertTriangle, CheckCircle, CreditCard, Loader2, Repeat, Sparkles } from "lucide-react";

interface SubscriptionAuditDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

export function SubscriptionAuditDialog({ isOpen, onClose }: SubscriptionAuditDialogProps) {
  const { formatCurrency } = useCurrency();
  const { error: toastError } = useToast();

  const [data, setData] = useState<SubscriptionAuditResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const fetchAudit = async () => {
    setLoading(true);
    try {
      const res = await aiApi.getSubscriptionAudit();
      setData(res.data);
    } catch (err: any) {
      toastError(err?.message || "Could not retrieve subscription audit.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchAudit();
    }
  }, [isOpen]);

  return (
    <Dialog isOpen={isOpen} onClose={onClose} title="📊 Subscription & Recurring Commitments Audit">
      <div className="space-y-6">
        <p className="text-sm text-zinc-400 leading-relaxed">
          Audit your recurring subscriptions, detect overlapping services in the same category,
          and unlock potential annual savings by switching to annual billing or bundling.
        </p>

        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-3 text-zinc-400">
            <Loader2 className="w-6 h-6 animate-spin text-purple-400" />
            <span className="text-sm">Auditing recurring commitments...</span>
          </div>
        ) : data ? (
          <div className="space-y-5">
            {/* Top Stat Banner */}
            <div className="grid grid-cols-2 gap-3">
              <div className="p-4 rounded-xl bg-purple-950/20 border border-purple-500/20 space-y-1">
                <div className="text-[11px] font-medium text-purple-400 flex items-center gap-1">
                  <Repeat className="w-3.5 h-3.5" /> Fixed Monthly Load
                </div>
                <div className="text-xl font-bold text-purple-300">
                  {formatCurrency(data.total_monthly_commitment)}
                  <span className="text-xs font-normal text-purple-400/80"> / mo</span>
                </div>
              </div>

              <div className="p-4 rounded-xl bg-emerald-950/20 border border-emerald-500/20 space-y-1">
                <div className="text-[11px] font-medium text-emerald-400 flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5" /> Potential Annual Savings
                </div>
                <div className="text-xl font-bold text-emerald-300">
                  {formatCurrency(data.potential_annual_savings)}
                  <span className="text-xs font-normal text-emerald-400/80"> / yr</span>
                </div>
              </div>
            </div>

            {/* Overlap Warnings */}
            {data.duplicate_warnings.length > 0 && (
              <div className="space-y-2">
                {data.duplicate_warnings.map((warn, i) => (
                  <div
                    key={i}
                    className="p-3 bg-amber-950/25 border border-amber-500/30 rounded-xl text-xs text-amber-300 flex items-start gap-2.5"
                  >
                    <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                    <span>{warn}</span>
                  </div>
                ))}
              </div>
            )}

            {/* Active Subscriptions List */}
            <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1">
              <h4 className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">
                Audited Services ({data.active_subscriptions.length})
              </h4>
              {data.active_subscriptions.length === 0 ? (
                <div className="text-center py-6 text-sm text-zinc-500">
                  No recurring subscriptions tagged yet. Toggle &quot;Recurring Subscription&quot; when adding recurring bills!
                </div>
              ) : (
                data.active_subscriptions.map((sub, idx) => (
                  <div
                    key={idx}
                    className="p-3 bg-zinc-900/40 hover:bg-zinc-900/70 transition-all rounded-xl border border-zinc-800/80 space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-semibold text-sm text-zinc-200">
                          {sub.merchant}
                        </div>
                        <div className="text-[11px] text-zinc-500">
                          {sub.category_name} • {sub.frequency}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-bold text-sm text-zinc-100">
                          {formatCurrency(sub.estimated_amount)}
                          <span className="text-[10px] text-zinc-500 font-normal"> / {sub.frequency}</span>
                        </div>
                        <div className="text-[10px] text-zinc-400">
                          {formatCurrency(sub.annual_cost)} / year
                        </div>
                      </div>
                    </div>

                    {sub.optimization_tip && (
                      <div className="text-xs text-purple-300 bg-purple-950/30 p-2 rounded-lg border border-purple-500/20">
                        💡 {sub.optimization_tip}
                      </div>
                    )}
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
            Close Audit
          </Button>
        </div>
      </div>
    </Dialog>
  );
}
