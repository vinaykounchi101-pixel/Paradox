"use client";

import React, { useState } from "react";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useCurrency } from "@/features/auth/context/CurrencyContext";
import { useCategories } from "@/features/categories/hooks/useCategories";
import { aiApi, SimulatePurchaseResponse } from "@/lib/api/ai";
import { useToast } from "@/components/ui/toast";
import { Sparkles, AlertTriangle, CheckCircle2, XCircle, ArrowRight, Loader2, DollarSign } from "lucide-react";

interface PurchaseSimulatorDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onAddAsExpense?: (amount: number, description?: string, categoryId?: string) => void;
}

export function PurchaseSimulatorDialog({
  isOpen,
  onClose,
  onAddAsExpense,
}: PurchaseSimulatorDialogProps) {
  const { formatCurrency, currencySymbol } = useCurrency();
  const { data: categories = [] } = useCategories();
  const { error: toastError } = useToast();

  const [amount, setAmount] = useState<string>("");
  const [description, setDescription] = useState<string>("");
  const [categoryName, setCategoryName] = useState<string>("");
  const [isSimulating, setIsSimulating] = useState<boolean>(false);
  const [simulation, setSimulation] = useState<SimulatePurchaseResponse | null>(null);

  const handleSimulate = async (e: React.FormEvent) => {
    e.preventDefault();
    const num = parseFloat(amount);
    if (isNaN(num) || num <= 0) {
      toastError("Please enter a valid purchase amount greater than zero.");
      return;
    }

    setIsSimulating(true);
    try {
      const res = await aiApi.simulatePurchase({
        amount: num,
        description: description.trim() || undefined,
        category_name: categoryName || undefined,
      });
      setSimulation(res.data);
    } catch (err: any) {
      toastError(err?.message || "Could not evaluate purchase at this moment.");
    } finally {
      setIsSimulating(false);
    }
  };

  const handleCreateExpense = () => {
    const num = parseFloat(amount);
    const cat = categories.find((c) => c.name.toLowerCase() === categoryName.toLowerCase());
    if (onAddAsExpense && !isNaN(num)) {
      onAddAsExpense(num, description, cat?.id);
    }
    onClose();
  };

  return (
    <Dialog isOpen={isOpen} onClose={onClose} title="🤔 Can I Afford This? (Purchase Simulator)">
      <div className="space-y-6">
        <p className="text-sm text-zinc-400">
          Test an impulse or planned purchase before spending. Paradox calculates your remaining
          budget, daily safe allowance impact, and month-end trajectory.
        </p>

        <form onSubmit={handleSimulate} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-zinc-300 uppercase tracking-wider mb-1.5">
                Amount ({currencySymbol}) *
              </label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400 font-medium">
                  {currencySymbol}
                </span>
                <Input
                  type="number"
                  step="any"
                  required
                  placeholder="2500"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-zinc-300 uppercase tracking-wider mb-1.5">
                Category (Optional)
              </label>
              <select
                value={categoryName}
                onChange={(e) => setCategoryName(e.target.value)}
                className="w-full bg-zinc-900/80 border border-zinc-700/80 text-zinc-100 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
              >
                <option value="">Select Category...</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.name}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-300 uppercase tracking-wider mb-1.5">
              Item or Merchant Description
            </label>
            <Input
              type="text"
              placeholder="e.g. Sony WH-1000XM5 Headphones, Weekend Dining"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <Button
            type="submit"
            disabled={isSimulating || !amount}
            className="w-full bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-medium py-2.5 rounded-lg flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/20"
          >
            {isSimulating ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Calculating Financial Impact...
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4" />
                Simulate Purchase Decision
              </>
            )}
          </Button>
        </form>

        {simulation && (
          <div className="space-y-4 pt-4 border-t border-zinc-800 animate-in fade-in-50 duration-300">
            {/* Verdict Banner */}
            <div
              className={`p-4 rounded-xl border flex items-start gap-3.5 ${
                simulation.verdict === "safe"
                  ? "bg-emerald-950/30 border-emerald-500/30 text-emerald-300"
                  : simulation.verdict === "caution"
                  ? "bg-amber-950/30 border-amber-500/30 text-amber-300"
                  : "bg-rose-950/30 border-rose-500/30 text-rose-300"
              }`}
            >
              <div className="mt-0.5">
                {simulation.verdict === "safe" ? (
                  <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                ) : simulation.verdict === "caution" ? (
                  <AlertTriangle className="w-5 h-5 text-amber-400" />
                ) : (
                  <XCircle className="w-5 h-5 text-rose-400" />
                )}
              </div>
              <div className="space-y-1">
                <div className="font-semibold text-sm tracking-wide">
                  {simulation.headline}
                </div>
                <p className="text-xs leading-relaxed text-zinc-300">
                  {simulation.advice}
                </p>
              </div>
            </div>

            {/* Metrics Grid */}
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 bg-zinc-900/60 rounded-lg border border-zinc-800/80">
                <div className="text-zinc-400 mb-1">Remaining Buffer (Before)</div>
                <div className="text-sm font-semibold text-zinc-200">
                  {formatCurrency(simulation.current_remaining_budget)}
                </div>
                <div className="text-[10px] text-zinc-500 mt-0.5">
                  Daily Safe: {formatCurrency(simulation.safe_to_spend_daily_before)}/day
                </div>
              </div>

              <div className="p-3 bg-zinc-900/60 rounded-lg border border-zinc-800/80">
                <div className="text-zinc-400 mb-1">Remaining Buffer (After)</div>
                <div
                  className={`text-sm font-semibold ${
                    Number(simulation.projected_remaining_budget) < 0
                      ? "text-rose-400"
                      : "text-zinc-200"
                  }`}
                >
                  {formatCurrency(simulation.projected_remaining_budget)}
                </div>
                <div className="text-[10px] text-zinc-500 mt-0.5">
                  Daily Safe: {formatCurrency(simulation.safe_to_spend_daily_after)}/day
                </div>
              </div>
            </div>

            {simulation.category_impact && (
              <div className="text-xs text-zinc-400 bg-zinc-900/40 p-2.5 rounded-lg border border-zinc-800/50">
                📌 <strong className="text-zinc-300">Category Context:</strong>{" "}
                {simulation.category_impact}
              </div>
            )}

            {/* 1-Click Action */}
            {onAddAsExpense && (
              <div className="pt-2 flex gap-3">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={onClose}
                  className="flex-1 border border-zinc-700 text-zinc-300"
                >
                  Close
                </Button>
                <Button
                  type="button"
                  onClick={handleCreateExpense}
                  className="flex-1 bg-emerald-600 hover:bg-emerald-500 text-white flex items-center justify-center gap-1.5"
                >
                  Add as Expense <ArrowRight className="w-4 h-4" />
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </Dialog>
  );
}
