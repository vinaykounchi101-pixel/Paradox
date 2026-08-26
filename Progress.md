# Progress Report: Paradox Project (Backend & Database Setup)

This document summarizes the current status, configurations, architectural decisions, and next steps for **Paradox Phase 1 Core MVP**. It serves as a handover handoff document for the next development session.

---

## 1. Project Rules & Guidelines
- **Folder Structure**: Follows the layout specified in the SRS.
- **Environment Driven**: The application is strictly environment-variable driven.
- **Secrets Constraint**: 
  - **Do NOT read, edit, or access `.env` or `.env.local` files** from any agent tool call.
  - The application retrieve all values dynamically from the OS process environment.
  - `.env.local` is ignored in both the backend and frontend `.gitignore` configurations.
- **Push Policy**: Do **not** commit or push changes to git until explicitly instructed by the user.

---

## 2. Directory Structure Setup
The project workspace has been created with decoupled frontend and backend trees:
- **`backend/`**: FastAPI implementation, Alembic migrations, database models, schemas, repositories, services, and pytest unit tests.
- **`frontend/`**: Next.js App Router boilerplate, features directories (expenses, categories, budget, dashboard), and API client integrations.
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
- **Core Configurations**: Environment settings parsed via Pydantic `BaseSettings`. Automatic fallback helper to convert `postgresql://` string prefixes to `postgresql+asyncpg://` to prevent asyncpg driver errors.
- **Domain Exceptions**: Mapping rules to HTTP status codes (`NotFoundError` -> 404, `ValidationError` -> 422, `ConflictError` -> 409, `UnprocessableRequestError` -> 422) with a custom JSON error-response envelope.
- **Pydantic Schemas**: Serializing decimal values as string strings with 2 decimal places to avoid floating-point loss in transit.
- **Request Logger**: Custom middleware logging HTTP method, path, status code, and processing duration in milliseconds.
- **Structured JSON Logging**: Standard JSON formatter printing logs cleanly to stdout.
- **Health Check Router**: Custom endpoint performing health checks and verifying DB connectivity.
- **Development Server**: Uvicorn server initialized and running locally in the background on `http://127.0.0.1:8000`.

---

## 5. Verification & Seeding
- **Unit Test Suite**: Successfully wrote unit tests (`backend/tests/unit/`) to verify money decimal rounding (`test_money.py`) and budget threshold state derivations (`test_budget_status.py`). Executed `pytest` inside the virtual environment (`.venv/`) and verified all tests pass cleanly.
- **Dummy Data Seed Script**: Developed [`seed_dummy_data.py`](file:///E:/Projects/Paradox/backend/app/db/seed_dummy_data.py). When executed, it seeds a monthly budget of 1200.00 and 10 calendar-month expenses distributed across standard categories and payment methods for the current month.

---

## 6. Handover: How to Run the Backend Locally

To boot the database locally and seed it with dummy data:
1. Load your environment variables from `.env.local` to the shell.
2. Run migrations:
   ```powershell
   .venv\Scripts\alembic upgrade head
   ```
3. Seed the test database:
   ```powershell
   .venv\Scripts\python app/db/seed_dummy_data.py
   ```
4. Run the development server (runs in the background automatically if keeping the current process, or can be started manually):
   ```powershell
   .venv\Scripts\uvicorn app.main:app --reload
   ```

---

## 7. Next Steps for Next Session
1. **Database migrations execution**: Apply the migrations on the local database.
2. **Next.js Frontend Development**:
   - Install React Query (TanStack Query), Framer Motion, and Zod in `frontend/`.
   - Setup the UI primitive components (Button, Input, Select, Card) aligning with the Design System tokens.
   - Implement the feature modules (expenses list, add expense form, category management list, budget edit, dashboard widgets).
   - Hook up frontend API queries using TanStack Query, referencing the backend API endpoints running at `http://127.0.0.1:8000`.
