# Progress & Technical Architecture Report: Paradox Project

This document provides a comprehensive summary of all architectural implementations, feature additions, database migrations, Progressive Web App (PWA) configurations, testing suites, Postman API collections, UI enhancements, and deployment states for **Paradox Phase 1 MVP, Phase 2 Multi-Granularity Budgeting, and Phase 3 Production Authentication & Multi-Tenant Data Isolation**.

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

## 4. Multi-Granularity Budget System
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

## 6. Testing & Build Verification
- **Backend Tests (`pytest`)**:
  - `tests/unit/test_auth.py`: Password hashing, JWT sign/decode, token rotation, registration/login flows. (PASSED)
  - `tests/unit/test_email.py`: Brevo REST API, Resend REST API, and SMTP email dispatching, unconfigured safety, and provider resolution. (PASSED)
  - `tests/unit/test_pre_registration_verification.py`: Pre-registration token generation, token validation, user creation, and expiry handling. (PASSED)
  - `tests/unit/test_user_isolation.py`: Cross-tenant data isolation and dynamic date scoping. (PASSED)
  - `tests/unit/test_budget_granularity.py`: Budget schemas across all granularities. (PASSED)
  - `tests/unit/test_budget_status.py`: 90% / 100% budget threshold calculations. (PASSED)
  - `tests/unit/test_money.py`: Monetary rounding and decimal precision. (PASSED)
  - **Result**: `23 passed in 2.02s` (100% green).
- **Frontend Builds (`next build`)**:
  - Next.js 16.3.3 + Turbopack compiles successfully with 0 TypeScript/ESLint errors across all 14 routes (including `/register/complete`).

---

## 7. Next Session Handoff Notes
- **Pre-Registration Email Verification**:
  - Unverified accounts are never created in the database until the email magic link is clicked and validated.
  - Route `/register/complete?token=...` allows verified users to set their Display Name and Password.
  - Alembic migration `e5f9a2b3c4d1_add_pending_registration_tokens` manages token hashing and expiration.
- **Universal Multi-Provider Email Service (Brevo REST API + Resend API + Gmail SMTP)**:
  - `EmailService` ([`backend/app/services/email_service.py`](file:///e:/Projects/Paradox/backend/app/services/email_service.py)) supports Brevo (ideal for Production/Render without custom domains via `BREVO_API_KEY`), Resend API (`RESEND_API_KEY`), and Gmail SMTP (`SMTP_*` for Localhost).
  - 100% environment-driven with automatic fallback.
- **Google OAuth 2.0 / OpenID Connect**:
  - Dual-mode backend verification (JWKS + TokenInfo fallback) and frontend Google Identity Services integration.
- For local testing:
  - PostgreSQL: `E:\PSQL\bin\postgres.exe -D "E:\PSQL\data"`
  - Backend: `.venv\Scripts\uvicorn app.main:app --port 8000`
  - Frontend: `npm.cmd run dev`
