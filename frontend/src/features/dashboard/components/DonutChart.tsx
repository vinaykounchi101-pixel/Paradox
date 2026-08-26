"use client";

import React, { useState } from "react";
import { motion } from "framer-motion";

interface ChartData {
  category_id: string;
  category_name: string;
  total: string;
  percentage: number;
}

interface DonutChartProps {
  data: ChartData[];
  totalSpent: string;
}

const COLORS = [
  "#6366f1", // Indigo
  "#a855f7", // Purple
  "#3b82f6", // Blue
  "#10b981", // Emerald
  "#f59e0b", // Amber
  "#f43f5e", // Rose
  "#06b6d4", // Cyan
  "#ec4899", // Pink
];

export const DonutChart: React.FC<DonutChartProps> = ({ data, totalSpent }) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  // Constants for Donut Geometry
  const radius = 50;
  const strokeWidth = 14;
  const hoveredStrokeWidth = 18;
  const center = 80;
  const circumference = 2 * Math.PI * radius; // ~314.16

  // If no data, render empty circle
  if (data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-64">
        <svg width="160" height="160" viewBox="0 0 160 160">
          <circle
            cx={center}
            cy={center}
            r={radius}
            fill="transparent"
            stroke="var(--border)"
            strokeWidth={strokeWidth}
          />
        </svg>
        <span className="text-sm text-muted-foreground mt-4">No data available</span>
      </div>
    );
  }

  // Calculate segment details
  let accumulatedPercentage = 0;
  const segments = data.map((item, index) => {
    const strokeLength = (item.percentage / 100) * circumference;
    const strokeOffset = circumference - ((accumulatedPercentage / 100) * circumference);
    accumulatedPercentage += item.percentage;
    
    return {
      ...item,
      color: COLORS[index % COLORS.length],
      strokeLength,
      strokeOffset,
    };
  });

  // Center display texts
  const activeSegment = hoveredIndex !== null ? segments[hoveredIndex] : null;
  const labelText = activeSegment ? activeSegment.category_name : "Total Spent";
  const amountText = activeSegment ? `$${activeSegment.total}` : `$${totalSpent}`;
  const pctText = activeSegment ? `${activeSegment.percentage.toFixed(1)}%` : "";

  return (
    <div className="flex flex-col sm:flex-row items-center justify-center gap-8 py-4">
      {/* SVG Chart */}
      <div className="relative w-44 h-44 flex-shrink-0">
        <svg
          width="100%"
          height="100%"
          viewBox="0 0 160 160"
          className="transform -rotate-90 select-none"
        >
          <AnimatePresence>
            {segments.map((seg, index) => {
              const isHovered = hoveredIndex === index;
              return (
                <motion.circle
                  key={seg.category_id}
                  cx={center}
                  cy={center}
                  r={radius}
                  fill="transparent"
                  stroke={seg.color}
                  strokeWidth={isHovered ? hoveredStrokeWidth : strokeWidth}
                  strokeDasharray={`${seg.strokeLength} ${circumference - seg.strokeLength}`}
                  strokeDashoffset={seg.strokeOffset}
                  strokeLinecap="round"
                  onMouseEnter={() => setHoveredIndex(index)}
                  onMouseLeave={() => setHoveredIndex(null)}
                  className="transition-all duration-200 cursor-pointer origin-center"
                  style={{
                    opacity: hoveredIndex === null || isHovered ? 1 : 0.6,
                  }}
                  initial={{ pathLength: 0 }}
                  animate={{ pathLength: 1 }}
                  transition={{ duration: 0.8, ease: "easeOut" }}
                />
              );
            })}
          </AnimatePresence>
        </svg>

        {/* Center text hole */}
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center p-4">
          <span className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground truncate w-full">
            {labelText}
          </span>
          <span className="text-xl font-extrabold tracking-tight text-foreground truncate w-full mt-0.5">
            {amountText}
          </span>
          {pctText && (
            <span className="text-xs font-semibold text-primary mt-0.5 animate-in fade-in duration-200">
              {pctText}
            </span>
          )}
        </div>
      </div>

      {/* Legend list */}
      <div className="flex-1 space-y-2.5 w-full max-w-[200px]">
        {segments.slice(0, 5).map((seg, index) => (
          <button
            key={seg.category_id}
            onMouseEnter={() => setHoveredIndex(index)}
            onMouseLeave={() => setHoveredIndex(null)}
            className={`flex items-center justify-between w-full text-left p-1.5 rounded-lg transition-colors cursor-pointer ${
              hoveredIndex === index ? "bg-secondary text-foreground" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <div className="flex items-center space-x-2 min-w-0">
              <span
                className="h-3 w-3 rounded-full flex-shrink-0"
                style={{ backgroundColor: seg.color }}
              />
              <span className="text-xs font-semibold truncate">{seg.category_name}</span>
            </div>
            <span className="text-xs font-bold font-mono ml-2">
              {seg.percentage.toFixed(0)}%
            </span>
          </button>
        ))}
        {segments.length > 5 && (
          <div className="text-[10px] text-muted-foreground text-center pt-1 border-t border-border/50">
            + {segments.length - 5} other categories
          </div>
        )}
      </div>
    </div>
  );
};

import { AnimatePresence } from "framer-motion";
