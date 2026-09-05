# Paradox Repository Persistent Memory

This document is the agent-level repository memory for Paradox, persisted across all development sessions.

## 1. Project Overview & Tech Stack
- **Project**: Paradox — AI-Powered Personal Finance & Wealth Intelligence Suite
- **Backend**: FastAPI (Python 3.11), SQLAlchemy 2.0 Async, PostgreSQL, Alembic, Pydantic v2
- **Frontend**: Next.js 16 (App Router), React 19, TypeScript, Vanilla Tailwind/CSS tokens, Framer Motion, TanStack Query
- **Authentication**: JWT access token + HttpOnly refresh cookie rotation + Google OAuth + Brevo OTP Omnichannel
- **AI Engine**: Google Gemini (gemini-2.5-flash, gemini-flash-latest, gemini-2.0-flash, gemini-1.5-flash Vision OCR), OpenAI, Claude, and deterministic semantic fallback engine

## 2. Completed Milestones
- **Phase 1 & 2**: Core MVP, Expense CRUD, Multi-Granularity Budgeting (Monthly `YYYY-MM`, Weekly `YYYY-Www`, Daily `YYYY-MM-DD`).
- **Phase 3**: Enterprise Multi-Tenant Auth, Token Rotation, Multi-Device Session Revocation (`/logout-all`).
- **Phase 4**: Multi-Provider AI categorizer, natural language expense parser, and offline heuristic fallback.
- **Phase 5**: Multi-Currency (`INR`, `USD`, `EUR`, `GBP`), Bank Statement CSV Import, CSV Export, Recurring Subscriptions, Multimodal Receipt OCR Scanner with client-side canvas downscaler.
- **Phase 6**: "Can I Afford This?" Purchase Simulator, Safe-to-Spend Speedometer, Financial Health Score (0-100), Micro-Spending Leak Hunter, Subscription Audit, Omnichannel Registration (6-digit OTP + magic link polling), Multi-Account Switcher & Session Vault, Brevo REST Email Delivery, Vercel CORS regex.
- **Phase 7**:
  - Voice Expense Quick-Add (Web Speech API)
  - Indian Bank & UPI SMS Transaction Parser
  - Smart Duplicate Transaction Guard
  - 50/30/20 Budget Optimization Framework
  - Financial Streaks & Discipline Achievements
  - Finny Financial Mood Mascot
  - Agent Repo Memory (`.agents/rules/`)
- **Phase 8 (Responsive Engine & Vision OCR Alignment)**:
  - AI Quick Add 2-tier responsive layout with strict 2-column tools grid, input min-w-0, and zero modal horizontal overflow on all viewports down to 320px
  - Verified Gemini multimodal OCR model alignment (`gemini-flash-latest`, `gemini-2.5-flash`, `gemini-flash-lite-latest`) with `inlineData` REST schema
  - Dynamic Category Expansion & 1-Click "Add & Select" prompt for unadded categories
  - Dialog responsive container scaling (`max-w-lg w-full`) and safe mobile vertical scroll
- **Phase 9 (Complete AI Feature Suite & Master Documentation)**:
  - Master documentation authored in `docs/AI_FEATURES.md` covering all implemented AI features
  - Full automated pytest coverage: 49/49 unit tests passing
  - Clean production builds on Next.js 16.3.3 Turbopack (0 errors)
- **Phase 10 (Full AI Intelligence Suite Completion & Zero-Hardcoding)**:
  - Implemented all 7 remaining AI features from `docs/AI_Features_List_Final.md`:
    1. Financial Assistant RAG live context chatbot (`POST /ai/chat`, `FinancialAssistantChat.tsx`)
    2. Real-time Spending Anomaly & Outlier Detector (`GET /ai/anomalies`, `AnomalyForecastCard.tsx`)
    3. 30-Day Category Predictive Forecast (`GET /ai/forecast`, `AnomalyForecastCard.tsx`)
    4. Goal-Based Savings Planner with discretionary category trimming (`POST /ai/savings-plan`, `SavingsPlannerDialog.tsx`)
    5. Emotional Sentiment & Buyer's Remorse reflection guard (`POST /ai/analyze-sentiment`, `ExpenseFormDialog.tsx`)
    6. Paradox Monthly Wrapped 5-slide animated story deck with financial archetypes (`GET /ai/monthly-wrapped`, `MonthlyWrappedModal.tsx`)
    7. Finny Vibe Check & Hinglish Roast Mode (`GET /ai/vibe-check`, `FinnyMascot.tsx`)
  - Fixed deprecated Gemini model 404 in insights, removed hardcoded currency symbols in frontend & backend.
- **Phase 11 (Brand & Product Auto-Categorization & 1-Click Category Creation)**:
  - Dual-engine instant auto-categorization (0ms client-side `PREDICTIVE_CATEGORIES` + debounced 400ms backend AI) listening to both `description` and `aiInputText`.
  - Embedded comprehensive Indian & global brand/product dictionary across Alcohol, Gaming, Shopping, Groceries, Healthcare, Personal Care, Fitness, Pets, Subscriptions, Food & Dining.
  - Added omnipresent 1-click "Add & Select" category creation directly inside the `CategoryPicker` pills grid, form suggestion banner, and inline `+ New Category` brand helper.
  - Fixed regex amount extraction bug in `_heuristic_parse` where alphanumeric names like `ps5` falsely extracted amount values.
  - Fixed Finny Mascot z-index layering against `FinancialCopilotCard` (`z-40` header, `z-50` mascot, `z-[100]` speech bubbles).
  - Automated tests: 58/58 unit tests passing in pytest (100% green).
  - Frontend production build: 14/14 routes compiled and prerendered cleanly with 0 TypeScript errors.

## 3. Database State
- Latest Alembic Migration: `a1b2c3d4e5f6_add_otp_to_pending_registration.py`
- Active Entities: `User`, `RefreshToken`, `PasswordResetToken`, `PendingRegistrationToken`, `Category`, `PaymentMethod`, `Expense`, `Budget`


