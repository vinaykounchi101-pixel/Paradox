"use client";

import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Sparkles,
  ChevronLeft,
  ChevronRight,
  Share2,
  X,
  Trophy,
  Flame,
  Wallet,
  Loader2,
} from "lucide-react";
import { useCurrency } from "@/features/auth/context/CurrencyContext";
import { aiApi, MonthlyWrappedResponse } from "@/lib/api/ai";
import { Button } from "@/components/ui/button";

interface MonthlyWrappedModalProps {
  isOpen: boolean;
  onClose: () => void;
  month?: string; // e.g. "2026-09"
}

export const MonthlyWrappedModal: React.FC<MonthlyWrappedModalProps> = ({
  isOpen,
  onClose,
  month,
}) => {
  const { formatCurrency } = useCurrency();
  const [currentSlide, setCurrentSlide] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [wrappedData, setWrappedData] = useState<MonthlyWrappedResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const totalSlides = 5;

  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    setCurrentSlide(0);
    setLoading(true);
    setError(null);

    aiApi
      .getMonthlyWrapped(month)
      .then((res) => {
        if (isMounted) {
          setWrappedData(res.data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err?.response?.data?.detail || "Could not generate your Monthly Wrapped.");
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen, month]);

  const handleNext = () => {
    if (currentSlide < totalSlides - 1) {
      setCurrentSlide((prev) => prev + 1);
    } else {
      onClose();
    }
  };

  const handlePrev = () => {
    if (currentSlide > 0) {
      setCurrentSlide((prev) => prev - 1);
    }
  };

  const handleShare = () => {
    if (!wrappedData) return;
    const shareText = `My ${wrappedData.month} Paradox Wrapped:\n` +
      `🔥 Archetype: ${wrappedData.archetype_title}\n` +
      `💰 Total Spent: ${formatCurrency(wrappedData.total_spent)}\n` +
      `🎯 Longest Discipline Streak: ${wrappedData.active_streak_days} days\n` +
      `#ParadoxApp #FinancialWrapped`;

    if (navigator.clipboard) {
      navigator.clipboard.writeText(shareText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
      {/* Backdrop */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 bg-black/85 backdrop-blur-md"
      />

      {/* Main Wrapped Card */}
      <motion.div
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        exit={{ scale: 0.9, opacity: 0, y: 20 }}
        transition={{ type: "spring", damping: 25, stiffness: 300 }}
        className="relative z-10 w-full max-w-md h-[580px] rounded-3xl overflow-hidden shadow-2xl border border-zinc-800 bg-gradient-to-br from-zinc-950 via-zinc-900 to-zinc-950 flex flex-col justify-between"
      >
        {/* Top Story Progress Bars */}
        <div className="p-4 pb-2 z-20">
          <div className="flex items-center gap-1.5 mb-3">
            {Array.from({ length: totalSlides }).map((_, idx) => (
              <div
                key={idx}
                className="h-1 flex-1 rounded-full bg-zinc-800 overflow-hidden"
              >
                <div
                  className={`h-full transition-all duration-300 ${
                    idx <= currentSlide ? "bg-gradient-to-r from-indigo-400 to-purple-400" : "w-0"
                  }`}
                  style={{ width: idx <= currentSlide ? "100%" : "0%" }}
                />
              </div>
            ))}
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                Paradox Wrapped
              </span>
              <span className="text-xs font-semibold text-zinc-400">
                {wrappedData?.month || "This Month"}
              </span>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1 rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800 transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Story Body Content */}
        <div className="flex-1 relative flex items-center justify-center p-6 text-center overflow-hidden">
          {loading ? (
            <div className="flex flex-col items-center justify-center space-y-3">
              <Loader2 className="w-8 h-8 animate-spin text-indigo-400" />
              <p className="text-xs text-zinc-400">Synthesizing your financial story...</p>
            </div>
          ) : error || !wrappedData ? (
            <div className="space-y-3 max-w-xs">
              <Trophy className="w-10 h-10 text-amber-400 mx-auto opacity-70" />
              <h3 className="text-sm font-semibold text-zinc-200">Not Enough Data Yet</h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                {error || "Add at least a couple expenses this month to unlock your personalized Paradox Wrapped story!"}
              </p>
            </div>
          ) : (
            <AnimatePresence mode="wait">
              {/* Slide 0: Total Spend & Velocity */}
              {currentSlide === 0 && (
                <motion.div
                  key="slide-0"
                  initial={{ opacity: 0, scale: 0.85, y: 15 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.85, y: -15 }}
                  transition={{ duration: 0.3 }}
                  className="space-y-5 w-full"
                >
                  <div className="w-16 h-16 mx-auto rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/30">
                    <Wallet className="w-8 h-8 text-white" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                      Total Capital Deployed
                    </p>
                    <h2 className="text-4xl sm:text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-white via-zinc-100 to-indigo-200 mt-2 font-mono">
                      {formatCurrency(wrappedData.total_spent)}
                    </h2>
                  </div>
                  <div className="inline-block px-4 py-2 rounded-xl bg-zinc-900/80 border border-zinc-800">
                    <p className="text-xs text-zinc-300">
                      Across <strong className="text-indigo-300">{wrappedData.total_transactions}</strong> individual transactions
                    </p>
                  </div>
                  <p className="text-xs text-zinc-500 italic max-w-xs mx-auto">
                    &ldquo;Every transaction tells a chapter of your financial year.&rdquo;
                  </p>
                </motion.div>
              )}

              {/* Slide 1: Top Categories */}
              {currentSlide === 1 && (
                <motion.div
                  key="slide-1"
                  initial={{ opacity: 0, scale: 0.85, y: 15 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.85, y: -15 }}
                  transition={{ duration: 0.3 }}
                  className="space-y-5 w-full text-left"
                >
                  <div className="text-center">
                    <span className="text-2xl">🥇</span>
                    <h3 className="text-xl font-bold text-white mt-1">Your Primary Outflows</h3>
                    <p className="text-xs text-zinc-400">Where your funds gravitated most</p>
                  </div>
                  <div className="space-y-3 pt-2">
                    {wrappedData.top_categories.slice(0, 3).map((cat, idx) => (
                      <div
                        key={cat.category_name}
                        className="p-3 rounded-2xl bg-zinc-900/90 border border-zinc-800 flex items-center justify-between"
                      >
                        <div className="flex items-center gap-3">
                          <span className="w-6 h-6 rounded-full bg-indigo-500/20 text-indigo-300 text-xs font-bold flex items-center justify-center border border-indigo-500/30">
                            #{idx + 1}
                          </span>
                          <div>
                            <p className="text-xs font-semibold text-zinc-100">{cat.category_name}</p>
                            <div className="w-24 bg-zinc-800 h-1.5 rounded-full mt-1.5 overflow-hidden">
                              <div
                                className="h-full bg-indigo-500 rounded-full"
                                style={{ width: `${Math.min(cat.percentage, 100)}%` }}
                              />
                            </div>
                          </div>
                        </div>
                        <div className="text-right">
                          <p className="text-xs font-bold text-zinc-100 font-mono">
                            {formatCurrency(cat.amount)}
                          </p>
                          <p className="text-[10px] text-zinc-400">{cat.percentage}%</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}

              {/* Slide 2: Biggest Splurge */}
              {currentSlide === 2 && (
                <motion.div
                  key="slide-2"
                  initial={{ opacity: 0, scale: 0.85, y: 15 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.85, y: -15 }}
                  transition={{ duration: 0.3 }}
                  className="space-y-5 w-full"
                >
                  <div className="w-16 h-16 mx-auto rounded-2xl bg-gradient-to-tr from-amber-500 to-rose-500 flex items-center justify-center shadow-lg shadow-amber-500/20">
                    <Sparkles className="w-8 h-8 text-white" />
                  </div>
                  <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-amber-400">
                      The Peak Outflow
                    </span>
                    <h3 className="text-lg font-bold text-white mt-1">Biggest Splurge of the Month</h3>
                  </div>

                  {wrappedData.biggest_splurge ? (
                    <div className="p-5 rounded-2xl bg-gradient-to-b from-amber-500/10 to-zinc-900 border border-amber-500/30 space-y-2">
                      <p className="text-base font-bold text-amber-200">
                        {wrappedData.biggest_splurge.description}
                      </p>
                      <p className="text-3xl font-extrabold text-white font-mono">
                        {formatCurrency(wrappedData.biggest_splurge.amount)}
                      </p>
                      <p className="text-[11px] text-zinc-400">
                        Date: {wrappedData.biggest_splurge.date} ({wrappedData.biggest_splurge.category_name})
                      </p>
                    </div>
                  ) : (
                    <div className="p-4 rounded-xl bg-zinc-900 border border-zinc-800 text-xs text-zinc-400">
                      No standalone major splurge recorded this month. Consistent pacing!
                    </div>
                  )}

                  <p className="text-xs text-zinc-400 max-w-xs mx-auto">
                    Splurges bring joy when budgeted with intention.
                  </p>
                </motion.div>
              )}

              {/* Slide 3: Discipline Streak */}
              {currentSlide === 3 && (
                <motion.div
                  key="slide-3"
                  initial={{ opacity: 0, scale: 0.85, y: 15 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.85, y: -15 }}
                  transition={{ duration: 0.3 }}
                  className="space-y-5 w-full"
                >
                  <div className="w-16 h-16 mx-auto rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-600 flex items-center justify-center shadow-lg shadow-emerald-500/25">
                    <Flame className="w-8 h-8 text-white" />
                  </div>

                  <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
                      Discipline Milestone
                    </span>
                    <h2 className="text-4xl font-extrabold text-white mt-2 font-mono">
                      {wrappedData.active_streak_days} Days
                    </h2>
                    <p className="text-xs text-zinc-400 mt-1">
                      Longest continuous run within daily safe-spending limits
                    </p>
                  </div>

                  {wrappedData.personalized_recap?.length > 0 && (
                    <div className="p-4 rounded-2xl bg-zinc-900/90 border border-zinc-800 text-xs text-zinc-300 leading-relaxed text-left space-y-1">
                      {wrappedData.personalized_recap.map((tip, idx) => (
                        <p key={idx} className="flex items-start gap-1.5">
                          <span>💡</span>
                          <span>{tip}</span>
                        </p>
                      ))}
                    </div>
                  )}
                </motion.div>
              )}

              {/* Slide 4: Personality Archetype Reveal */}
              {currentSlide === 4 && (
                <motion.div
                  key="slide-4"
                  initial={{ opacity: 0, scale: 0.85, y: 15 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.85, y: -15 }}
                  transition={{ duration: 0.3 }}
                  className="space-y-4 w-full"
                >
                  <div className="w-20 h-20 mx-auto rounded-3xl bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center shadow-xl shadow-purple-500/30 text-3xl">
                    🌟
                  </div>

                  <div>
                    <span className="text-[10px] font-bold uppercase tracking-widest text-indigo-400">
                      Your Financial Archetype
                    </span>
                    <h2 className="text-2xl font-black text-white mt-1">
                      {wrappedData.archetype_title}
                    </h2>
                  </div>

                  <div className="p-4 rounded-2xl bg-indigo-500/10 border border-indigo-500/30 text-xs text-indigo-200 leading-relaxed text-left">
                    {wrappedData.archetype_description}
                  </div>

                  <div className="pt-2 flex items-center justify-center gap-2">
                    <Button
                      size="sm"
                      onClick={handleShare}
                      className="cursor-pointer text-xs gap-1.5 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white shadow-md"
                    >
                      <Share2 className="w-3.5 h-3.5" />
                      {copied ? "Copied to Clipboard!" : "Share Wrapped"}
                    </Button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          )}
        </div>

        {/* Story Bottom Navigation Controls */}
        <div className="p-4 pt-2 border-t border-zinc-800/80 flex items-center justify-between z-20">
          <Button
            variant="ghost"
            size="sm"
            onClick={handlePrev}
            disabled={currentSlide === 0 || loading}
            className="cursor-pointer text-xs text-zinc-400 hover:text-white"
          >
            <ChevronLeft className="w-4 h-4 mr-1" /> Back
          </Button>

          <span className="text-[11px] text-zinc-500 font-mono">
            {currentSlide + 1} / {totalSlides}
          </span>

          <Button
            variant="secondary"
            size="sm"
            onClick={handleNext}
            disabled={loading}
            className="cursor-pointer text-xs bg-indigo-600 hover:bg-indigo-500 text-white"
          >
            {currentSlide === totalSlides - 1 ? "Done" : "Next"}
            {currentSlide < totalSlides - 1 && <ChevronRight className="w-4 h-4 ml-1" />}
          </Button>
        </div>
      </motion.div>
    </div>
  );
};
