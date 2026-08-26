"use client";

import React from "react";
import { motion } from "framer-motion";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "danger" | "ghost" | "link";
  size?: "sm" | "md" | "lg";
  isLoading?: boolean;
  fullWidth?: boolean;
  animate?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = "primary",
      size = "md",
      isLoading = false,
      fullWidth = false,
      animate = true,
      children,
      disabled,
      ...props
    },
    ref
  ) => {
    const baseStyles = "inline-flex items-center justify-center font-medium rounded-lg transition-all focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 focus:ring-offset-background disabled:opacity-50 disabled:pointer-events-none cursor-pointer";
    
    const variants = {
      primary: "bg-primary text-primary-foreground hover:bg-indigo-500 shadow-md shadow-indigo-500/10 hover:shadow-indigo-500/20 active:scale-95",
      secondary: "bg-secondary text-secondary-foreground hover:bg-zinc-800 border border-border active:scale-95",
      danger: "bg-destructive text-destructive-foreground hover:bg-red-500 shadow-md shadow-red-500/10 hover:shadow-red-500/20 active:scale-95",
      ghost: "text-foreground hover:bg-accent hover:text-accent-foreground active:scale-95",
      link: "text-primary underline-offset-4 hover:underline bg-transparent p-0 border-0 focus:ring-0 focus:ring-offset-0",
    };

    const sizes = {
      sm: "h-9 px-3 text-xs",
      md: "h-10 px-4 py-2 text-sm",
      lg: "h-11 px-6 text-base",
    };

    const buttonClass = twMerge(
      clsx(
        baseStyles,
        variants[variant],
        sizes[size],
        fullWidth && "w-full",
        className
      )
    );

    const buttonContent = (
      <>
        {isLoading && (
          <svg
            className="animate-spin -ml-1 mr-2 h-4 w-4 text-current"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {children}
      </>
    );

    if (animate && !disabled && !isLoading && variant !== "link") {
      return (
        <motion.button
          ref={ref as any}
          className={buttonClass}
          whileHover={{ scale: 1.015 }}
          whileTap={{ scale: 0.985 }}
          disabled={disabled}
          {...(props as any)}
        >
          {buttonContent}
        </motion.button>
      );
    }

    return (
      <button
        ref={ref}
        className={buttonClass}
        disabled={disabled || isLoading}
        {...props}
      >
        {buttonContent}
      </button>
    );
  }
);

Button.displayName = "Button";
