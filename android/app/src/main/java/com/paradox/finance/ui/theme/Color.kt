package com.paradox.finance.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// 🌑 PARADOX DESIGN SYSTEM — OBSIDIAN FLOW (Design.md & Stitch Screens)
// =========================================================================

// 1. Canvas & Foundation Tones (Pitch Black & Obsidian Elevations)
val BackgroundPitchBlack = Color(0xFF000000)          // OLED Pitch Black (#000000)
val BackgroundDark = Color(0xFF0A0D14)                // Base Canvas (Charcoal-Slate Obsidian)
val SurfaceObsidianSubtle = Color(0xFF0D0E12)          // Surface Container Lowest (#0D0E12)
val SurfaceObsidianBase = Color(0xFF14161F)            // Surface Elevation 1 Base Cards (#14161F)
val SurfaceObsidianElevated = Color(0xFF1A1D26)        // Surface Elevation 2 Elevated Cards (#1A1D26)
val SurfaceObsidianHighlight = Color(0xFF242838)       // Surface Elevation 3 Highlight (#242838)

val SurfaceDark = Color(0xFF121722)                   // Legacy Surface Elevation 1
val SurfaceCard = Color(0xFF1A2234)                   // Legacy Surface Elevation 2
val SurfaceBright = Color(0xFF272A32)                 // Surface High

val BorderGlass = Color(0x14FFFFFF)                    // rgba(255, 255, 255, 0.08)
val BorderGlassFocused = Color(0x2EFFFFFF)             // rgba(255, 255, 255, 0.18)
val BorderDark = Color(0xFF1E2638)                    // Subtle Surface 1px Stroke
val BorderMuted = Color(0xFF3C4A42)                   // Outline Variant

// 2. Typography & Contrast Hierarchy
val TextPrimary = Color(0xFFF8FAFC)                   // High Contrast (#F8FAFC)
val TextSecondary = Color(0xFF94A3B8)                 // Mid Contrast (#94A3B8)
val TextTertiary = Color(0xFF64748B)                  // Subtitle & Labels (#64748B)
val TextMuted = Color(0xFF475569)                     // Muted Contrast

// 3. Financial Semantic Colors & Stitch Neons
val NeonEmerald = Color(0xFF10B981)                   // Income / Safe Velocity (#10B981)
val NeonTeal = Color(0xFF14B8A6)                      // Pacing / Telemetry (#14B8A6)
val NeonCyan = Color(0xFF06B6D4)                      // AI Copilot & Telemetry (#06B6D4)
val NeonViolet = Color(0xFF8B5CF6)                    // Goals / Insights (#8B5CF6)
val AlertCoral = Color(0xFFF43F5E)                    // Expense Alert / Drain (#F43F5E)
val AlertAmber = Color(0xFFF59E0B)                    // Warning / Caution (#F59E0B)

val AccentEmerald = NeonEmerald
val AccentEmeraldLight = Color(0xFF4EDEA3)
val AccentEmeraldGlow = Color(0x3310B981)

val PrimaryIndigo = Color(0xFF6366F1)
val PrimaryIndigoHover = Color(0xFF4F46E5)
val PrimaryIndigoLight = Color(0xFFC0C1FF)
val PrimaryIndigoGlow = Color(0x336366F1)

val AccentCyan = NeonCyan
val AccentCyanLight = Color(0xFF4CD7F6)
val AccentCyanGlow = Color(0x3306B6D4)

val AccentRose = AlertCoral
val AccentRoseLight = Color(0xFFFFB4AB)
val AccentRoseGlow = Color(0x33F43F5E)

val AccentViolet = NeonViolet
val AccentAmber = AlertAmber

// 4. 50/30/20 Budget Framework
val NeedsColor = Color(0xFF3B82F6)                    // Blue-500 (50% Needs)
val WantsColor = Color(0xFFF59E0B)                    // Amber-500 (30% Wants)
val SavingsColor = Color(0xFF10B981)                  // Emerald-500 (20% Savings)

// 5. Utility Compatibility Aliases
val Zinc950 = BackgroundDark
val Zinc900 = SurfaceDark
val Zinc800 = SurfaceCard
val Zinc700 = BorderDark
val Zinc600 = Color(0xFF334155)
val Zinc500 = TextMuted
val Zinc400 = TextSecondary
val Zinc300 = Color(0xFFCBD5E1)
val Zinc200 = Color(0xFFE2E8F0)
val Zinc50 = TextPrimary

val Indigo600 = PrimaryIndigoHover
val Indigo500 = PrimaryIndigo
val Indigo400 = Color(0xFF818CF8)

val Emerald500 = AccentEmerald
val Emerald400 = Color(0xFF34D399)

val Rose500 = AccentRose
val Rose400 = Color(0xFFFB7185)

val Amber500 = AccentAmber
val Amber400 = Color(0xFFFBBF24)
