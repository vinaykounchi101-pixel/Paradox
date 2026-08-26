"use client";

import React, { useState } from "react";
import { motion } from "framer-motion";

interface ChartData {
  category_id: string;
  category_name: string;
  total: string;
  percentage: number;
}

interface BarChartProps {
  data: ChartData[];
}

const COLORS = [
  "#6366f1", // Indigo
  "#f43f5e", // Rose
  "#10b981", // Emerald
  "#a855f7", // Purple
  "#3b82f6", // Blue
  "#f59e0b", // Amber
  "#06b6d4", // Cyan
  "#ec4899", // Pink
];

export const BarChart: React.FC<BarChartProps> = ({ data }) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
  const [showTooltip, setShowTooltip] = useState(false);

  // Geometry
  const svgWidth = 320;
  const svgHeight = 220;
  const padding = { top: 20, right: 15, bottom: 45, left: 45 };

  const chartWidth = svgWidth - padding.left - padding.right;
  const chartHeight = svgHeight - padding.top - padding.bottom;

  // Track mouse coordinates relative to the parent container
  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    setMousePos({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    });
  };

  if (data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-48 border border-border rounded-lg">
        <span className="text-sm text-muted-foreground">No category data available</span>
      </div>
    );
  }

  // Parse Y max value
  const parsedData = data.map((item, index) => ({
    ...item,
    val: parseFloat(item.total),
    color: COLORS[index % COLORS.length],
  }));

  const maxVal = Math.max(...parsedData.map((d) => d.val), 10);
  const roundedMaxVal = Math.ceil(maxVal * 1.15); // 15% headroom

  // Calculate bar geometry
  const barCount = parsedData.length;
  const slotWidth = chartWidth / barCount;
  const barWidth = slotWidth * 0.6; // 60% of slot width
  const barGap = slotWidth * 0.4;  // 40% spacing

  // Horizontal Grid Lines Y Coordinates
  const gridCount = 3;
  const gridLines = Array.from({ length: gridCount + 1 }).map((_, i) => {
    const ratio = i / gridCount;
    const val = roundedMaxVal * ratio;
    const y = padding.top + chartHeight - ratio * chartHeight;
    return { y, val };
  });

  return (
    <div
      className="relative flex flex-col items-center justify-center w-full py-2"
      onMouseMove={handleMouseMove}
    >
      <svg
        width="100%"
        height="100%"
        viewBox={`0 0 ${svgWidth} ${svgHeight}`}
        className="overflow-visible select-none"
      >
        {/* Horizontal gridlines */}
        {gridLines.map((line, i) => (
          <g key={i} className="opacity-20">
            <line
              x1={padding.left}
              y1={line.y}
              x2={svgWidth - padding.right}
              y2={line.y}
              stroke="var(--foreground)"
              strokeWidth="0.75"
              strokeDasharray="4 4"
            />
            {/* Axis Label */}
            <text
              x={padding.left - 10}
              y={line.y + 3}
              textAnchor="end"
              className="text-[9px] font-mono fill-muted-foreground font-semibold"
            >
              ${line.val.toFixed(0)}
            </text>
          </g>
        ))}

        {/* Vertical Bars */}
        {parsedData.map((item, index) => {
          const isHovered = hoveredIndex === index;
          const x = padding.left + index * slotWidth + barGap / 2;
          const ratio = item.val / roundedMaxVal;
          const calculatedHeight = ratio * chartHeight;
          const y = padding.top + chartHeight - calculatedHeight;

          // Short label for X-axis
          const shortLabel =
            item.category_name.length > 7
              ? `${item.category_name.substring(0, 6)}..`
              : item.category_name;

          return (
            <g key={item.category_id}>
              {/* Bar rectangle */}
              <motion.rect
                x={x}
                y={y}
                width={barWidth}
                height={calculatedHeight > 0 ? calculatedHeight : 2} // At least 2px height for visual reference
                fill={item.color}
                rx={3}
                onMouseEnter={() => {
                  setHoveredIndex(index);
                  setShowTooltip(true);
                }}
                onMouseLeave={() => {
                  setHoveredIndex(null);
                  setShowTooltip(false);
                }}
                className="cursor-pointer transition-all duration-200"
                style={{
                  opacity: hoveredIndex === null || isHovered ? 1 : 0.6,
                }}
                initial={{ scaleY: 0, originY: 1 }}
                animate={{ scaleY: 1 }}
                transition={{ duration: 0.6, ease: "easeOut" }}
              />

              {/* X Axis Label */}
              <text
                x={x + barWidth / 2}
                y={svgHeight - 15}
                textAnchor="middle"
                className={`text-[9px] font-semibold transition-colors duration-150 uppercase tracking-wider ${
                  isHovered ? "fill-foreground font-bold" : "fill-muted-foreground"
                }`}
              >
                {shortLabel}
              </text>
            </g>
          );
        })}
      </svg>

      {/* Floating Tooltip */}
      {showTooltip && hoveredIndex !== null && parsedData[hoveredIndex] && (
        <div
          className="absolute z-50 pointer-events-none glass px-3 py-2 rounded-lg shadow-md text-xs font-semibold animate-in fade-in zoom-in-95 duration-100 border border-primary/20 text-center"
          style={{
            left: `${mousePos.x}px`,
            top: `${mousePos.y - 12}px`,
            transform: "translate(-50%, -100%)",
          }}
        >
          <div className="font-bold text-foreground uppercase tracking-wider text-[10px]">
            {parsedData[hoveredIndex].category_name}
          </div>
          <div className="font-bold text-primary text-sm mt-0.5">
            ${parsedData[hoveredIndex].total}
          </div>
          <div className="text-[9px] text-muted-foreground mt-0.5">
            {parsedData[hoveredIndex].percentage.toFixed(1)}% of total
          </div>
        </div>
      )}
    </div>
  );
};
