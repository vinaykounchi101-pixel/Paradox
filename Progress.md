# Progress & Technical Architecture Report: Paradox Project

This document provides a comprehensive summary of all architectural implementations, feature additions, database migrations, Progressive Web App (PWA) configurations, testing suites, Postman API collections, UI enhancements, and deployment states across **Phases 1 through 6: MVP, Multi-Granularity Budgeting, Multi-Tenant Authentication, Multi-Provider AI Financial Intelligence, Paradox V2 Supercharged Financial Suite, Omnichannel Dual-Mode Registration, and Multi-Account In-App Switching**.

---

## 1. Core Project Guidelines & Constraints
- **Strict Architecture**: Layered architecture (**API/Router ➔ Service Layer ➔ Repository Layer ➔ Database**).
- **Zero Inline Hardcoding**: All configurations, URLs, categories, budgets, and tokens are dynamically managed or environment-driven.
- **Secrets & Environment Isolation**: `.env` and `.env.local` are strictly ignored by Git and never committed or directly accessed by tools. Keep `.env.example` continuously updated.
- **Precision Monetary Values**: Uses fixed-precision `Numeric(12, 2)` (Python `Decimal`) with database `CHECK (amount > 0)` and `CHECK (amount >= 0)` constraints.
- **Communication Conventions**: Follows pair-programming callout rules (`"Roger That"` before starting work, `"Over n Out"` upon completion, and `"Signing off"` upon session closure).

---

## 2. Directory Layout & Module Structure
```
Paradox/
├── backend/
│   ├── app/
│   │   ├── api/                 # FastAPI router endpoints (auth, expenses, categories, payment_methods, budget, dashboard, health)
│   │   │   ├── auth.py          # Register, Login, Google OAuth, Refresh, Logout, Logout-all, Password recovery
│   │   │   └── deps.py          # Authoritative get_current_user Bearer JWT dependency
│   │   ├── constants/           # Starter system UUIDs & definitions
│   │   ├── core/                # App configuration, security utilities (bcrypt, JWT, token hashing), error handlers & exceptions
│   │   ├── db/
│   │   │   ├── models/          # SQLAlchemy 2.0 ORM models (User, RefreshToken, PasswordResetToken, Category, PaymentMethod, Expense, Budget)
│   │   │   ├── session.py       # Async engine & sessionmaker
│   │   │   └── base.py          # Declarative Base
│   │   ├── repositories/        # Database access layer (scoped to user_id, atomic cascade reassignments)
│   │   ├── schemas/             # Pydantic validation models & serializers (auth, expense, budget, category, etc.)
│   │   ├── services/            # Pure business logic layer (AuthService, ExpenseService, BudgetService, etc.)
│   │   └── utils/               # Datetime and formatting utilities
│   ├── migrations/              # Alembic asynchronous migrations
│   │   └── versions/            # 80a4bc410a4b, b2f8a1c9d4e5, c3a7b9e1f5d2, d4e8f2a1b9c3 (Auth & User Isolation)
│   ├── tests/                   # Pytest automated test suites (test_auth.py, test_user_isolation.py, test_budget_status.py, etc.)
│   ├── Dockerfile               # Production container config for Render
│   └── requirements.txt         # Production backend dependencies (pyjwt, passlib, bcrypt, google-auth)
├── frontend/
│   ├── public/
│   │   ├── icons/               # PWA icons (icon-192.svg, icon-512.svg, icon-maskable.svg)
│   │   ├── manifest.json        # Web App Manifest
│   │   └── sw.js                # Resilient Service Worker (network-first navigation)
│   ├── src/
│   │   ├── app/                 # Next.js App Router (login, register, forgot-password, reset-password, dashboard, expenses, budget, categories)
│   │   ├── components/
│   │   │   ├── common/          # ProtectedRoute, PwaRegister, BackgroundGrid, ThemeProvider, QueryProvider
│   │   │   ├── layout/          # Shell navigation with Hamburger Drawer, Topbar profile menu, and modal dialogs
│   │   │   └── ui/              # Button, Card, TiltCard, Dialog, Toast, CategoryPicker, PaymentMethodDropdown, BarChart3D
│   │   ├── features/            # Feature-scoped hooks, components, and views
│   │   │   ├── auth/            # AuthContext, useAuth, LoginForm, RegisterForm, GoogleSignInButton, Password Modals
│   │   │   ├── dashboard/
│   │   │   ├── expenses/
│   │   │   ├── categories/
│   │   │   └── budget/
│   │   ├── lib/api/             # Typed API client services (with in-memory token, credentials include, 401 refresh interceptor)
│   │   └── styles/              # Global CSS & Design System tokens
│   └── vercel.json              # Vercel deployment framework configuration
├── docs/                        # PRD (v2.0), SRS (v2.0), Design System, and POSTMAN_TESTING_GUIDE.md
├── Paradox.postman_collection.json # Full Postman collection (v2.1.0) with automated variable chaining
├── Paradox.postman_environment.json # Postman local environment configuration
└── Paradox.postman_production_environment.json # Postman production/Render environment configuration
```

---

## 3. Phase 3: Production Authentication & Multi-Tenant Data Isolation
- **Authentication Engine**:
  - **Short-Lived Access Token**: Signed with HS256, 15-minute expiration, containing `sub` (user UUID) and `email`. Stored strictly **in memory** on the client.
  - **Long-Lived Refresh Token**: 64-character random hex string, 7–30 days expiration, stored in a secure **`HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth`** cookie.
  - **Refresh Token Rotation**: Each call to `/api/v1/auth/refresh` immediately revokes the presented refresh token and issues a new access token + new refresh token.
  - **Database Hashing**: Database stores only SHA-256 hashes (`token_hash`) of refresh and password reset tokens.
  - **Multi-Device Revocation**: `/api/v1/auth/logout-all` revokes all active refresh tokens for the user across all devices.
  - **Google OAuth 2.0 / OIDC**: Server-side verification of Google ID token signature and audience.
- **Row-Level Tenant Isolation**:
  - Authoritative `get_current_user` FastAPI dependency extracts and validates JWT Bearer tokens.
  - All database queries across `ExpenseRepository`, `BudgetRepository`, `CategoryRepository`, `PaymentMethodRepository`, and `DashboardService` strictly filter by `user_id == current_user.id`.
  - Custom categories and payment methods belong to individual users (`user_id = user.id`); system starter records are globally shared (`user_id IS NULL`, `is_default = True`).
- **Frontend Session Hydration & Interceptors**:
  - `client.ts` automatically attaches Bearer tokens and sends cookies with `credentials: "include"`.
  - On 401 response, `attemptRefreshToken()` triggers silent token refresh with mutex concurrency control and retries the original request seamlessly.
  - `<ProtectedRoute>` wraps all private views (`/dashboard`, `/expenses`, `/budget`, `/categories`) with automatic login redirection.

---

## 4. Phase 4: Multi-Provider AI Financial Intelligence & Smart Automation
- **Multi-Provider AI Service Layer**:
  - `AIService` ([`backend/app/services/ai_service.py`](file:///e:/Projects/Paradox/backend/app/services/ai_service.py)) dynamically auto-detects and connects to **Google Gemini** (`GEMINI_API_KEY`), **OpenAI** (`OPENAI_API_KEY`), or **Anthropic Claude** (`ANTHROPIC_API_KEY`) using asynchronous `httpx` REST APIs.
  - Zero hardcoding, 100% environment-variable driven (`AI_PROVIDER=auto|gemini|openai|anthropic`).
  - Resilient offline fallback: If external AI services fail or keys are unconfigured, it automatically falls back to an internal **Semantic Keyword Heuristic Engine** without throwing 500 errors.
- **REST Endpoints**:
  - `POST /api/v1/ai/categorize`: Context-aware category recommendation from note/description.
  - `POST /api/v1/ai/parse-expense`: Natural language sentence parsing into structured amount, category, payment method, date, and description.
- **Frontend Form Enhancements**:
  - **AI Quick Add**: Freeform input box in [`ExpenseFormDialog.tsx`](file:///e:/Projects/Paradox/frontend/src/features/expenses/components/ExpenseFormDialog.tsx) for 1-click parsing.
  - **Live Category Recommendation**: Debounced suggestion badge under description input with 1-click apply.

---

## 5. Multi-Granularity Budget System
- **Budgeting Granularities**:
  - **Monthly**: Target identified by `YYYY-MM` (e.g. `2026-08`).
  - **Weekly**: Target identified by ISO week `YYYY-Www` (e.g. `2026-W35`).
  - **Daily**: Target identified by date `YYYY-MM-DD` (e.g. `2026-08-28`).
- **REST Endpoints**:
  - `GET /api/v1/budget?period_type=...&period_key=...`
  - `GET /api/v1/budget/all?period_type=...`
  - `PUT /api/v1/budget` (Upsert target scoped to user)
  - `DELETE /api/v1/budget?period_type=...&period_key=...`

---

## 5. UI, PWA & Component Upgrades
- **Progressive Web App (PWA)**:
  - Configured with `display: "standalone"`, `start_url: "/"`, theme `#6366f1`, background `#09090b`.
  - Resilient network-first Service Worker ([`sw.js`](file:///E:/Projects/Paradox/frontend/public/sw.js)) & auto-registration.
- **Top Navigation Bar & Profile Menu**:
  - Displays user avatar / initials and dropdown menu with "Change Password", "Sign Out", and "Sign Out from All Devices".
  - Automatically hides navigation bars on standalone auth pages (`/login`, `/register`, `/forgot-password`, `/reset-password`).

---

## 6. Phase 5: Paradox V2 Supercharged Financial Architecture & Vision AI
- **Dynamic Multi-Currency System (`₹ INR`, `$ USD`, `€ EUR`, `£ GBP`)**:
  - Added `currency` column to `User` table (defaulting to `'INR'`).
  - Added `PATCH /api/v1/auth/me` with `UpdateProfileRequest` schema.
  - Alembic migration `f6a1b2c3d4e5_add_user_currency_and_recurring_expenses` applied.
  - Frontend reactive [`CurrencyContext.tsx`](frontend/src/features/auth/context/CurrencyContext.tsx) with Topbar selector pill in [`shell.tsx`](frontend/src/components/layout/shell.tsx).
  - Dynamic `formatCurrency(amount)` reformatting across Dashboard, Expense list, Trend graph, and Copilot without hardcoded `$`.
- **CSV Export & Intelligent Bank Statement Import**:
  - `GET /api/v1/expenses/export` streams complete CSV spreadsheet with date/category filters.
  - `POST /api/v1/expenses/import` accepts bank statement CSVs, automatically maps Date/Amount/Narration columns, and bulk-classifies transactions with AI.
  - Frontend: "📥 Export CSV" and "📤 Import Statement" with drag-and-drop modal in [`ExpenseListView.tsx`](frontend/src/features/expenses/components/ExpenseListView.tsx).
- **Recurring Expenses & Subscription Commitments**:
  - Added `is_recurring` (boolean) and `recurring_frequency` (varchar) to `Expense` model.
  - `GET /api/v1/expenses/recurring` computes active subscriptions and normalized monthly commitments.
  - Frontend: "🔁 Recurring Subscription" toggle with frequency pills (`monthly`, `weekly`, `yearly`) in [`ExpenseFormDialog.tsx`](frontend/src/features/expenses/components/ExpenseFormDialog.tsx).
  - Frontend: "Subscriptions & Recurring Bills" card in [`DashboardView.tsx`](frontend/src/features/dashboard/components/DashboardView.tsx) and recurring badges in expense lists.
- **Multimodal Receipt & Invoice OCR Scanner**:
  - `POST /api/v1/ai/scan-receipt` using Google **Gemini 3.6 Flash** Vision API (`gemini-flash-latest` fallback).
  - Client-side canvas pre-scaler in [`ExpenseFormDialog.tsx`](frontend/src/features/expenses/components/ExpenseFormDialog.tsx) downscales 5MB-10MB camera photos to 1280px (~120KB) in ~50ms.
  - Parses merchant, total amount, date, category, and payment method, auto-filling the entire expense form in ~1-2 seconds.
- **Monthly Financial Health Report**:
  - Modal report view with key financial KPIs, budget adherence %, category distributions, and recurring burden.
  - One-click print / PDF export via browser print stylesheets.
- **Category Picker & Quick-Add Bugfixes**:
  - Fixed duplicate category creation in [`ExpenseFormDialog.tsx`](frontend/src/features/expenses/components/ExpenseFormDialog.tsx) and [`CategoryPicker.tsx`](frontend/src/components/ui/CategoryPicker.tsx) by tracking local optimistic states.
  - AI category auto-fill matches aliases and updates state immediately.

---

## 7. Phase 6: AI Evolution — Core Financial Intelligence
- **"Can I Afford This?" Purchase Decision Simulator**:
  - `POST /api/v1/ai/simulate-purchase` evaluating requested purchases against remaining budget buffers, daily safe allowances, and burn velocity.
  - Frontend [`PurchaseSimulatorDialog.tsx`](frontend/src/features/dashboard/components/PurchaseSimulatorDialog.tsx) with before/after buffer impact and 1-click "Add as Expense".
- **Safe-to-Spend Speedometer & Daily Allowance Gauge**:
  - `GET /api/v1/ai/safe-to-spend` calculating daily spending targets, pacing velocity, and budget depletion dates.
  - Frontend [`SafeToSpendCard.tsx`](frontend/src/features/dashboard/components/SafeToSpendCard.tsx) on Dashboard with dynamic status glows and pacing feedback.
- **Financial Health Score (0–100 Deterministic Index)**:
  - `GET /api/v1/ai/health-score` computing composite scores across Budget Adherence (40 pts), Savings Velocity (35 pts), and Category Discipline (25 pts).
- **Micro-Spending "Leak Hunter"**:
  - `GET /api/v1/ai/leak-analysis` detecting repetitive sub-threshold transactions and projecting annual drain.
  - Frontend [`LeakHunterDialog.tsx`](frontend/src/features/dashboard/components/LeakHunterDialog.tsx) with customizable thresholds (`≤ ₹100`, `≤ ₹150`, `≤ ₹250`, `≤ ₹500`).
- **Subscription & Recurring Commitments Audit**:
  - `GET /api/v1/ai/subscription-audit` identifying periodic costs and flagging duplicate services.
  - Frontend [`SubscriptionAuditDialog.tsx`](frontend/src/features/dashboard/components/SubscriptionAuditDialog.tsx) accessible from the Subscriptions card.
- **Multi-Account In-App Switcher**:
  - Backend: `POST /api/v1/auth/switch-account` with token rotation and user session validation.
  - Frontend: Multi-account vault in [`AuthContext.tsx`](frontend/src/features/auth/context/AuthContext.tsx), topbar switcher in [`shell.tsx`](frontend/src/components/layout/shell.tsx), and [`AddAccountModal.tsx`](frontend/src/features/auth/components/AddAccountModal.tsx) with Google OAuth and email/password support.

- **Dual-Mode Omnichannel Registration (6-Digit OTP + Cross-Device Auto-Sync)**:
  - Database: Alembic migration `a1b2c3d4e5f6` adding `otp_code_hash` and `is_verified` columns with composite index.
  - Backend: Endpoints `POST /api/v1/auth/register/verify-otp` (instant 1-step account creation via code) and `GET /api/v1/auth/register/status` (live background polling for mobile approval).
  - Brevo Email: Formats email with prominent 6-digit OTP code badge in subject line and body + 1-click magic link button.
  - Frontend: [`RegisterForm.tsx`](frontend/src/features/auth/components/RegisterForm.tsx) featuring a 6-digit code input, password setup, real-time background listener (`checkRegistrationStatus`), and navigation safeguards.

---

## 8. Phase 7: Supercharged Automation, Frameworks & Engagement
- **Voice Expense Quick-Add (Web Speech API)**:
  - Hands-free expense logging directly into the Quick Add bar in [`ExpenseFormDialog.tsx`](frontend/src/features/expenses/components/ExpenseFormDialog.tsx).
  - Uses native browser `SpeechRecognition` / `webkitSpeechRecognition` API (zero third-party SDK dependencies or fees) with Indian English (`en-IN`) and general English support.
  - Features real-time pulsing mic recording states and instant transcription auto-dispatch into the natural language parser pipeline.
- **Indian Bank & UPI SMS Transaction Parser**:
  - `POST /api/v1/ai/parse-sms` endpoint in [`ai_service.py`](backend/app/services/ai_service.py) with specialized heuristics for Indian financial SMS formats (HDFC, SBI, ICICI, Axis, Kotak, Paytm, PhonePe, Google Pay, CRED).
  - Automatically extracts amount, merchant, date, payment method (UPI, Card, NetBanking), and UTR reference number while scrubbing sensitive card/account numbers.
  - Frontend collapsible "📱 SMS" paste drawer inside [`ExpenseFormDialog.tsx`](frontend/src/features/expenses/components/ExpenseFormDialog.tsx) for 1-click autofill.
- **Smart Duplicate Transaction Guard**:
  - `POST /api/v1/ai/check-duplicate` and repository method `find_potential_duplicates` querying candidate transactions against user ledger within a ±2 day tolerance window.
  - Frontend live warning banner in [`ExpenseFormDialog.tsx`](frontend/src/features/expenses/components/ExpenseFormDialog.tsx) alerting users to existing identical charges before recording.
- **50/30/20 Budget Optimization Framework**:
  - `GET /api/v1/ai/fifty-thirty-twenty` deterministically partitioning monthly expenses into Needs (50%), Wants (30%), and Savings (20%), computing variances, adherence scores (0–100), and personalized rebalancing tips.
  - Frontend [`FiftyThirtyTwentyCard.tsx`](frontend/src/features/dashboard/components/FiftyThirtyTwentyCard.tsx) with tri-color segmented progress bar mounted on Dashboard and Budget views.
- **Financial Streaks & Discipline Achievements**:
  - `GET /api/v1/ai/achievements` tracking gamified streaks (Budget Champion, Leak Hunter Master, Consistency Ace, Discipline Titan) and dynamic motivational quotes.
  - Frontend [`AchievementsCard.tsx`](frontend/src/features/dashboard/components/AchievementsCard.tsx) with progress rings and milestone badges on the Dashboard.
- **"Finny" — Interactive Financial Mood Mascot**:
  - Interactive SVG-based mascot [`FinnyMascot.tsx`](frontend/src/features/dashboard/components/FinnyMascot.tsx) in the Dashboard header.
  - Dynamically updates facial expressions and glowing mood states (Joyful, Calm, Alert, Stressed) based on real-time financial health score and burn velocity, with interactive speech bubble advice.
- **Persistent Workspace Agent Repo Memory (`.agents/`)**:
  - Created `.agents/rules/paradox-architecture.md` and `.agents/rules/paradox-memory.md` to permanently anchor architectural constraints, database conventions, and milestone states across all future AI sessions.

---

## 10. Responsive UI, Gemini Receipt OCR Auto-Fill, and Dynamic Category Expansion
- **Responsive Layout Engine & AI Quick Add Redesign**:
  - Reorganized `ExpenseFormDialog` into a 2-tier responsive structure:
    - **Row 1**: 100% width natural language input with embedded Voice Recognition mic inside right edge + `Auto-Fill` primary button.
    - **Row 2**: Secondary action tools `[ 📷 Scan Bill / Receipt ]` and `[ 💬 Paste Bank SMS ]` in a wrapping container (`flex-wrap gap-2`).
    - Eliminates all horizontal modal overflow and button squishing on mobile (< 400px) and desktop viewports.
  - Enhanced `Dialog.tsx` with responsive width scaling (`max-w-lg w-full`), mobile padding (`p-4 sm:p-6`), and safe vertical scroll containment (`max-h-[90vh] overflow-y-auto`).
  - Wrapped period filter tabs in `DashboardView.tsx` (`flex-wrap sm:flex-nowrap w-full sm:w-auto`).
- **Gemini Vision Receipt OCR Auto-Fill**:
  - Aligned model calls to verified Google Generative AI models: `["gemini-2.5-flash", "gemini-flash-latest", "gemini-2.0-flash", "gemini-1.5-flash"]` with `responseMimeType: "application/json"`.
  - Hardened regex extractors for monetary amounts and dates (ISO, DD-MM-YYYY, DD/MM/YYYY).
  - Scanning a receipt now immediately populates Amount, Date, Description (merchant/item), Category, and Payment Method.
- **Dynamic Category Recommendation & 1-Click "Add & Select"**:
  - Added `is_new_category` detection to Gemini categorization and semantic heuristic engines.
  - When typing in Description, if the detected category exists in the user's category list, it suggests `AI Suggests: [Category Name] [ Apply ]`.
  - If the category does not exist yet (e.g. Pets, Healthcare, Fitness, Travel, Subscriptions, Gifts), it displays:
    `✨ Category "[Name]" doesn't exist yet. [ + Add & Select ] [ ✕ ]`
  - Clicking `[ + Add & Select ]` creates the category in the backend database via `handleCreateCategory`, updates local category state, selects it immediately, and displays a confirmation toast.

---

## 11. Testing & Build Verification
- **Backend Tests (`pytest`)**:
  - `49 passed in 4.42s` (100% green).
  - Added test coverage in [`test_ai.py`](backend/tests/unit/test_ai.py) for receipt OCR payload extraction and `is_new_category` dynamic recommendations.
- **Frontend Builds (`next build`)**:
  - Next.js 16.3.3 + Turbopack compiles successfully with 0 TypeScript/ESLint errors across all 14 static/dynamic routes.

---

## 12. Current Session Handoff Notes
- All features across Phases 1 through 7, including Responsive Layouts, Gemini Vision OCR, and Category Expansion, are fully operational, tested, and documented.
- Active servers:
  - Backend: `http://127.0.0.1:8000` (FastAPI + Uvicorn)
  - Frontend: `http://localhost:3000` (Next.js 16 App Router)
- Database:
  - Latest migration: `a1b2c3d4e5f6` (OTP & is_verified on pending_registration_tokens).
- Testing:
  - Backend: `.venv\Scripts\pytest tests/ -v` -> 49/49 passed.
  - Frontend: `npm.cmd run build` -> 0 errors.
- **Documentation & Agent Memory**:
  - `.agents/rules/paradox-architecture.md` & `.agents/rules/paradox-memory.md` maintained as persistent repo memory.
  - `docs/PARADOX_AI_FEATURES_UPDATED.md` maintained as the master roadmap.
- For local testing:
  - PostgreSQL: `E:\PSQL\bin\postgres.exe -D "E:\PSQL\data"`
  - Backend: `.venv\Scripts\uvicorn app.main:app --port 8000`
  - Frontend: `npm.cmd run dev`


