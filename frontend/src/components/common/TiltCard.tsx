"use client";

import React, { useRef, useState } from "react";
import {
  motion,
  useMotionValue,
  useSpring,
  useTransform,
  useMotionTemplate,
} from "framer-motion";

interface TiltCardProps {
  children: React.ReactNode;
  className?: string;
  /** Max rotation in degrees. Default: 10 */
  intensity?: number;
}

const SPRING_CFG = { stiffness: 220, damping: 22, mass: 0.5 };

export const TiltCard: React.FC<TiltCardProps> = ({
  children,
  className = "",
  intensity = 10,
}) => {
  const ref = useRef<HTMLDivElement>(null);
  const [isHovered, setIsHovered] = useState(false);

  // Normalised mouse position: -0.5 → 0.5
  const rawX = useMotionValue(0);
  const rawY = useMotionValue(0);

  const rotateY = useSpring(
    useTransform(rawX, [-0.5, 0.5], [-intensity, intensity]),
    SPRING_CFG
  );
  const rotateX = useSpring(
    useTransform(rawY, [-0.5, 0.5], [intensity, -intensity]),
    SPRING_CFG
  );

  // Glare spotlight position (0% → 100%)
  const glareX = useTransform(rawX, [-0.5, 0.5], [0, 100]);
  const glareY = useTransform(rawY, [-0.5, 0.5], [0, 100]);
  const glareBackground = useMotionTemplate`radial-gradient(circle at ${glareX}% ${glareY}%, rgba(255,255,255,0.10) 0%, rgba(255,255,255,0.03) 40%, transparent 70%)`;

  // Subtle Z scale on hover
  const scale = useSpring(1, SPRING_CFG);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!ref.current) return;
    const rect = ref.current.getBoundingClientRect();
    rawX.set((e.clientX - rect.left) / rect.width - 0.5);
    rawY.set((e.clientY - rect.top) / rect.height - 0.5);
  };

  const handleMouseEnter = () => {
    setIsHovered(true);
    scale.set(1.015);
  };

  const handleMouseLeave = () => {
    rawX.set(0);
    rawY.set(0);
    scale.set(1);
    setIsHovered(false);
  };

  return (
    /* Perspective wrapper — must NOT be the animated element itself */
    <div className="perspective-800 w-full">
      <motion.div
        ref={ref}
        className={`relative ${className}`}
        style={{
          rotateX,
          rotateY,
          scale,
          transformStyle: "preserve-3d",
        }}
        onMouseMove={handleMouseMove}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
      >
        {children}

        {/* Specular glare overlay */}
        <motion.div
          className="tilt-glare"
          style={{
            background: glareBackground,
            opacity: isHovered ? 1 : 0,
          }}
        />
      </motion.div>
    </div>
  );
};
