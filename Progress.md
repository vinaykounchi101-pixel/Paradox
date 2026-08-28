# Progress & Technical Architecture Report: Paradox Project

This document provides a comprehensive summary of all architectural implementations, feature additions, database migrations, Progressive Web App (PWA) configurations, testing suites, Postman API collections, UI enhancements, and deployment states for **Paradox Phase 1 MVP**.

---

## 1. Core Project Guidelines & Constraints
- **Strict Architecture**: Layered architecture (**API/Router ➔ Service Layer ➔ Repository Layer ➔ Database**).
- **Zero Inline Hardcoding**: All configurations, URLs, categories, budgets, and tokens are dynamically managed or environment-driven.
- **Secrets & Environment Isolation**: `.env` and `.env.local` are strictly ignored by Git and never committed or directly accessed by tools.
- **Precision Monetary Values**: Uses fixed-precision `Numeric(12, 2)` (Python `Decimal`) with database `CHECK (amount > 0)` and `CHECK (amount >= 0)` constraints.
- **Communication Conventions**: Follows pair-programming callout rules (`"Roger That"` before starting work, `"Over n Out"` upon completion, and `"Signing off"` upon session closure).

---

## 2. Directory Layout & Module Structure
```
Paradox/
├── backend/
│   ├── app/
│   │   ├── api/                 # FastAPI router endpoints (expenses, categories, payment_methods, budget, dashboard, health)
│   │   ├── constants/           # Starter system UUIDs & definitions
│   │   ├── core/                # App configuration, logging, and error handling
│   │   ├── db/
│   │   │   ├── models/          # SQLAlchemy 2.0 ORM models (User, Category, PaymentMethod, Expense, Budget)
│   │   │   ├── session.py       # Async engine & sessionmaker
│   │   │   └── seed_dummy_data.py # Idempotent starter data seed (no fake financial data)
│   │   ├── repositories/        # Database access layer (CRUD, queries, atomic cascade reassignments)
│   │   ├── schemas/             # Pydantic validation models & serializers
│   │   ├── services/            # Pure business logic layer
│   │   └── utils/               # Datetime and formatting utilities
│   ├── migrations/              # Alembic asynchronous migrations
│   │   └── versions/            # 80a4bc410a4b, b2f8a1c9d4e5, c3a7b9e1f5d2
│   ├── tests/                   # Pytest automated test suites
│   ├── Dockerfile               # Production container config for Render
│   └── requirements.txt         # Production backend dependencies
├── frontend/
│   ├── public/
│   │   ├── icons/               # PWA icons (icon-192.svg, icon-512.svg, icon-maskable.svg)
│   │   ├── manifest.json        # Web App Manifest
│   │   └── sw.js                # Resilient Service Worker (network-first navigation)
│   ├── src/
│   │   ├── app/                 # Next.js App Router (layout, dashboard, expenses, categories, budget, manifest.ts)
│   │   ├── components/
│   │   │   ├── common/          # PwaRegister, BackgroundGrid, ThemeProvider, QueryProvider
│   │   │   ├── layout/          # Shell navigation with Hamburger Drawer & Topbar
│   │   │   └── ui/              # Button, Card, TiltCard, Dialog, Toast, CategoryPicker, PaymentMethodDropdown, BarChart3D
│   │   ├── features/            # Feature-scoped hooks, components, and views
│   │   │   ├── dashboard/
│   │   │   ├── expenses/
│   │   │   ├── categories/
│   │   │   └── budget/
│   │   ├── lib/api/             # Typed API client services (with precise error extraction)
│   │   └── styles/              # Global CSS & Design System tokens
│   └── vercel.json              # Vercel deployment framework configuration
├── docs/                        # PRD, SRS, Design System, and POSTMAN_TESTING_GUIDE.md
├── Paradox.postman_collection.json # Full Postman collection (v2.1.0) with automated variable chaining
├── Paradox.postman_environment.json # Postman local environment configuration
└── Paradox.postman_production_environment.json # Postman production/Render environment configuration
```

---

## 3. Database Architecture & Alembic Migrations
- **Database Engine**: PostgreSQL on Supabase (accessed asynchronously via `asyncpg` and synchronously via `psycopg2` during migrations).
- **Migration History**:
  1. `80a4bc410a4b_initial_schema.py`: Initial schema for `users`, `categories`, `payment_methods`, `expenses`, and `budgets`.
  2. `b2f8a1c9d4e5_add_month_to_budgets.py`: Added `month` column with raw idempotent SQL.
  3. `c3a7b9e1f5d2_add_period_granularity_to_budgets.py`:
     - Added `period_type` (`"month" | "week" | "day"`) and `period_key` (`"YYYY-MM"`, `"YYYY-Www"`, `"YYYY-MM-DD"`).
     - Idempotent raw DDL: `ALTER TABLE budgets ADD COLUMN IF NOT EXISTS ...`
     - Enforced composite constraint `uq_budget_period (period_type, period_key)`.
- **Atomic Cascade Reassignments**:
  - When a category or payment method is deleted, referencing expenses are atomically reassigned to remaining fallback entities in the exact same database transaction block.
- **No Dummy Auto-Seeding**: Automatic creation of placeholder financial budgets on boot has been completely eliminated to maintain database integrity.

---

## 4. Multi-Granularity Budget System
- **Budgeting Granularities**:
  - **Monthly**: Target identified by `YYYY-MM` (e.g. `2026-08`).
  - **Weekly**: Target identified by ISO week `YYYY-Www` (e.g. `2026-W35`).
  - **Daily**: Target identified by date `YYYY-MM-DD` (e.g. `2026-08-28`).
- **REST Endpoints**:
  - `GET /api/v1/budget?period_type=...&period_key=...`
  - `GET /api/v1/budget/all?period_type=...`
  - `PUT /api/v1/budget` (Upsert target)
  - `DELETE /api/v1/budget?period_type=...&period_key=...`
- **Frontend Budget Planner (`/budget`)**:
  - Granularity switcher tabs (**Monthly**, **Weekly**, **Daily**).
  - Dynamic native date pickers (`<input type="month" />`, `<input type="week" />`, `<input type="date" />`).
  - Real-time audit meter, spending limit slider, and status alerts.
  - History table of all configured budgets with edit/delete dialogs and category/period filtering.

---

## 5. UI, PWA & Component Upgrades
- **Progressive Web App (PWA)**:
  - Configured with `display: "standalone"`, `start_url: "/"`, theme `#6366f1`, background `#09090b`.
  - Resilient network-first Service Worker ([`sw.js`](file:///E:/Projects/Paradox/frontend/public/sw.js)) & auto-registration.
- **Hamburger Navigation Drawer**:
  - Backdrop blur overlay, smooth Framer Motion spring slide-in panel, theme toggle, and 3D rotating Paradox cube.
- **Custom Payment Method Dropdown (`PaymentMethodDropdown.tsx`)**:
  - Animated dropdown menu with context-aware icons (Cash, Credit/Debit Card, Bank Transfer, Digital/Crypto Wallet, UPI/Mobile).
  - Clean active checkmark indicators and outside-click dismissal.
- **Smart Modal State Sync & Fallback**:
  - Safely falls back to valid default categories/payment methods if an edited expense references a previously deleted custom entity.

---

## 6. Backend Fixes & Schema Integrity
- **Pydantic Schema Scope Shadowing Fix (`ExpenseUpdate`)**:
  - Aliased `from datetime import date as date_type` in `backend/app/schemas/expense.py` to prevent the field name `date` from shadowing the Python type annotation, resolving `Input should be None` validation errors during expense editing.
- **Async SQLAlchemy Model Refresh Fix**:
  - Added explicit `await self.db.refresh(instance)` and `execution_options(populate_existing=True)` across `ExpenseRepository`, `CategoryRepository`, `PaymentMethodRepository`, and `BudgetRepository` to eliminate lazy-loading `MissingGreenlet` exceptions during serialization of `updated_at`.
- **API Error Unpacking (`client.ts`)**:
  - Enhanced frontend API client to unpack `errorJson.error.message` and field validation arrays for accurate toast feedback.

---

## 7. API Testing & Postman Test Suites
- **Postman Collection (v2.1.0)**: `Paradox.postman_collection.json`
  - 100% endpoint coverage across 6 modules: `health`, `dashboard`, `categories`, `payment-methods`, `expenses`, `budget`.
  - Built-in dynamic ID capture scripts for automated execution chaining.
  - Dynamic `{{$randomInt}}` naming to eliminate duplicate name conflicts (`409`).
  - 35 automated assertion scripts (`pm.test`).
- **Newman CLI Verification**:
  - **Result**: `30 requests executed, 35 assertions passed, 0 failed, 0 errors in 2.3s` (100% green).
- **Backend Tests (`pytest`)**:
  - `tests/unit/test_budget_granularity.py`: Passed.
  - `tests/unit/test_budget_status.py`: Passed.
  - `tests/unit/test_money.py`: Passed.
  - **Result**: `3 passed in 0.56s`.
- **Frontend Builds (`next build`)**:
  - Next.js 16.3.3 + Turbopack compiles successfully with 0 TypeScript/ESLint errors.

---

## 8. Deployment Information
- **Live Frontend**: [https://paradox-neon.vercel.app/](https://paradox-neon.vercel.app/)
- **Live Backend**: Render Web Service connected to Supabase PostgreSQL.

---

## 9. Next Session Handoff Notes
- All requested features, Postman suites, repository model refresh fixes, ExpenseUpdate schema fixes, and the custom PaymentMethodDropdown component are fully implemented, verified, and pushed.
- For local testing:
  - Backend: Run `.venv\Scripts\uvicorn app.main:app --port 8000` from `backend/`.
  - Frontend: Run `npm run dev` from `frontend/`.
  - Postman: Open Collection Runner and execute `Paradox API Collection` (or `npx newman run Paradox.postman_collection.json -e Paradox.postman_environment.json`).
