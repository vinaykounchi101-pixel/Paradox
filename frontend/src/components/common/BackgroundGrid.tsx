"use client";

import React from "react";

/**
 * BackgroundGrid
 * Renders a CSS perspective-transformed animated grid behind all page content.
 * Pure CSS — zero JS after mount. Styled via `.bg-grid-3d` in globals.css.
 */
export const BackgroundGrid: React.FC = () => (
  <div className="bg-grid-3d" aria-hidden="true" />
);
