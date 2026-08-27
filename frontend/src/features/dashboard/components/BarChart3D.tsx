"use client";

import React, { useState } from "react";
import { motion } from "framer-motion";

interface ChartData {
  category_id: string;
  category_name: string;
  total: string;
  percentage: number;
}

interface BarChart3DProps {
  data: ChartData[];
}

const COLORS = [
  { front: "#6366f1", top: "#818cf8", side: "#4338ca" }, // Indigo
  { front: "#f43f5e", top: "#fb7185", side: "#be123c" }, // Rose
  { front: "#10b981", top: "#34d399", side: "#065f46" }, // Emerald
  { front: "#a855f7", top: "#c084fc", side: "#7e22ce" }, // Purple
  { front: "#3b82f6", top: "#60a5fa", side: "#1d4ed8" }, // Blue
  { front: "#f59e0b", top: "#fbbf24", side: "#b45309" }, // Amber
  { front: "#06b6d4", top: "#22d3ee", side: "#0e7490" }, // Cyan
  { front: "#ec4899", top: "#f472b6", side: "#9d174d" }, // Pink
];

// 3D extrusion depth (pixels in SVG coords)
const DEPTH_X = 10;
const DEPTH_Y = -8;

export const BarChart3D: React.FC<BarChart3DProps> = ({ data }) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  // SVG geometry
  const svgWidth = 320;
  const svgHeight = 240;
  const padding = { top: 30, right: 24, bottom: 48, left: 48 };

  const chartWidth = svgWidth - padding.left - padding.right;
  const chartHeight = svgHeight - padding.top - padding.bottom;

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    setMousePos({ x: e.clientX - rect.left, y: e.clientY - rect.top });
  };

  if (data.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-52 border border-border rounded-lg">
        <span className="text-sm text-muted-foreground">No category data available</span>
      </div>
    );
  }

  const parsedData = data.map((item, i) => ({
    ...item,
    val: parseFloat(item.total),
    colors: COLORS[i % COLORS.length],
  }));

  const maxVal = Math.max(...parsedData.map((d) => d.val), 10);
  const roundedMax = Math.ceil(maxVal * 1.2);

  const barCount = parsedData.length;
  const slotWidth = chartWidth / barCount;
  const barWidth = Math.min(slotWidth * 0.55, 38);
  const barGap = (slotWidth - barWidth) / 2;

  const gridCount = 4;
  const gridLines = Array.from({ length: gridCount + 1 }).map((_, i) => {
    const ratio = i / gridCount;
    return {
      val: roundedMax * ratio,
      y: padding.top + chartHeight - ratio * chartHeight,
    };
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
        {/* ── Defs for 3D floor shadow filter ── */}
        <defs>
          <filter id="bar-shadow" x="-20%" y="-20%" width="140%" height="140%">
            <feDropShadow dx="2" dy="4" stdDeviation="3" floodOpacity="0.25" />
          </filter>
        </defs>

        {/* ── Horizontal gridlines ── */}
        {gridLines.map((line, i) => (
          <g key={i} className="opacity-15">
            <line
              x1={padding.left}
              y1={line.y}
              x2={svgWidth - padding.right}
              y2={line.y}
              stroke="var(--foreground)"
              strokeWidth="0.75"
              strokeDasharray="4 4"
            />
            <text
              x={padding.left - 8}
              y={line.y + 3}
              textAnchor="end"
              className="text-[8px] font-mono fill-muted-foreground"
            >
              ${line.val.toFixed(0)}
            </text>
          </g>
        ))}

        {/* ── 3D Bars ── */}
        {parsedData.map((item, index) => {
          const isHovered = hoveredIndex === index;
          const x = padding.left + index * slotWidth + barGap;
          const ratio = item.val / roundedMax;
          const bh = Math.max(ratio * chartHeight, 3);
          const y = padding.top + chartHeight - bh;

          // Truncated x-axis label
          const shortLabel =
            item.category_name.length > 6
              ? `${item.category_name.substring(0, 5)}..`
              : item.category_name;

          // 3D face vertices
          // Front face: (x, y) → (x+bw, y+bh)
          // Top face parallelogram: front-top-left → front-top-right → extrude back-right → extrude back-left
          const topFace = [
            `${x},${y}`,
            `${x + barWidth},${y}`,
            `${x + barWidth + DEPTH_X},${y + DEPTH_Y}`,
            `${x + DEPTH_X},${y + DEPTH_Y}`,
          ].join(" ");

          // Right face parallelogram: front-top-right → front-bottom-right → extrude back-bottom-right → extrude back-top-right
          const rightFace = [
            `${x + barWidth},${y}`,
            `${x + barWidth},${y + bh}`,
            `${x + barWidth + DEPTH_X},${y + bh + DEPTH_Y}`,
            `${x + barWidth + DEPTH_X},${y + DEPTH_Y}`,
          ].join(" ");

          const opacity = hoveredIndex === null || isHovered ? 1 : 0.55;

          return (
            <g
              key={item.category_id}
              style={{ opacity, filter: isHovered ? "url(#bar-shadow)" : undefined }}
              onMouseEnter={() => setHoveredIndex(index)}
              onMouseLeave={() => setHoveredIndex(null)}
              className="cursor-pointer"
            >
              {/* Right (shadow) face */}
              <motion.polygon
                points={rightFace}
                fill={item.colors.side}
                initial={{ scaleY: 0 }}
                animate={{ scaleY: 1 }}
                style={{ originY: `${padding.top + chartHeight}px` }}
                transition={{ duration: 0.55, delay: index * 0.07, ease: "easeOut" }}
              />

              {/* Front face */}
              <motion.rect
                x={x}
                y={y}
                width={barWidth}
                height={bh}
                fill={item.colors.front}
                rx={3}
                ry={3}
                initial={{ scaleY: 0 }}
                animate={{ scaleY: 1 }}
                style={{ originY: `${padding.top + chartHeight}px` }}
                transition={{ duration: 0.55, delay: index * 0.07, ease: "easeOut" }}
              />

              {/* Top (highlight) face */}
              <motion.polygon
                points={topFace}
                fill={item.colors.top}
                opacity={0.9}
                initial={{ opacity: 0 }}
                animate={{ opacity: 0.9 }}
                transition={{ duration: 0.3, delay: index * 0.07 + 0.4 }}
              />

              {/* Hover shimmer on front face */}
              {isHovered && (
                <rect
                  x={x}
                  y={y}
                  width={barWidth}
                  height={bh}
                  rx={3}
                  fill="rgba(255,255,255,0.12)"
                />
              )}

              {/* X-axis label */}
              <text
                x={x + barWidth / 2}
                y={svgHeight - 18}
                textAnchor="middle"
                className={`text-[8px] uppercase tracking-wide font-semibold transition-all ${
                  isHovered ? "fill-foreground" : "fill-muted-foreground"
                }`}
              >
                {shortLabel}
              </text>

              {/* Value label above bar */}
              {isHovered && (
                <motion.text
                  x={x + barWidth / 2 + DEPTH_X / 2}
                  y={y + DEPTH_Y - 4}
                  textAnchor="middle"
                  className="text-[8px] font-bold fill-foreground"
                  initial={{ opacity: 0, y: y + DEPTH_Y + 4 }}
                  animate={{ opacity: 1, y: y + DEPTH_Y - 4 }}
                  transition={{ duration: 0.15 }}
                >
                  ${item.val.toFixed(0)}
                </motion.text>
              )}
            </g>
          );
        })}
      </svg>

      {/* Floating tooltip */}
      {hoveredIndex !== null && parsedData[hoveredIndex] && (
        <div
          className="absolute z-50 pointer-events-none glass px-3 py-2 rounded-lg shadow-lg text-xs font-semibold border border-primary/20 text-center"
          style={{
            left: `${mousePos.x}px`,
            top: `${mousePos.y - 14}px`,
            transform: "translate(-50%, -100%)",
          }}
        >
          <div className="font-bold text-foreground uppercase tracking-wider text-[10px]">
            {parsedData[hoveredIndex].category_name}
          </div>
          <div
            className="font-bold text-sm mt-0.5"
            style={{ color: parsedData[hoveredIndex].colors.front }}
          >
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
