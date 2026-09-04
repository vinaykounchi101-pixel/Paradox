"use client";

import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Sparkles, MessageCircle, X } from "lucide-react";

interface FinnyMascotProps {
  healthScore?: number;
  healthStatus?: "excellent" | "good" | "needs_attention";
  headline?: string;
}

export const FinnyMascot: React.FC<FinnyMascotProps> = ({
  healthScore = 75,
  healthStatus = "good",
  headline,
}) => {
  const [showBubble, setShowBubble] = useState(false);

  // Determine mood based on healthScore
  const getMood = () => {
    if (healthScore >= 80) {
      return {
        type: "joyful",
        label: "Thriving",
        bgColor: "from-emerald-500/20 to-teal-500/10",
        borderColor: "border-emerald-500/40",
        glowColor: "rgba(16, 185, 129, 0.25)",
        eyeType: "happy",
        mouthType: "bigSmile",
        tip: headline || "Financial discipline is on point! Keep this pace to smash your monthly savings goal.",
      };
    } else if (healthScore >= 60) {
      return {
        type: "calm",
        label: "Balanced",
        bgColor: "from-indigo-500/20 to-purple-500/10",
        borderColor: "border-indigo-500/40",
        glowColor: "rgba(99, 102, 241, 0.25)",
        eyeType: "normal",
        mouthType: "smile",
        tip: headline || "Healthy pacing! Watch out for weekend dining spikes to protect your buffer.",
      };
    } else if (healthScore >= 40) {
      return {
        type: "alert",
        label: "Caution",
        bgColor: "from-amber-500/20 to-orange-500/10",
        borderColor: "border-amber-500/40",
        glowColor: "rgba(245, 158, 11, 0.25)",
        eyeType: "wide",
        mouthType: "flat",
        tip: headline || "Spending velocity is picking up. Tap 'Safe-to-Spend' to check your daily allowance.",
      };
    } else {
      return {
        type: "stressed",
        label: "Critical",
        bgColor: "from-rose-500/20 to-red-500/10",
        borderColor: "border-rose-500/40",
        glowColor: "rgba(244, 63, 94, 0.25)",
        eyeType: "worried",
        mouthType: "frown",
        tip: headline || "Budget threshold exceeded. Use 'Leak Hunter' to eliminate recurring micro-spending.",
      };
    }
  };

  const mood = getMood();

  return (
    <div className="relative inline-flex items-center">
      {/* Interactive Mascot Trigger */}
      <motion.button
        type="button"
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
        onClick={() => setShowBubble(!showBubble)}
        className={`group relative flex items-center gap-2 px-3 py-1.5 rounded-full border bg-gradient-to-r ${mood.bgColor} ${mood.borderColor} backdrop-blur-md cursor-pointer transition-all shadow-sm`}
        style={{ boxShadow: `0 0 16px ${mood.glowColor}` }}
        title="Finny - Your Financial Mood Companion"
      >
        {/* Animated SVG Finny Face */}
        <motion.div
          animate={{ y: [0, -2, 0] }}
          transition={{ repeat: Infinity, duration: 2.5, ease: "easeInOut" }}
          className="w-7 h-7 relative flex items-center justify-center"
        >
          <svg viewBox="0 0 36 36" className="w-full h-full">
            {/* Robot/Critter Head */}
            <circle cx="18" cy="18" r="16" fill="#18181b" stroke="currentColor" strokeWidth="2" className="text-zinc-600" />
            
            {/* Antenna / Star */}
            <line x1="18" y1="2" x2="18" y2="6" stroke="#6366f1" strokeWidth="2" strokeLinecap="round" />
            <circle cx="18" cy="2" r="2" fill="#818cf8" />

            {/* Eyes */}
            {mood.eyeType === "happy" && (
              <>
                <path d="M10 16 Q13 12 16 16" fill="none" stroke="#34d399" strokeWidth="2.5" strokeLinecap="round" />
                <path d="M20 16 Q23 12 26 16" fill="none" stroke="#34d399" strokeWidth="2.5" strokeLinecap="round" />
              </>
            )}
            {mood.eyeType === "normal" && (
              <>
                <circle cx="13" cy="15" r="2.5" fill="#818cf8" />
                <circle cx="23" cy="15" r="2.5" fill="#818cf8" />
              </>
            )}
            {mood.eyeType === "wide" && (
              <>
                <circle cx="13" cy="15" r="3" fill="#fbbf24" />
                <circle cx="23" cy="15" r="3" fill="#fbbf24" />
              </>
            )}
            {mood.eyeType === "worried" && (
              <>
                <path d="M10 14 Q13 17 16 14" fill="none" stroke="#f43f5e" strokeWidth="2.5" strokeLinecap="round" />
                <path d="M20 14 Q23 17 26 14" fill="none" stroke="#f43f5e" strokeWidth="2.5" strokeLinecap="round" />
              </>
            )}

            {/* Cheeks */}
            <circle cx="9" cy="20" r="1.5" fill="#f472b6" opacity="0.6" />
            <circle cx="27" cy="20" r="1.5" fill="#f472b6" opacity="0.6" />

            {/* Mouth */}
            {mood.mouthType === "bigSmile" && (
              <path d="M12 21 Q18 28 24 21" fill="none" stroke="#34d399" strokeWidth="2" strokeLinecap="round" />
            )}
            {mood.mouthType === "smile" && (
              <path d="M13 22 Q18 26 23 22" fill="none" stroke="#818cf8" strokeWidth="2" strokeLinecap="round" />
            )}
            {mood.mouthType === "flat" && (
              <line x1="14" y1="23" x2="22" y2="23" stroke="#fbbf24" strokeWidth="2" strokeLinecap="round" />
            )}
            {mood.mouthType === "frown" && (
              <path d="M13 24 Q18 20 23 24" fill="none" stroke="#f43f5e" strokeWidth="2" strokeLinecap="round" />
            )}
          </svg>
        </motion.div>

        {/* Mascot Label */}
        <div className="flex flex-col text-left">
          <span className="text-[10px] uppercase font-bold tracking-wider text-zinc-400 leading-none">Finny</span>
          <span className="text-xs font-semibold text-zinc-100 leading-tight">{mood.label}</span>
        </div>

        <MessageCircle className="w-3.5 h-3.5 text-zinc-400 group-hover:text-zinc-200 transition ml-0.5" />
      </motion.button>

      {/* Finny Speech Bubble Overlay */}
      <AnimatePresence>
        {showBubble && (
          <motion.div
            initial={{ opacity: 0, y: 8, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.95 }}
            transition={{ duration: 0.15 }}
            className="absolute right-0 top-full mt-2 w-72 z-50 rounded-xl border border-zinc-800 bg-zinc-950/95 p-3.5 shadow-2xl backdrop-blur-md"
          >
            <div className="flex items-center justify-between pb-2 border-b border-zinc-800/80">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-indigo-300">
                <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                <span>Finny&apos;s Financial Pulse</span>
              </div>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setShowBubble(false);
                }}
                className="text-zinc-500 hover:text-zinc-300 p-0.5 rounded transition cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            <p className="pt-2 text-xs text-zinc-300 leading-relaxed">
              {mood.tip}
            </p>

            <div className="mt-3 pt-2 border-t border-zinc-800/60 flex items-center justify-between text-[10px] text-zinc-500">
              <span>Health Score: <strong className="text-zinc-300">{healthScore}/100</strong></span>
              <span className="capitalize">{mood.label} Mode</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
