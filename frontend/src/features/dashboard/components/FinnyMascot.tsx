"use client";

import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Sparkles, MessageCircle, X, Flame, Loader2, Zap, Heart, PartyPopper } from "lucide-react";
import { aiApi, VibeCheckResponse } from "@/lib/api/ai";

interface FinnyMascotProps {
  healthScore?: number;
  healthStatus?: "excellent" | "good" | "needs_attention";
  headline?: string;
}

interface FloatingEmoji {
  id: number;
  emoji: string;
  x: number;
  y: number;
}

export const FinnyMascot: React.FC<FinnyMascotProps> = ({
  healthScore = 75,
  healthStatus = "good",
  headline,
}) => {
  const [showBubble, setShowBubble] = useState(false);
  const [roastMode, setRoastMode] = useState(false);
  const [vibeData, setVibeData] = useState<VibeCheckResponse | null>(null);
  const [loadingVibe, setLoadingVibe] = useState(false);
  const [pokeCount, setPokeCount] = useState(0);
  const [floatingEmojis, setFloatingEmojis] = useState<FloatingEmoji[]>([]);
  const [isWiggling, setIsWiggling] = useState(false);

  // Fetch AI Vibe Check on bubble open or roastMode toggle
  useEffect(() => {
    if (!showBubble) return;

    let isMounted = true;
    setLoadingVibe(true);

    aiApi
      .getVibeCheck(roastMode)
      .then((res) => {
        if (isMounted) {
          setVibeData(res.data);
          setLoadingVibe(false);
        }
      })
      .catch(() => {
        if (isMounted) setLoadingVibe(false);
      });

    return () => {
      isMounted = false;
    };
  }, [showBubble, roastMode]);

  // Determine mood based on healthScore
  const getMood = () => {
    if (healthScore >= 80) {
      return {
        type: "joyful",
        label: "Thriving",
        emoji: "🧘",
        bgColor: "from-emerald-500/25 via-teal-500/15 to-emerald-500/10",
        borderColor: "border-emerald-500/50",
        glowColor: "rgba(16, 185, 129, 0.35)",
        eyeType: "happy",
        mouthType: "bigSmile",
        tip: headline || "Financial discipline is on point! Keep this pace to smash your monthly savings goal. 🎯",
      };
    } else if (healthScore >= 60) {
      return {
        type: "calm",
        label: "Balanced",
        emoji: "✨",
        bgColor: "from-indigo-500/25 via-purple-500/15 to-indigo-500/10",
        borderColor: "border-indigo-500/50",
        glowColor: "rgba(99, 102, 241, 0.35)",
        eyeType: "normal",
        mouthType: "smile",
        tip: headline || "Healthy pacing! Watch out for weekend dining spikes to protect your buffer. ☕",
      };
    } else if (healthScore >= 40) {
      return {
        type: "alert",
        label: "Caution",
        emoji: "⚡",
        bgColor: "from-amber-500/25 via-orange-500/15 to-amber-500/10",
        borderColor: "border-amber-500/50",
        glowColor: "rgba(245, 158, 11, 0.35)",
        eyeType: "wide",
        mouthType: "flat",
        tip: headline || "Spending velocity is picking up. Tap 'Safe-to-Spend' to check your daily allowance. ⚠️",
      };
    } else {
      return {
        type: "stressed",
        label: "Critical",
        emoji: "🚨",
        bgColor: "from-rose-500/25 via-red-500/15 to-rose-500/10",
        borderColor: "border-rose-500/50",
        glowColor: "rgba(244, 63, 94, 0.35)",
        eyeType: "worried",
        mouthType: "frown",
        tip: headline || "Budget threshold exceeded. Use 'Leak Hunter' to eliminate recurring micro-spending. 🔥",
      };
    }
  };

  const mood = getMood();

  // Micro-interaction: Poking / Petting Finny
  const handlePoke = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsWiggling(true);
    setPokeCount((prev) => prev + 1);

    const burstEmojis = ["✨", "💖", "💎", "🚀", "💸", "🎉", "🧘"];
    const randomEmoji = burstEmojis[Math.floor(Math.random() * burstEmojis.length)];
    const newEmoji: FloatingEmoji = {
      id: Date.now() + Math.random(),
      emoji: randomEmoji,
      x: (Math.random() - 0.5) * 40,
      y: -20 - Math.random() * 20,
    };

    setFloatingEmojis((prev) => [...prev.slice(-6), newEmoji]);
    setTimeout(() => {
      setFloatingEmojis((prev) => prev.filter((item) => item.id !== newEmoji.id));
    }, 1200);

    setTimeout(() => setIsWiggling(false), 600);
    setShowBubble(true);
  };

  return (
    <div className="relative inline-flex items-center" style={{ perspective: 1000 }}>
      {/* Floating Emojis on micro-interaction poke */}
      <AnimatePresence>
        {floatingEmojis.map((item) => (
          <motion.span
            key={item.id}
            initial={{ opacity: 1, scale: 0.5, x: item.x, y: 0 }}
            animate={{ opacity: 0, scale: 1.5, x: item.x * 1.5, y: item.y - 30 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 1, ease: "easeOut" }}
            className="absolute -top-3 left-3 pointer-events-none z-50 text-base select-none"
          >
            {item.emoji}
          </motion.span>
        ))}
      </AnimatePresence>

      {/* Interactive Mascot Trigger with 3D Depth */}
      <motion.button
        type="button"
        whileHover={{ scale: 1.06, y: -2 }}
        whileTap={{ scale: 0.94 }}
        animate={
          isWiggling
            ? {
                rotate: [0, -14, 14, -10, 10, -4, 4, 0],
                scale: [1, 1.12, 0.96, 1.05, 1],
              }
            : {}
        }
        transition={{ duration: 0.5 }}
        onClick={handlePoke}
        className={`group relative flex items-center gap-2.5 px-3 py-1.5 rounded-full border bg-gradient-to-r ${mood.bgColor} ${mood.borderColor} backdrop-blur-md cursor-pointer transition-all shadow-md`}
        style={{
          boxShadow: `0 4px 20px ${mood.glowColor}, inset 0 1px 1px rgba(255, 255, 255, 0.2)`,
          transformStyle: "preserve-3d",
        }}
        title="Finny - Tap to pet & check your financial pulse!"
      >
        {/* 3D Animated Finny Face */}
        <motion.div
          animate={{
            y: [0, -3, 0],
            rotateZ: [-1, 1, -1],
          }}
          transition={{ repeat: Infinity, duration: 3, ease: "easeInOut" }}
          className="w-7 h-7 relative flex items-center justify-center filter drop-shadow-md"
        >
          <svg viewBox="0 0 36 36" className="w-full h-full">
            {/* Robot/Critter 3D Outer Halo */}
            <circle cx="18" cy="18" r="17" fill="none" stroke="currentColor" strokeWidth="1" className="text-white/10" />

            {/* Robot Head Body */}
            <circle cx="18" cy="18" r="15" fill="#18181b" stroke="currentColor" strokeWidth="2" className="text-zinc-600" />

            {/* 3D Antenna / Star with pulse */}
            <line x1="18" y1="2" x2="18" y2="5" stroke="#818cf8" strokeWidth="2" strokeLinecap="round" />
            <circle cx="18" cy="2" r="2.5" fill="#a5b4fc" className="animate-pulse" />

            {/* Eyes based on mood */}
            {mood.eyeType === "happy" && (
              <>
                <path d="M10 16 Q13 11 16 16" fill="none" stroke="#34d399" strokeWidth="2.5" strokeLinecap="round" />
                <path d="M20 16 Q23 11 26 16" fill="none" stroke="#34d399" strokeWidth="2.5" strokeLinecap="round" />
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
                <path d="M10 14 Q13 18 16 14" fill="none" stroke="#f43f5e" strokeWidth="2.5" strokeLinecap="round" />
                <path d="M20 14 Q23 18 26 14" fill="none" stroke="#f43f5e" strokeWidth="2.5" strokeLinecap="round" />
              </>
            )}

            {/* Cheeks */}
            <circle cx="9" cy="20" r="1.5" fill="#f472b6" opacity="0.7" />
            <circle cx="27" cy="20" r="1.5" fill="#f472b6" opacity="0.7" />

            {/* Mouth */}
            {mood.mouthType === "bigSmile" && (
              <path d="M12 21 Q18 28 24 21" fill="none" stroke="#34d399" strokeWidth="2.2" strokeLinecap="round" />
            )}
            {mood.mouthType === "smile" && (
              <path d="M13 22 Q18 26 23 22" fill="none" stroke="#818cf8" strokeWidth="2" strokeLinecap="round" />
            )}
            {mood.mouthType === "flat" && (
              <line x1="14" y1="23" x2="22" y2="23" stroke="#fbbf24" strokeWidth="2" strokeLinecap="round" />
            )}
            {mood.mouthType === "frown" && (
              <path d="M13 24 Q18 19 23 24" fill="none" stroke="#f43f5e" strokeWidth="2.2" strokeLinecap="round" />
            )}
          </svg>
        </motion.div>

        {/* Mascot Label & Mood Emoji */}
        <div className="flex flex-col text-left">
          <div className="flex items-center gap-1">
            <span className="text-[10px] uppercase font-bold tracking-wider text-zinc-400 leading-none">Finny</span>
            <span className="text-[11px] leading-none">{mood.emoji}</span>
          </div>
          <span className="text-xs font-semibold text-zinc-100 leading-tight flex items-center gap-1">
            {mood.label}
          </span>
        </div>

        <MessageCircle className="w-3.5 h-3.5 text-zinc-400 group-hover:text-zinc-200 transition ml-0.5" />
      </motion.button>

      {/* Finny Speech Bubble Overlay with 3D Pop & Micro-Interactions */}
      <AnimatePresence>
        {showBubble && (
          <motion.div
            initial={{ opacity: 0, y: 12, scale: 0.92, rotateX: -10 }}
            animate={{ opacity: 1, y: 0, scale: 1, rotateX: 0 }}
            exit={{ opacity: 0, y: 10, scale: 0.92, rotateX: -10 }}
            transition={{ type: "spring", damping: 20, stiffness: 300 }}
            className="absolute sm:left-0 sm:right-auto right-0 top-full mt-2.5 w-[calc(100vw-2.5rem)] sm:w-84 max-w-sm z-50 rounded-2xl border border-zinc-800/90 bg-zinc-950/95 p-4 shadow-2xl backdrop-blur-xl space-y-3"
            style={{
              boxShadow: "0 20px 40px rgba(0,0,0,0.6), 0 0 25px rgba(99,102,241,0.15)",
              transformOrigin: "top center",
            }}
          >
            {/* Header with Roast Mode Switch */}
            <div className="flex items-center justify-between pb-2 border-b border-zinc-800/80">
              <div className="flex items-center gap-1.5 text-xs font-bold text-indigo-300">
                <Sparkles className="w-3.5 h-3.5 text-indigo-400 animate-spin" style={{ animationDuration: "6s" }} />
                <span>Finny&apos;s Financial Pulse</span>
              </div>
              <div className="flex items-center gap-1.5">
                {/* Roast Mode Toggle */}
                <motion.button
                  type="button"
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={(e) => {
                    e.stopPropagation();
                    setRoastMode(!roastMode);
                  }}
                  className={`px-2 py-0.5 rounded-full text-[10px] font-bold transition flex items-center gap-1 cursor-pointer border ${
                    roastMode
                      ? "bg-rose-500/25 text-rose-300 border-rose-500/50 shadow-sm shadow-rose-500/30"
                      : "bg-zinc-800/80 text-zinc-400 border-zinc-700 hover:text-zinc-200"
                  }`}
                  title={roastMode ? "Switch to Gentle Coach Mode" : "Switch to Hinglish Roast Mode 🔥"}
                >
                  <Flame className={`w-3 h-3 ${roastMode ? "text-rose-400 animate-bounce" : "text-zinc-400"}`} />
                  <span>{roastMode ? "Roast 🔥" : "Roast"}</span>
                </motion.button>
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowBubble(false);
                  }}
                  className="text-zinc-500 hover:text-zinc-300 p-1 rounded-md hover:bg-zinc-800 transition cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Finny Commentary & Live Vibe */}
            {loadingVibe ? (
              <div className="py-5 flex flex-col items-center justify-center gap-2 text-xs text-zinc-400">
                <Loader2 className="w-4 h-4 animate-spin text-indigo-400" />
                <span className="text-[11px]">Reading spending vibes & burn rate...</span>
              </div>
            ) : vibeData ? (
              <div className="space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-zinc-300 flex items-center gap-1.5">
                    <span className="text-lg">{vibeData.vibe_emoji}</span>
                    <span>{vibeData.vibe_title}</span>
                  </span>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-zinc-900 border border-zinc-800 text-zinc-400 capitalize">
                    Burn: <strong className="text-zinc-200">{vibeData.burn_rate_status}</strong> ({vibeData.budget_percent_consumed}%)
                  </span>
                </div>

                <motion.div
                  initial={{ opacity: 0, y: 4 }}
                  animate={{ opacity: 1, y: 0 }}
                  className={`p-3 rounded-xl border text-xs leading-relaxed ${
                    roastMode
                      ? "bg-rose-500/10 border-rose-500/30 text-rose-200 shadow-inner"
                      : "bg-indigo-500/10 border-indigo-500/25 text-zinc-200"
                  }`}
                >
                  {vibeData.roast_commentary}
                </motion.div>
              </div>
            ) : (
              <p className="text-xs text-zinc-300 leading-relaxed p-2 bg-zinc-900/50 rounded-xl border border-zinc-800/60">
                {mood.tip}
              </p>
            )}

            {/* Footer with XP / Poke interaction */}
            <div className="pt-2 border-t border-zinc-800/60 flex items-center justify-between text-[10px] text-zinc-400">
              <span className="flex items-center gap-1">
                <span>Health:</span>
                <strong className="text-zinc-200 font-mono">{healthScore}/100</strong>
              </span>
              <span className="text-zinc-500 flex items-center gap-1">
                <Heart className="w-2.5 h-2.5 text-pink-400 fill-pink-400" />
                <span>Petted {pokeCount}x</span>
              </span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
