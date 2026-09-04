# Paradox Repository Persistent Memory

This document is the agent-level repository memory for Paradox, persisted across all development sessions.

## 1. Project Overview & Tech Stack
- **Project**: Paradox — AI-Powered Personal Finance & Wealth Intelligence Suite
- **Backend**: FastAPI (Python 3.11), SQLAlchemy 2.0 Async, PostgreSQL, Alembic, Pydantic v2
- **Frontend**: Next.js 16 (App Router), React 19, TypeScript, Vanilla Tailwind/CSS tokens, Framer Motion, TanStack Query
- **Authentication**: JWT access token + HttpOnly refresh cookie rotation + Google OAuth + Brevo OTP Omnichannel
- **AI Engine**: Google Gemini (Gemini 3.6 Flash / Vision), OpenAI, Claude, and local deterministic fallback engine

## 2. Completed Milestones
- **Phase 1 & 2**: Core MVP, Expense CRUD, Multi-Granularity Budgeting (Monthly `YYYY-MM`, Weekly `YYYY-Www`, Daily `YYYY-MM-DD`).
- **Phase 3**: Enterprise Multi-Tenant Auth, Token Rotation, Multi-Device Session Revocation (`/logout-all`).
- **Phase 4**: Multi-Provider AI categorizer, natural language expense parser, and offline heuristic fallback.
- **Phase 5**: Multi-Currency (`INR`, `USD`, `EUR`, `GBP`), Bank Statement CSV Import, CSV Export, Recurring Subscriptions, Multimodal Receipt OCR Scanner with client-side canvas downscaler.
- **Phase 6**: "Can I Afford This?" Purchase Simulator, Safe-to-Spend Speedometer, Financial Health Score (0-100), Micro-Spending Leak Hunter, Subscription Audit, Omnichannel Registration (6-digit OTP + magic link polling), Multi-Account Switcher & Session Vault, Brevo REST Email Delivery, Vercel CORS regex.
- **Phase 7 (Current)**:
  - Voice Expense Quick-Add (Web Speech API)
  - Indian Bank & UPI SMS Transaction Parser
  - Smart Duplicate Transaction Guard
  - 50/30/20 Budget Optimization Framework
  - Financial Streaks & Discipline Achievements
  - Finny Financial Mood Mascot
  - Agent Repo Memory (.agents/)

## 3. Database State
- Latest Alembic Migration: `a1b2c3d4e5f6_add_otp_to_pending_registration.py`
- Active Entities: `User`, `RefreshToken`, `PasswordResetToken`, `PendingRegistrationToken`, `Category`, `PaymentMethod`, `Expense`, `Budget`
