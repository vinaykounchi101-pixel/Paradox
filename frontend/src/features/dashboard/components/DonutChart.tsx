"use client";

import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";

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
  "#14b8a6", // Teal/Cyan (BILLS in screenshot is teal)
  "#f43f5e", // Rose
  "#6366f1", // Indigo
  "#10b981", // Emerald
  "#a855f7", // Purple
  "#f59e0b", // Amber
  "#06b6d4", // Cyan
  "#ec4899", // Pink
];

export const DonutChart: React.FC<DonutChartProps> = ({ data, totalSpent }) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [showTooltip, setShowTooltip] = useState(false);

  // Constants for Donut Geometry
  const radius = 50;
  const strokeWidth = 14;
  const hoveredStrokeWidth = 18;
  const center = 80;
  const circumference = 2 * Math.PI * radius; // ~314.16

  // Handle mouse movements for tooltip tracking
  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    setMousePos({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    });
  };

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

  return (
    <div 
      className="relative flex flex-col items-center justify-center py-4 w-full"
      onMouseMove={handleMouseMove}
    >
      {/* SVG Chart centered */}
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
                  onMouseEnter={() => {
                    setHoveredIndex(index);
                    setShowTooltip(true);
                  }}
                  onMouseLeave={() => {
                    setHoveredIndex(null);
                    setShowTooltip(false);
                  }}
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

        {/* Hollow center area */}
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center p-4">
          <span className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground truncate w-full">
            Total Spent
          </span>
          <span className="text-xl font-extrabold tracking-tight text-foreground truncate w-full mt-0.5">
            ${totalSpent}
          </span>
        </div>
      </div>

      {/* Legend list flowing horizontally beneath the pie chart */}
      <div className="flex flex-wrap justify-center gap-x-4 gap-y-2 mt-6 w-full px-2">
        {segments.map((seg, index) => (
          <button
            key={seg.category_id}
            onMouseEnter={() => setHoveredIndex(index)}
            onMouseLeave={() => setHoveredIndex(null)}
            className={`flex items-center space-x-1.5 px-2.5 py-1 rounded-full text-xs transition-colors cursor-pointer ${
              hoveredIndex === index ? "bg-secondary text-foreground" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <span
              className="h-2.5 w-2.5 rounded-full flex-shrink-0"
              style={{ backgroundColor: seg.color }}
            />
            <span className="font-semibold uppercase tracking-wider text-[10px]">{seg.category_name}</span>
            <span className="font-mono text-[10px] font-bold">({seg.percentage.toFixed(0)}%)</span>
          </button>
        ))}
      </div>

      {/* Floating Tooltip */}
      {showTooltip && hoveredIndex !== null && segments[hoveredIndex] && (
        <div
          className="absolute z-50 pointer-events-none glass px-3 py-2 rounded-lg shadow-md text-xs font-semibold animate-in fade-in zoom-in-95 duration-100 border border-primary/20 text-center"
          style={{
            left: `${mousePos.x}px`,
            top: `${mousePos.y - 12}px`,
            transform: "translate(-50%, -100%)",
          }}
        >
          <div className="font-bold text-foreground uppercase tracking-wider text-[10px]">
            {segments[hoveredIndex].category_name}
          </div>
          <div className="font-bold text-primary text-sm mt-0.5">
            ${segments[hoveredIndex].total}
          </div>
          <div className="text-[9px] text-muted-foreground mt-0.5">
            {segments[hoveredIndex].percentage.toFixed(1)}% of total
          </div>
        </div>
      )}
    </div>
  );
};
