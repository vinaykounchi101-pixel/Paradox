"use client";

import React, { useEffect, useState } from "react";
import { motion } from "framer-motion";
import {
  Trophy,
  Flame,
  ShieldCheck,
  Crosshair,
  Award,
  Sparkles,
  Lock,
  CheckCircle2,
  Loader2,
  RefreshCw,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { aiApi, AchievementsResponse, AchievementBadge } from "@/lib/api/ai";

export const AchievementsCard: React.FC = () => {
  const [data, setData] = useState<AchievementsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchAchievements = async () => {
    try {
      setIsLoading(true);
      const res = await aiApi.getAchievements();
      setData(res.data);
    } catch {
      // Fail gracefully
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAchievements();
  }, []);

  if (isLoading) {
    return (
      <Card className="border-zinc-800 bg-zinc-950/60 p-6 flex items-center justify-center min-h-[160px]">
        <div className="flex items-center gap-2 text-zinc-400 text-xs">
          <Loader2 className="w-4 h-4 animate-spin text-amber-400" />
          <span>Tracking financial achievements...</span>
        </div>
      </Card>
    );
  }

  if (!data) return null;

  const renderBadgeIcon = (iconName: string, isUnlocked: boolean) => {
    const className = `w-4 h-4 ${isUnlocked ? "text-amber-400" : "text-zinc-500"}`;
    switch (iconName) {
      case "ShieldCheck":
        return <ShieldCheck className={className} />;
      case "Crosshair":
        return <Crosshair className={className} />;
      case "Flame":
        return <Flame className={className} />;
      case "Award":
      default:
        return <Award className={className} />;
    }
  };

  const getTierBadge = (tier: string) => {
    switch (tier) {
      case "diamond":
        return "bg-cyan-500/20 text-cyan-300 border-cyan-500/30";
      case "gold":
        return "bg-amber-500/20 text-amber-300 border-amber-500/30";
      case "silver":
        return "bg-zinc-400/20 text-zinc-300 border-zinc-500/30";
      case "bronze":
      default:
        return "bg-amber-900/30 text-amber-400 border-amber-800/40";
    }
  };

  return (
    <Card className="border-zinc-800 bg-zinc-950/60 overflow-hidden relative">
      <CardHeader className="pb-3 border-b border-zinc-800/80">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-400">
              <Trophy className="w-4 h-4" />
            </div>
            <div>
              <CardTitle className="text-sm font-semibold text-zinc-100 flex items-center gap-2">
                Discipline Streaks & Achievements
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1">
                  <Flame className="w-3 h-3 text-amber-400 fill-amber-400" />
                  {data.active_streak_days} Day Streak
                </span>
              </CardTitle>
              <CardDescription className="text-xs text-zinc-400">
                {data.total_unlocked} of {data.badges.length} badges unlocked this month
              </CardDescription>
            </div>
          </div>

          <button
            type="button"
            onClick={fetchAchievements}
            className="p-1.5 rounded-md hover:bg-zinc-800 text-zinc-500 hover:text-zinc-300 transition cursor-pointer"
            title="Refresh achievements"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
        </div>
      </CardHeader>

      <CardContent className="pt-4 space-y-4">
        {/* Badges Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-2.5">
          {data.badges.map((badge: AchievementBadge) => (
            <motion.div
              key={badge.id}
              whileHover={{ scale: 1.02 }}
              className={`p-3 rounded-xl border transition-all relative overflow-hidden flex flex-col justify-between space-y-2 ${
                badge.is_unlocked
                  ? "bg-zinc-900/80 border-amber-500/30 shadow-sm"
                  : "bg-zinc-900/40 border-zinc-800 opacity-75"
              }`}
            >
              <div className="flex items-center justify-between">
                <div
                  className={`p-1.5 rounded-lg border ${
                    badge.is_unlocked
                      ? "bg-amber-500/10 border-amber-500/30"
                      : "bg-zinc-800 border-zinc-700"
                  }`}
                >
                  {renderBadgeIcon(badge.icon, badge.is_unlocked)}
                </div>
                <span
                  className={`text-[9px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded border capitalize ${getTierBadge(
                    badge.tier
                  )}`}
                >
                  {badge.tier}
                </span>
              </div>

              <div>
                <h4 className="text-xs font-semibold text-zinc-200 flex items-center gap-1">
                  {badge.title}
                  {badge.is_unlocked && (
                    <CheckCircle2 className="w-3 h-3 text-emerald-400 inline" />
                  )}
                </h4>
                <p className="text-[10px] text-zinc-400 line-clamp-2 mt-0.5">
                  {badge.description}
                </p>
              </div>

              {/* Progress Bar */}
              <div className="space-y-1 pt-1">
                <div className="flex justify-between text-[9px] text-zinc-400 font-medium">
                  <span>Progress</span>
                  <span className="text-zinc-300">{badge.progress_label}</span>
                </div>
                <div className="h-1.5 w-full bg-zinc-800 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      badge.is_unlocked ? "bg-amber-400" : "bg-zinc-600"
                    }`}
                    style={{ width: `${badge.progress}%` }}
                  />
                </div>
              </div>
            </motion.div>
          ))}
        </div>

        {/* Motivation quote banner */}
        {data.motivation_quote && (
          <div className="p-3 rounded-lg border border-zinc-800 bg-zinc-900/60 flex items-center gap-2.5 text-xs text-zinc-300">
            <Sparkles className="w-4 h-4 text-amber-400 shrink-0" />
            <span className="italic">&ldquo;{data.motivation_quote}&rdquo;</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
