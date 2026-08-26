# Progress Report: Paradox Project (Backend & Frontend Implementation)

This document summarizes the final status, configurations, architectural decisions, and verification results for **Paradox Phase 1 MVP**. It serves as a comprehensive overview of the entire project lifecycle.

---

## 1. Project Rules & Guidelines
- **Folder Structure**: Follows the strict layout specified in the SRS.
- **Environment Driven**: The application is strictly environment-variable driven.
- **Secrets Constraint**: 
  - **Do NOT read, edit, or access `.env` or `.env.local` files** from any agent tool call.
  - The application retrieves all values dynamically from the OS process environment.
  - `.env.local` is ignored in both the backend and frontend `.gitignore` configurations.
- **Push Policy**: Commits and pushes are only executed upon explicit instructions from the user.

---

## 2. Workspace Directory Layout
The project workspace consists of decoupled frontend and backend modules:
- **`backend/`**: FastAPI implementation, Alembic migrations, database models, schemas, repositories, services, and pytest unit tests.
- **`frontend/`**: Next.js App Router workspace, features directories, API client integrations, and styling sheets.
- **`docs/`**: Holds product-level specifications (`PARADOX_PRD_FINAL.md`, `PARADOX_SRS.md`, and `DESIGN SYSTEM.md`).

---

## 3. Database Schema & Migration Layer
We have implemented the relational database mappings in SQLAlchemy 2.0 and initialized Alembic async migrations:
- **Entities**:
  - `users`: Placeholder table for future multi-user compatibility (seeded with 1 primary user).
  - `categories`: Unique category names. Custom categories can be deleted. Default starter categories are protected (`is_default = true`) and cannot be deleted.
  - `payment_methods`: Unique payment methods. Custom methods can be deleted; default methods are protected.
  - `expenses`: Fixed-precision `amount` (using `Numeric(12,2)` with `CHECK (amount > 0)`), category, payment method, date, and description. Indexes are placed on `date` and `category_id`.
  - `budgets`: Singular monthly budget resource upserted on `PUT` requests (`CHECK (amount >= 0)`).
- **Atomic Cascade Reassignments**:
  - Deleting a custom category reassigns referencing expenses to the default `Uncategorized` fallback category in the **same transaction block** before deleting the category row.
  - Deleting a custom payment method reassigns referencing expenses to the default `Other` fallback payment method in the same transaction block.
- **Seeded Seeds**: Sourced via Alembic upgrade with fixed UUIDs for starter categories and payment methods (defined in `app/constants/categories.py` and `app/constants/payment_methods.py`).

---

## 4. Backend Implementation Details
The backend service (`backend/app/`) is fully implemented with a structured layered architecture (**Router ➔ Service ➔ Repository ➔ Database**):
- **Core Configurations**: Environment settings parsed via Pydantic `BaseSettings`. Fallback helper to convert `postgresql://` string prefixes to `postgresql+asyncpg://` to prevent asyncpg driver errors.
- **Domain Exceptions**: Mapping rules to HTTP status codes (`NotFoundError` -> 404, `ValidationError` -> 422, `ConflictError` -> 409, `UnprocessableRequestError` -> 422) with a custom JSON error-response envelope.
- **Pydantic Schemas**: Serializing decimal values as string representations with 2 decimal places to avoid floating-point loss in transit.
- **Request Logger**: Custom middleware logging HTTP method, path, status code, and processing duration in milliseconds.
- **Structured JSON Logging**: Standard JSON formatter printing logs cleanly to stdout.
- **Health Check Router**: Custom endpoint performing health checks and verifying DB connectivity.
- **Development Server**: Uvicorn server running locally in the background on `http://127.0.0.1:8000`.

---

## 5. Frontend Implementation Details
The frontend web application (`frontend/`) is fully implemented with **Next.js (App Router), TypeScript, Tailwind CSS v4, Framer Motion, and TanStack Query**:
- **Central API Client**: Built custom Client wrapper that handles REST requests and maps backend validation error dictionaries to visual inputs.
- **State Synchronizer**: React Query configured with a 30s cache stale-time threshold to balance DB overhead with UI freshness.
- **Global Theme Switcher**: Installed client-side `ThemeProvider` supporting transitions between a premium dark glass theme and a slate light theme. Persistent selection is stored in `localStorage` and loads without SSR hydration mismatches.
- **Premium SVG Visualizations**:
  - **Spending Trends**: Custom area chart displaying weekly spending aggregates with bezier lines, gradient fills, and cursor hover tooltip boxes.
  - **Spending Categories**: Custom vertical bar chart displaying category spending totals. Hovering over a bar scales it and shows a floating cursor tooltip indicating category name and amount spent.
- **Navigation Shell**: A sidebar (desktop layout) that collapses into a bottom navigation bar (mobile viewport) for absolute accessibility.
- **Feature Screen Suites**:
  - **Dashboard Page**: Displays period aggregations, spending totals, budget consumption status bars, category bar charts, trend curves, and recent transactions.
  - **Expense List**: Handles paginated, sorted lists with mutually exclusive single-dimension search filters (picking a category clears date ranges; selecting date ranges clears category filter). Dialog modals manage creations, edits, and deletions.
  - **Metadata Manager**: Tabbed view displaying Categories and Payment Methods. Protects system defaults and handles warnings for cascade items.
  - **Budget Configurator**: Sliders let users update thresholds, showing green (normal), yellow (>=80%), and red (exceeded) status indicators.

---

## 6. How to Run the Project Locally

### A. Run the Backend Server
1. Load environment variables.
2. Run database migrations:
   ```powershell
   .venv\Scripts\alembic upgrade head
   ```
3. Seed the test database:
   ```powershell
   .venv\Scripts\python app/db/seed_dummy_data.py
   ```
4. Run the development server (available on `http://127.0.0.1:8000`):
   ```powershell
   .venv\Scripts\uvicorn app.main:app
   ```

### B. Run the Frontend Server
1. Navigate to the `frontend/` directory.
2. Run development server (available on `http://localhost:3000`):
   ```powershell
   npm run dev
   ```

---

## 7. Verification Summary
* **Backend Tests**: Executed `pytest` inside the virtual environment; all unit tests pass cleanly.
* **Frontend Builds**: Executed `npm run build` inside `frontend/`; compiled production package with zero TypeScript or ESLint errors.
* **DB Liveness Check**: Verified liveness checks connected successfully to PostgreSQL (`{"status":"ok","database":"connected"}`).
* **Repository State**: Cleanly pushed tracking states to the remote repository.

---

## 8. Deployment Configuration (Vercel + Render + Supabase)

### A. Changes Made for Production Deployment
- **`backend/Dockerfile`**: Populated with a production-ready multi-stage configuration using `python:3.11-slim`. Dynamically binds to the `$PORT` environment variable required by Render.
- **`backend/app/core/config.py`**: Extended the `DATABASE_URL` validator to handle both `postgresql://` and `postgres://` URL prefixes (converting both to `postgresql+asyncpg://`). This ensures full compatibility with Supabase connection strings.
- **`backend/app/services/dashboard_service.py`**: Fixed a deploy-time `NameError` — `Any` was used in a type annotation but was missing from the `typing` import. Added `Any` to the import line.
- **`frontend/vercel.json`**: Created with explicit `"framework": "nextjs"` and `"outputDirectory": ".next"` so Vercel correctly detects the Next.js framework when deploying from a monorepo subdirectory.

### B. Required Environment Variables

#### Render (Backend)
| Variable | Value |
|---|---|
| `DATABASE_URL` | Supabase connection string (`postgres://...`) |
| `CORS_ALLOWED_ORIGINS` | Vercel frontend URL (e.g. `https://paradox.vercel.app`) |
| `APP_ENV` | `production` |
| `DEBUG` | `false` |
| `LOG_LEVEL` | `INFO` |

#### Vercel (Frontend)
| Variable | Value |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | Render backend URL + `/api/v1` (e.g. `https://paradox-backend.onrender.com/api/v1`) |
| `NEXT_PUBLIC_APP_ENV` | `production` |

> **Important**: After setting `NEXT_PUBLIC_*` variables on Vercel, a full **Redeploy** must be triggered for the values to be baked into the static build output.

### C. Database Setup (Supabase)
1. Create a Supabase project and copy the PostgreSQL connection string.
2. Set `DATABASE_URL` in the local shell temporarily and run:
   ```powershell
   .venv\Scripts\alembic upgrade head
   ```
   This creates all tables and seeds starter categories, payment methods, and the placeholder primary user.

### D. API Endpoint Audit
All frontend API call paths were audited against backend router definitions. **No mismatches found.** All routes for expenses, categories, payment-methods, budget, and dashboard are correctly aligned.

### E. Commits Pushed (This Session)
| Commit | Description |
|---|---|
| `1d604fc` | `chore: configure backend dockerfile and postgresql connection validation for render and supabase deployment` |
| `007c4e2` | `fix: import Any in dashboard_service to resolve deploy time NameError` |
| `f1270d1` | `fix: add vercel.json to configure Next.js framework and output directory` |
| `19b985c` | `docs: update progress.md and technical debt with deployment notes` |
| `3af1998` | `feat: run alembic migrations automatically on startup via lifespan event` |

---

## 9. Auto-Migration on Startup

### Problem
Tables were not being created in Supabase because `alembic upgrade head` was never run
against the production database. The Render backend would boot successfully (health check OK)
but all API endpoints returned HTTP 500 because no tables existed.

### Solution
Added a `lifespan` startup event in `backend/app/main.py` that runs `alembic upgrade head`
automatically every time the server boots:
- Derives a **synchronous** `postgresql://` URL from the `DATABASE_URL` env var (stripping `+asyncpg`) so Alembic's sync runner works correctly.
- Uses `alembic.command.upgrade(cfg, "head")` via the Alembic scripting API.
- Logs `INFO` on success and `ERROR` (with full traceback) on failure.
- Added `psycopg2-binary>=2.9.9` to `requirements.txt` — required by Alembic's sync engine.

### Behavior
- If tables already exist, Alembic detects the migration is already at `head` and skips — **idempotent, safe to run on every restart**.
- If tables are missing, they are created and seeded before the first request is served.
