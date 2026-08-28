# Progress & Technical Architecture Report: Paradox Project

This document provides a comprehensive summary of all architectural implementations, feature additions, database migrations, Progressive Web App (PWA) configurations, testing suites, Postman API collections, and deployment states for **Paradox Phase 1 MVP**.

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
│   │   │   └── ui/              # Button, Card, TiltCard, Dialog, Toast, CategoryPicker, BarChart3D
│   │   ├── features/            # Feature-scoped hooks, components, and views
│   │   │   ├── dashboard/
│   │   │   ├── expenses/
│   │   │   ├── categories/
│   │   │   └── budget/
│   │   ├── lib/api/             # Typed API client services
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

## 5. Progressive Web App (PWA) Implementation
- **Manifest (`public/manifest.json` & `app/manifest.ts`)**:
  - Configured with `display: "standalone"`, `start_url: "/"`, theme `#6366f1`, and background `#09090b`.
- **App Icons**:
  - High-resolution SVG icons: `icon-192.svg`, `icon-512.svg`, and `icon-maskable.svg`.
- **Service Worker (`public/sw.js`)**:
  - Network-first caching strategy for navigation and HTML documents to eliminate `ERR_FAILED` or stale-cache locks.
  - Cache-first strategy for static image assets and icons.
  - Excludes API requests (`/api/*`) and Next.js internal data (`/_next/data/*`).
- **PWA Auto-Registration (`PwaRegister.tsx`)**:
  - Checks for HTTPS and browser capability on window load, handling update lifecycles automatically.

---

## 6. UI & Navigation (Hamburger Drawer Layout)
- **Top Navigation Bar**:
  - Sticky glass header with 3D perspective animated grid background.
  - Rotating 3D Paradox logo cube + gradient title.
  - Animated Hamburger Menu toggle (`Menu` ➔ `X`).
  - Dark / Light mode toggle and session avatar.
- **Slide-Out Hamburger Drawer**:
  - Full backdrop blur overlay (`backdrop-blur-sm bg-black/60`).
  - Smooth Framer Motion spring slide-in panel.
  - Direct navigation links with icons, descriptions, and active spring highlight indicators.
  - Dark/Light mode switcher and Live PWA online status badge.

---

## 7. API Testing & Postman Test Suites
- **Postman Collection (v2.1.0)**: `Paradox.postman_collection.json`
  - 100% endpoint coverage across 6 modules: `health`, `dashboard`, `categories`, `payment-methods`, `expenses`, `budget`.
  - Built-in dynamic ID capture scripts for automated execution chaining.
- **Async SQLAlchemy Model Refresh Fix**:
  - Applied explicit `await self.db.refresh(instance)` across `CategoryRepository`, `PaymentMethodRepository`, and `BudgetRepository` to eliminate lazy-loading `MissingGreenlet` exceptions during serialization of `updated_at`.
- **Postman Environments**:
  - `Paradox.postman_environment.json`: Local backend (`http://localhost:8000`).
  - `Paradox.postman_production_environment.json`: Live Render service (`https://paradox-api.onrender.com`).
- **Documentation**: `docs/POSTMAN_TESTING_GUIDE.md`
  - Step-by-step import instructions, testing workflow, Newman CLI instructions, and HTTP status code reference.
- **Backend Tests (`pytest`)**:
  - `tests/unit/test_budget_granularity.py`: Passed (100% assertions for month, week, day schemas, validation, and serializers).
  - `tests/unit/test_budget_status.py`: Passed (Budget threshold math & warning ranges).
  - `tests/unit/test_money.py`: Passed (Decimal precision verification).
  - **Result**: `3 passed in 0.51s`.
- **Frontend Builds (`next build`)**:
  - Next.js 16.3.3 + Turbopack compiles successfully with 0 TypeScript/ESLint errors.

---

## 8. Deployment Information
- **Live Frontend**: [https://paradox-neon.vercel.app/](https://paradox-neon.vercel.app/)
- **Live Backend**: Render Web Service connected to Supabase PostgreSQL.
- **Latest Commit Pushed**: `cfe61d5` (All features, tests, migrations, PWA assets, and Postman suites synchronized with `origin/main`).

---

## 9. Next Session Handoff Notes
- All requested features (3D elements, unrestricted categories, multi-granularity budgets, PWA, hamburger navigation, Postman suites, and repository model refresh fixes) are fully implemented and verified.
- For local testing:
  - Backend: Run `.venv\Scripts\uvicorn app.main:app --port 8000` from `backend/`.
  - Frontend: Run `npm run dev` from `frontend/`.
  - Postman CLI: Run `npx newman run Paradox.postman_collection.json -e Paradox.postman_environment.json`.
