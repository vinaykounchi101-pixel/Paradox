"use client";

import React, { useState } from "react";
import { motion } from "framer-motion";

interface TrendItem {
  label: string;
  total: string;
}

interface TrendGraphProps {
  data: TrendItem[];
}

export const TrendGraph: React.FC<TrendGraphProps> = ({ data }) => {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  // Geometry
  const svgWidth = 500;
  const svgHeight = 200;
  const padding = { top: 20, right: 20, bottom: 35, left: 45 };
  
  const chartWidth = svgWidth - padding.left - padding.right;
  const chartHeight = svgHeight - padding.top - padding.bottom;

  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center h-48 border border-border rounded-lg">
        <span className="text-sm text-muted-foreground">No trend data available</span>
      </div>
    );
  }

  // Parse points
  const points = data.map((item) => ({
    label: item.label,
    val: parseFloat(item.total),
  }));

  const yValues = points.map((p) => p.val);
  const maxVal = Math.max(...yValues, 10);
  const roundedMaxVal = Math.ceil(maxVal * 1.15); // Add 15% headroom

  // Calculate coordinates
  const coords = points.map((p, index) => {
    const x = padding.left + (index / (points.length - 1)) * chartWidth;
    const y = padding.top + chartHeight - (p.val / roundedMaxVal) * chartHeight;
    return { x, y, label: p.label, val: p.val };
  });

  // Build SVG Path string (Smooth Cubic Bezier Curves)
  let linePath = "";
  let areaPath = "";

  if (coords.length > 0) {
    linePath = `M ${coords[0].x} ${coords[0].y}`;
    
    for (let i = 1; i < coords.length; i++) {
      const prev = coords[i - 1];
      const curr = coords[i];
      // Control points for smooth S-curves
      const cp1x = prev.x + (curr.x - prev.x) / 2;
      const cp1y = prev.y;
      const cp2x = prev.x + (curr.x - prev.x) / 2;
      const cp2y = curr.y;
      
      linePath += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${curr.x} ${curr.y}`;
    }

    // Connect to bottom for area fill
    const bottomY = padding.top + chartHeight;
    areaPath = `${linePath} L ${coords[coords.length - 1].x} ${bottomY} L ${coords[0].x} ${bottomY} Z`;
  }

  // Horizontal Grid Lines Y Values
  const gridCount = 3;
  const gridLines = Array.from({ length: gridCount + 1 }).map((_, i) => {
    const ratio = i / gridCount;
    const val = roundedMaxVal * ratio;
    const y = padding.top + chartHeight - ratio * chartHeight;
    return { y, val };
  });

  return (
    <div className="relative w-full">
      <svg
        width="100%"
        height="100%"
        viewBox={`0 0 ${svgWidth} ${svgHeight}`}
        className="overflow-visible select-none"
      >
        {/* Gradients */}
        <defs>
          <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.25" />
            <stop offset="100%" stopColor="var(--primary)" stopOpacity="0.0" />
          </linearGradient>
        </defs>

        {/* Horizontal Grid lines */}
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
              y={line.y + 4}
              textAnchor="end"
              className="text-[9px] font-mono fill-muted-foreground font-semibold"
            >
              ${line.val.toFixed(0)}
            </text>
          </g>
        ))}

        {/* Shaded Area fill under path */}
        <motion.path
          d={areaPath}
          fill="url(#areaGradient)"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.5 }}
        />

        {/* Smooth Bezier Line path */}
        <motion.path
          d={linePath}
          fill="none"
          stroke="var(--primary)"
          strokeWidth="2.5"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
        />

        {/* Interaction points / tooltips */}
        {coords.map((pt, index) => {
          const isHovered = hoveredIndex === index;
          return (
            <g key={index}>
              {/* Glowing anchor point on hover */}
              <circle
                cx={pt.x}
                cy={pt.y}
                r={isHovered ? 5 : 3.5}
                className="fill-primary stroke-background transition-all duration-150"
                style={{ strokeWidth: isHovered ? 2 : 1 }}
              />
              {isHovered && (
                <circle
                  cx={pt.x}
                  cy={pt.y}
                  r="10"
                  className="fill-primary/20 animate-ping pointer-events-none"
                />
              )}

              {/* Large invisible catch circle for better mouse hit targets */}
              <circle
                cx={pt.x}
                cy={pt.y}
                r="18"
                fill="transparent"
                onMouseEnter={() => setHoveredIndex(index)}
                onMouseLeave={() => setHoveredIndex(null)}
                className="cursor-pointer"
              />

              {/* X Axis Labels */}
              <text
                x={pt.x}
                y={svgHeight - 12}
                textAnchor="middle"
                className={`text-[9px] font-semibold transition-colors duration-150 ${
                  isHovered ? "fill-foreground font-bold" : "fill-muted-foreground"
                }`}
              >
                {pt.label}
              </text>
            </g>
          );
        })}
      </svg>

      {/* Floating HTML Tooltip overlay */}
      {hoveredIndex !== null && coords[hoveredIndex] && (
        <div
          className="absolute z-10 pointer-events-none glass px-2.5 py-1.5 rounded-md shadow-md text-xs font-semibold animate-in fade-in zoom-in duration-100 border border-primary/20"
          style={{
            left: `${(coords[hoveredIndex].x / svgWidth) * 100}%`,
            top: `${(coords[hoveredIndex].y / svgHeight) * 100 - 25}%`,
            transform: "translate(-50%, -100%)",
          }}
        >
          <div className="text-[10px] text-muted-foreground">{coords[hoveredIndex].label}</div>
          <div className="font-bold text-primary mt-0.5">${coords[hoveredIndex].val.toFixed(2)}</div>
        </div>
      )}
    </div>
  );
};
