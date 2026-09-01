# Paradox — Software Requirements Specification (SRS)

**Document Version:** 2.0
**Status:** Final — Ready for Implementation (Authentication & Multi-Tenant User Isolation)
**Product:** Paradox
**Source of Truth (Product):** `PARADOX_PRD_FINAL.md` (v1.0)
**Source of Truth (Technical Decisions):** `PARADOX_SRS_CLAUDE_PROMPT.md` & Production Auth Specification
**Guiding Principle:** Secure, robust, multi-tenant user data isolation with zero trust in frontend identity, maintaining simplicity and performance.

---

## Document Control

| Field | Value |
|---|---|
| Document Type | Software Requirements Specification |
| Product | Paradox — Personal Expense Tracker |
| Phase Covered | Phase 1 + Authentication & Row-Level Data Isolation |
| Authoritative Product Spec | PARADOX_PRD_FINAL.md |
| Authoritative Technical Decisions | PARADOX_SRS_CLAUDE_PROMPT.md & Auth Spec |
| Intended Consumer | Development team / AI coding agent (e.g. Antigravity) |
| Out-of-date Policy | Any change to product scope must first be reflected in the PRD, then propagated here |

## Revision History

| Version | Change Summary |
|---|---|
| 1.0 | Initial complete SRS for Phase 1. |
| 1.1 | Narrowed V1 expense filtering to a single dimension (date OR category, not combined — Section 3.4.1); added explicit default-vs-custom create/edit/delete rule tables for categories (3.2.1) and payment methods (3.3.1); added frontend server-state management approach (11.8, TanStack Query); added CI/CD pipeline (20.6); expanded baseline security threat-category coverage (14.1: XSS, SQL injection, CSRF, secure headers, CORS, secret management); added database connection pooling, concurrency-control scoping, and idempotent-seeding clarifications (6.5); reinforced the three-layer validation principle (9.1); added Design System cross-reference (11.1) without duplicating visual rules; strengthened environment-file/secret-handling language (19.3); confirmed Zod as an explicit frontend dependency for schema validation (2.2, 9.2). |
| 2.0 | Added production-ready Authentication Layer & Row-Level Multi-Tenant Data Isolation. Specified JWT authentication with short-lived access tokens (15 mins) and long-lived rotated refresh tokens (7–30 days) stored in HttpOnly, Secure, SameSite cookies with server-side SHA-256 token hashing; Google Sign-In via OAuth 2.0 / OpenID Connect with backend token verification and account linking; user registration, email/password login, forgot/reset password, change password, single session logout, and logout from all devices; added `refresh_tokens` and `password_reset_tokens` tables; updated `users`, `expenses`, `budgets`, `categories`, and `payment_methods` with mandatory `user_id` foreign keys and composite period constraints; established authoritative backend authorization (`get_current_user` SecurityContext) with zero trust in client-supplied user identifiers across all endpoints; added authentication rate limiting and CSRF protection. |

---

# 1. Introduction

## 1.1 Purpose

This SRS defines **how** Paradox will be technically implemented. It translates the product-level requirements defined in the PRD into a concrete, implementation-ready technical specification: architecture, database schema, API contracts, validation rules, business logic, frontend behavior, security, error handling, testing, and deployment.

This document is written so that a developer or an AI coding agent can implement Paradox **without having to guess or invent requirements**. Anything not explicitly defined here or in the PRD is out of scope.

## 1.2 Scope

This SRS covers **Paradox with Secure Production-Ready Authentication & Multi-Tenant User Isolation**. It documents:

- User Registration, Email/Password Login, and Google OAuth 2.0 / OpenID Connect Sign-In
- JWT-based authentication: Short-lived access tokens (15 mins) and HttpOnly rotated refresh tokens (7–30 days)
- Server-side refresh token revocation, secure single-session logout, and logout from all devices
- Forgot password, reset password, and change password flows
- Strict row-level multi-tenant user data isolation across all resources
- Expense creation, viewing, editing, deletion scoped strictly to the authenticated user
- Starter and custom categories with user-scoping for custom entries
- Payment methods with user-scoping for custom entries
- Search, filter, sort, pagination of user expenses
- Spending summaries and category breakdowns scoped to the authenticated user
- Multi-granularity budgets (Monthly, Weekly, Daily) scoped to the authenticated user
- A single aggregated dashboard endpoint/screen scoped to the authenticated user
- Loading, empty, validation, and error states across all auth and app screens
- Responsive web frontend with route protection and automatic token refresh
- Local development and deployment topology

This SRS does **not** cover RBAC/admin roles (each user has equal ownership over only their own data), notifications/reminders, bank integrations, or multi-currency.

## 1.3 Definitions, Acronyms, and Abbreviations

| Term | Meaning |
|---|---|
| MVP | Minimum Viable Product |
| PRD | Product Requirements Document (`PARADOX_PRD_FINAL.md`) |
| SRS | This document |
| FR-xx | Functional Requirement ID from the PRD |
| API | Application Programming Interface |
| ORM | Object-Relational Mapper (SQLAlchemy 2.0) |
| CRUD | Create, Read, Update, Delete |
| DTO | Data Transfer Object (represented via Pydantic schemas) |
| JWT | JSON Web Token (used for stateless API authorization) |
| Access Token | Short-lived signed JWT (10–15 min lifetime) passed in `Authorization: Bearer <token>` |
| Refresh Token | Cryptographically secure random token stored in an `HttpOnly`, `Secure`, `SameSite` cookie, used to obtain new access tokens |
| Token Rotation | Invalidation of the prior refresh token and issuance of a new pair on every refresh request |
| Starter Entity | A default category or payment method seeded at application initialization, shared across all users |
| Custom Entity | A user-created category or payment method owned exclusively by that user |
| User Isolation | Architectural enforcement ensuring User A can only view, mutate, or delete User A's data |

## 1.4 References

- `PARADOX_PRD_FINAL.md` — Product Requirements Document v1.0
- `PARADOX_SRS_CLAUDE_PROMPT.md` — Finalized technical decisions used to author initial SRS
- RFC 7519 (JSON Web Token), RFC 6749 (OAuth 2.0), OpenID Connect Core 1.0
- FastAPI, SQLAlchemy 2.0, Alembic, Pydantic, Next.js, Framer Motion official documentation

## 1.5 Intended Audience

- Development team implementing Paradox
- AI coding agents (e.g., Antigravity) generating the codebase from this document
- QA/testing contributors
- Product owner, for verifying technical fidelity to the PRD

## 1.6 Document Conventions

- Requirement IDs use the prefix `SRS-` followed by a section-scoped number (e.g., `SRS-FN-AUTH-01`, `SRS-API-01`).
- "Must" denotes a mandatory requirement. "Should" denotes a strongly recommended requirement. "May" denotes an optional enhancement.
- All monetary values are represented using fixed-precision decimal types — never floating point — throughout the stack.
- All authorization decisions are made strictly on the backend using the validated SecurityContext (`current_user.id`).

---

# 2. System Overview

## 2.1 Product Summary

Paradox is a personal expense-tracking web application implementing the core loop **Record → Organize → Review → Understand → Adjust** with secure user authentication and strict data isolation. Each user registers, logs in (or uses Google Sign-In), and manages their own independent financial data — expenses, custom categories, custom payment methods, budgets, and dashboards.

## 2.2 High-Level Architecture

Paradox uses a **decoupled frontend/backend architecture** communicating over a versioned REST API with JWT Bearer authorization and HttpOnly cookie refresh:

```
┌─────────────────────┐      HTTPS / REST / JSON + Bearer JWT     ┌──────────────────────┐
│   Next.js Frontend   │ ───────────────────────────────────────▶ │   FastAPI Backend     │
│  (AuthContext, TS)  │ ◀─────────────────────────────────────── │ (Router→Service→Repo) │
└─────────────────────┘      HttpOnly Refresh Token Cookie        └──────────┬───────────┘
                                                                             │ SQLAlchemy 2.0 (async)
                                                                             ▼
                                                                   ┌──────────────────┐
                                                                   │   PostgreSQL      │
                                                                   │ (User-Scoped DB) │
                                                                   └──────────────────┘
```

- **Frontend**: Next.js (App Router) + TypeScript + Framer Motion + Zod + TanStack Query + AuthContext, managing client-side routing, protected routes, form validation, in-memory access token storage, and automatic token refresh interceptors.
- **Backend**: Python + FastAPI, responsible for auth verification, JWT token issuance/rotation, business logic, validation, persistence, and API contracts, structured as **Router → Service → Repository → Database**.
- **Database**: PostgreSQL, accessed exclusively through SQLAlchemy 2.0 (async, via `asyncpg`), with schema evolution managed by Alembic. All application entities are strictly foreign-keyed to `users.id`.
- **Communication**: REST over JSON, versioned at the routing/contract layer as `/api/v1/...`.

## 2.3 System Boundaries

**In scope:**
- User Authentication (Sign Up, Email/Password Login, Google OAuth 2.0 / OpenID Connect, Logout, Multi-Device Logout, Password Recovery/Reset, Change Password)
- JWT Access Token (15 min) + HttpOnly SameSite Refresh Token Rotation (7–30 days) with database token hashing
- Strict row-level multi-tenant user data isolation across all entities
- Web frontend (responsive; PWA-enabled; route guards)
- Single backend service
- Single PostgreSQL database with Alembic migrations
- Single fixed currency per user account

**Out of scope:**
- Role-Based Access Control (RBAC) / Admin systems (all users have equal ownership over only their own data)
- External bank/financial sync integrations
- SMS notifications
## 2.4 Assumptions and Dependencies

- Local development does not require Docker; Docker is used for backend deployment (Section 20).
- Google OAuth credentials (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`) are configured in backend environment variables; Google Client ID is configured in frontend environment variables.
- One fixed currency is assumed application-wide; all calculations use Python `Decimal`.
- Each user account is completely isolated; there are no shared expense records or cross-user permissions.

---

# 3. Functional Requirements

Each functional requirement below implements the product capabilities. IDs are grouped by module.

## 3.0 Authentication & User Isolation

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-AUTH-01 | The system must allow new users to register with `email`, `password`, and optional `display_name`. Passwords must be hashed via BCrypt/Argon2 before persistence. | Auth Spec |
| SRS-FN-AUTH-02 | The system must allow users to log in with verified `email` and `password`, returning a short-lived Access Token (15 mins) and setting a rotated Refresh Token in an `HttpOnly`, `Secure`, `SameSite` cookie (7–30 days). | Auth Spec |
| SRS-FN-AUTH-03 | The system must support Google Sign-In via OAuth 2.0 / OpenID Connect, validating Google ID tokens on the backend, auto-provisioning new users or linking existing accounts, and issuing standard app session tokens. | Auth Spec |
| SRS-FN-AUTH-04 | The system must rotate refresh tokens upon invocation of `/api/v1/auth/refresh`, revoking the prior token hash and issuing a new access token and new refresh cookie. | Auth Spec |
| SRS-FN-AUTH-05 | The system must allow users to log out securely, clearing the client cookie and marking the active refresh token hash as revoked in the database. | Auth Spec |
| SRS-FN-AUTH-06 | The system must support "Logout from all devices", invalidating all active refresh tokens for the authenticated user across all sessions. | Auth Spec |
| SRS-FN-AUTH-07 | The system must support password recovery: requesting a password reset token via `/api/v1/auth/forgot-password` and securely consuming it via `/api/v1/auth/reset-password`. | Auth Spec |
| SRS-FN-AUTH-08 | The system must allow an authenticated user to change their password via `/api/v1/auth/change-password` by verifying their current password. | Auth Spec |
| SRS-FN-AUTH-09 | The system must enforce strict row-level user data isolation on every endpoint: User A can only read, create, update, or delete User A's expenses, budgets, custom categories, and custom payment methods. | Auth Spec |
| SRS-FN-AUTH-10 | Attempting to access, modify, or delete a resource belonging to another user must return `404 Not Found` (or `403 Forbidden`) and must never leak entity existence or content. | Auth Spec |

## 3.1 Expense Management

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-01 | The system must allow creation of an expense with `amount`, `category_id`, `payment_method_id`, `date`, and optional `description`. | FR-01 |
| SRS-FN-02 | The system must reject expense creation/update where `amount <= 0`. | FR-02 |
| SRS-FN-03 | The system must reject expense creation/update where `date` is later than the current server date. | FR-02 |
| SRS-FN-04 | The system must return recorded expenses via a paginated list endpoint, ordered by date (default: descending). | FR-03 |
| SRS-FN-05 | The system must allow updating any mutable field of an existing expense (`amount`, `category_id`, `payment_method_id`, `date`, `description`). | FR-04 |
| SRS-FN-06 | The system must allow deletion of an expense by ID, requiring explicit client confirmation at the UI layer before the delete request is sent. | FR-05 |
| SRS-FN-07 | Every expense must reference exactly one valid, existing category at time of creation/update. | FR-06 |
| SRS-FN-08 | A successfully created, updated, or deleted expense must be immediately reflected in subsequent `GET` requests (list, detail, dashboard, budget). | FR-22, FR-23 |

## 3.2 Category Management

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-09 | The system must seed a fixed set of starter categories on first application initialization (see Appendix, Section 27.2). | FR-06 |
| SRS-FN-10 | The system must allow creation of custom categories with a unique, non-empty `name`. | FR-07 |
| SRS-FN-11 | The system must allow renaming any category (starter or custom). | FR-07 |
| SRS-FN-12 | Starter categories must be flagged (`is_default = true`) and must not be deletable. Renaming is permitted. | FR-08 |
| SRS-FN-13 | Custom categories may be deleted only if no expenses reference them, **or** the system must reassign affected expenses to a designated fallback category (`Uncategorized`, a protected starter category) at deletion time — see Section 10.3 for the finalized rule. | FR-08 |

### 3.2.1 Default (Starter) vs. Custom Category Rules

| Action | Default/Starter Category | Custom Category |
|---|---|---|
| **Create** | Not user-creatable — only exists via the seed migration (Section 27.2). | User-creatable via `POST /api/v1/categories`; `name` must be unique (case-insensitive) and non-empty (≤ 60 chars). |
| **Edit (rename)** | Allowed via `PATCH /api/v1/categories/{id}`. `is_default` itself is never user-editable. | Allowed via `PATCH /api/v1/categories/{id}`, same name-uniqueness rule. |
| **Delete** | **Never permitted.** Any `DELETE` request against a row with `is_default = true` returns `409 Conflict` (`CONFLICT`) and is rejected before any expense data is touched. | Permitted via `DELETE /api/v1/categories/{id}`. If expenses reference it, they are reassigned to the reserved `Uncategorized` category first, inside the same transaction as the delete (Section 10.3/6.4). If unreferenced, the row is simply removed. |
| **Identification** | `is_default = true` in the database and in all API responses, so the frontend can disable/hide the delete action for these rows. | `is_default = false`. |

This table is the authoritative rule set for FR-06, FR-07, and FR-08; Sections 6.2.2, 6.4, and 10.3 describe the same rule at the schema and transaction-implementation level and must remain consistent with it.

## 3.3 Payment Method Management

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-14 | The system must seed a fixed set of starter payment methods on first application initialization (see Section 27.3). | FR-09 |
| SRS-FN-15 | The system must allow creation, renaming, and deletion of payment methods, subject to the same protection rule as categories for starter entries and the same reassignment rule for referenced entries. | FR-09 |

### 3.3.1 Default (Starter) vs. Custom Payment Method Rules

Payment methods follow **exactly the same rule set as categories** (Section 3.2.1), substituting the reserved fallback `Other` for `Uncategorized`:

| Action | Default/Starter Payment Method | Custom Payment Method |
|---|---|---|
| **Create** | Not user-creatable — only exists via the seed migration (Section 27.3). | User-creatable via `POST /api/v1/payment-methods`; `name` must be unique (case-insensitive) and non-empty (≤ 60 chars). |
| **Edit (rename)** | Allowed via `PATCH /api/v1/payment-methods/{id}`. `is_default` is never user-editable. | Allowed via `PATCH /api/v1/payment-methods/{id}`, same name-uniqueness rule. |
| **Delete** | **Never permitted.** `DELETE` against `is_default = true` returns `409 Conflict`. | Permitted via `DELETE /api/v1/payment-methods/{id}`. Referencing expenses are reassigned to the reserved `Other` payment method first, inside the same transaction as the delete (Section 10.3/6.4). If unreferenced, the row is simply removed. |
| **Identification** | `is_default = true`. | `is_default = false`. |

## 3.4 Review, Search, Filter, Sort

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-16 | The expense list endpoint must support filtering by date range (`date_from`, `date_to`). | FR-10 |
| SRS-FN-17 | The expense list endpoint must support free-text search across `description`, combinable with sorting, pagination, and at most one active filter dimension (Section 3.4.1). | FR-11 |
| SRS-FN-18 | The expense list endpoint must support filtering by **either** a date range **or** a single category — not both at once, and not by payment method or amount in V1 (see Section 3.4.1 for the scoping rationale). | FR-12 (narrowed for V1) |
| SRS-FN-19 | The expense list endpoint must support sorting by `date`, `amount`, and `category` (ascending/descending), independent of and combinable with any active single-dimension filter and with search. | FR-13 |

### 3.4.1 V1 Filter Scoping Rule (single-dimension filtering)

Per finalized product decision, **V1 supports filtering the expense list by date range OR category — never both at the same time.** Payment-method and amount-range filtering are **not** V1 requirements (deferred; see Section 26).

- **Search** (`search`, free-text on `description`), **sorting** (`sort_by`, `sort_order`), and **pagination** (`page`, `page_size`) are unrestricted and always combinable with each other and with a single active filter.
- A request supplying **both** a date-range parameter (`date_from` and/or `date_to`) **and** `category_id` in the same call is rejected with `422 INVALID_REQUEST` ("only one filter — date range or category — may be applied at a time in V1").
- This is a deliberate MVP scoping decision, not an oversight: it keeps the filter UI and query logic minimal while the core loop is validated. Combined multi-dimension filtering (date + category + payment method + amount together) is a candidate for a later phase (Section 26) once real usage shows it's needed.
- This narrows PRD FR-12 ("filtering by date, category, amount, and payment method") for V1 implementation purposes; the PRD's broader intent remains a future-phase candidate rather than a contradiction — the PRD describes the general product need, and this SRS defines the V1-scoped technical implementation of it, consistent with PRD Principle 6 ("Complexity must be earned").

## 3.5 Summaries and Insight

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-20 | The system must compute and expose total spending for a given period with monetary accuracy (fixed-precision decimal arithmetic only). | FR-14 |
| SRS-FN-21 | The system must compute and expose spending grouped by category for a given period. | FR-15 |
| SRS-FN-22 | The system must expose spending trend data across meaningful periods (e.g., current month vs. prior month, weekly buckets within a month). | FR-16 |
| SRS-FN-23 | The system must identify and expose the top spending categories for a given period. | FR-17 |

## 3.6 Budget

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-24 | The system must allow defining exactly one overall monthly budget amount (a singular resource, not a collection). | FR-18 |
| SRS-FN-25 | The system must compute actual spending for the current calendar month and compare it against the configured budget. | FR-19 |
| SRS-FN-26 | The system must expose `budget_amount`, `amount_spent`, `remaining_amount`, and a simple status (`under_budget`, `near_limit`, `over_budget`) from the budget/dashboard endpoints. | FR-20 |

## 3.7 Dashboard

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-27 | The system must expose a single dashboard endpoint aggregating: current-period total spending, budget status, category breakdown, top categories, and recent expenses. | FR-21 |

## 3.8 System-Level Behaviors

| ID | Requirement | Traces to |
|---|---|---|
| SRS-FN-28 | All financial data returned by the API must reflect actual persisted records; no fabricated or placeholder financial data may be returned in normal operation. | FR-23 |
| SRS-FN-29 | The frontend must present distinguishable, understandable states for loading, empty, validation error, and system error conditions on every primary screen. | FR-24 |
| SRS-FN-30 | The system must expose a health-check endpoint reporting service and database connectivity status. | Operational requirement |

---

# 4. Use Cases

## 4.0A UC-00A: User Registration
- **Actor**: Anonymous visitor
- **Main Flow**:
  1. User navigates to `/register`.
  2. User provides email, password (min 8 chars, mixed complexity), and display name.
  3. Frontend validates format via Zod and calls `POST /api/v1/auth/register`.
  4. Backend hashes password using BCrypt/Argon2, creates the user record, and issues an Access Token + HttpOnly Refresh Token cookie.
  5. User is redirected to `/dashboard` with active session.

## 4.0B UC-00B: User Login (Email / Password)
- **Actor**: Anonymous visitor
- **Main Flow**:
  1. User navigates to `/login` and enters email and password.
  2. Frontend calls `POST /api/v1/auth/login`.
  3. Backend verifies password hash, issues a 15-minute Access Token, and sets a rotated Refresh Token cookie.
  4. Frontend stores Access Token in-memory and redirects to `/dashboard`.

## 4.0C UC-00C: Google Sign-In (OAuth 2.0 / OpenID Connect)
- **Actor**: Anonymous visitor
- **Main Flow**:
  1. User clicks "Sign in with Google" on the login/register screen.
  2. Google authentication modal returns a cryptographically signed Google ID Token (credential).
  3. Frontend sends ID Token to backend `POST /api/v1/auth/google`.
  4. Backend validates token signature with Google's public certs, retrieves verified email/Google ID, provisions a new account or links an existing account, and issues standard app session tokens.

## 4.0D UC-00D: Automatic Token Refresh & Rotation
- **Actor**: Authenticated user (background client interceptor)
- **Main Flow**:
  1. Frontend API client detects an expired Access Token (`401 Unauthorized` or preemptive expiry timer).
  2. Client calls `POST /api/v1/auth/refresh` (cookie sent automatically).
  3. Backend verifies token hash against database, marks previous token as used/revoked, stores new rotated token hash, and returns a fresh Access Token + updated cookie.
  4. Client retries the failed API request seamlessly.

## 4.0E UC-00E: Forgot & Reset Password
- **Actor**: Unauthenticated user
- **Main Flow**:
  1. User requests password reset at `/forgot-password` providing email.
  2. Backend generates a secure cryptographic reset token and stores hash with expiration.
  3. User navigates to `/reset-password?token=...`, enters a new password, and submits `POST /api/v1/auth/reset-password`.
  4. Backend verifies token, updates user's password hash, revokes all active refresh sessions, and confirms reset.

## 4.0F UC-00F: Secure Logout & Multi-Device Revocation
- **Actor**: Authenticated user
- **Main Flow**:
  1. User clicks "Logout" (single device) or "Logout from all devices".
  2. Frontend calls `POST /api/v1/auth/logout` or `POST /api/v1/auth/logout-all` with Bearer token.
  3. Backend marks corresponding refresh token hash(es) as revoked in the database and clears the refresh cookie.
  4. Frontend purges in-memory Access Token and redirects to `/login`.

## 4.1 UC-01: Add Expense
- **Actor**: Authenticated user (User A)
- **Preconditions**: User is logged in; at least one category and payment method exist.
- **Main Flow**:
  1. User opens the "Add Expense" form.
  2. User enters amount, selects category, selects payment method, confirms date, and optional description.
  3. Frontend sends `POST /api/v1/expenses` with `Authorization: Bearer <token>`.
  4. Backend derives `current_user.id` from JWT, validates data, and persists expense with `user_id = current_user.id`.
  5. UI updates list, dashboard, and budget views.
- **Postcondition**: Expense belongs strictly to User A.

## 4.2 UC-02: View / Search / Filter / Sort Expenses
- **Actor**: Authenticated user (User A)
- **Main Flow**:
  1. User navigates to `/expenses`.
  2. Frontend calls `GET /api/v1/expenses` with active filters.
  3. Backend queries `expenses` filtered strictly by `user_id == current_user.id`. User B's expenses are never included.
  4. Frontend renders User A's expense list.

## 4.3 UC-03: Edit Expense
- **Actor**: Authenticated user (User A)
- **Main Flow**:
  1. User edits an expense and submits `PATCH /api/v1/expenses/{id}`.
  2. Backend verifies `expense.user_id == current_user.id`. If matching, updates record.
  3. If another user (User B) attempts to edit User A's expense ID, backend returns `404 Not Found`.

## 4.4 UC-04: Delete Expense
- **Actor**: Authenticated user (User A)
- **Main Flow**:
  1. User initiates delete and confirms dialog.
  2. Frontend calls `DELETE /api/v1/expenses/{id}`.
  3. Backend verifies `expense.user_id == current_user.id` and removes record. If ID belongs to another user, returns `404 Not Found`.

## 4.5 UC-05: Manage Categories
- **Actor**: Authenticated user (User A)
- **Main Flow**: User creates/renames custom categories (`user_id = current_user.id`). User can see system starter categories (`user_id = NULL, is_default = true`) and their own custom categories. User cannot view or mutate User B's custom categories.

## 4.6 UC-06: Manage Payment Methods
- **Actor**: Authenticated user (User A)
- **Main Flow**: Same isolation and default protection rules as categories.

## 4.7 UC-07: Set / Update Budget
- **Actor**: Authenticated user (User A)
- **Main Flow**:
  1. User selects period granularity (Month/Week/Day) and target key.
  2. User enters budget amount and submits `PUT /api/v1/budget`.
  3. Backend upserts budget record strictly scoped to `(user_id, period_type, period_key)`.

## 4.8 UC-08: View Dashboard
- **Main Flow**:
  1. User opens the dashboard (default landing screen).
  2. Frontend calls `GET /api/v1/dashboard`.
  3. Backend returns aggregated totals, budget status, category breakdown, top categories, and recent expenses in one response.
  4. Frontend renders summary cards, a category breakdown visualization, and a recent-expenses list.

---

# 5. System Architecture

## 5.1 Architectural Style

Paradox uses a **layered, service-oriented monolith** for the backend (appropriate to the single-user validation goal — PRD Principle: "Complexity must be earned") and a **feature-organized Next.js frontend**. No microservices, message queues, or distributed system components are introduced in Phase 1.

## 5.2 Backend Layering

```
Client Request
   │
   ▼
API Router (app/api/*.py)          — HTTP concerns, route definitions, dependency injection
   │
   ▼
Pydantic Schemas (app/schemas/*)   — request/response validation & serialization
   │
   ▼
Service Layer (app/services/*)     — business logic, orchestration, business-rule enforcement
   │
   ▼
Repository Layer (app/repositories/*) — database queries via SQLAlchemy, no business logic
   │
   ▼
SQLAlchemy Models (app/db/models/*) — ORM entities mapped to PostgreSQL tables
   │
   ▼
PostgreSQL
```

**Layering rules:**
- Routers must not contain business logic or direct database queries.
- Services must not import SQLAlchemy session objects directly into routers; all DB access is mediated by repositories.
- Repositories must not contain business rules (e.g., "amount must be positive" is a service-layer rule, not a repository-layer rule).
- Cross-cutting concerns (config, exceptions, error handling, logging) live in `app/core/`.
- Any deviation from this layering must be documented inline with a stated architectural reason (per source prompt Rule: "Do not bypass layers without a documented architectural reason").

## 5.3 Frontend Architecture

- **App Router** pages (`src/app/*`) are thin: they compose feature components and handle routing/layout only.
- **Feature modules** (`src/features/*`) own their components, hooks, API-calling services, and Zod schemas (`schemas/`) per domain (expenses, categories, payment-methods, budget, dashboard). Zod schemas are the single source of truth for client-side validation shape and TypeScript types (via `z.infer`), keeping frontend validation (Section 9.2, 11.4) consistent across forms.
- **Shared UI primitives** live in `src/components/ui`; shared layout in `src/components/layout`; shared non-visual utilities in `src/components/common`.
- **API client** (`src/lib/api/*`) centralizes HTTP calls to the backend, one module per resource, all built on a shared `client.ts` (base URL, headers, error normalization).
- State is managed with React state/hooks and server data-fetching per feature; no global state library is introduced unless a documented need arises (kept minimal per "complexity must be earned").

## 5.4 Data Flow Example (Add Expense)

1. `ExpenseForm` component (feature/expenses) collects input and performs client-side validation.
2. On submit, `features/expenses/services` calls `lib/api/expenses.ts` → `POST /api/v1/expenses`.
3. FastAPI router `app/api/expenses.py` receives the request, validates against `ExpenseCreate` Pydantic schema.
4. `expense_service.create_expense()` enforces business rules (positive amount, non-future date, valid category/payment method reference).
5. `expense_repository.create()` persists via SQLAlchemy async session.
6. Response is serialized via `ExpenseRead` schema and returned with `201 Created`.
7. Frontend invalidates/refetches affected views (expense list, dashboard, budget).

---

# 6. Database Design

## 6.1 Design Principles

- PostgreSQL is the sole datastore.
- All schema changes are made exclusively through Alembic migrations; no uncontrolled manual schema changes.
- Application-level data access uses SQLAlchemy 2.0 ORM; critical integrity rules are additionally enforced at the database level (constraints, foreign keys, `NOT NULL`, `CHECK`).
- All monetary columns use `NUMERIC(12, 2)` (fixed-precision decimal) — never `FLOAT`/`DOUBLE`.
- All timestamps are stored in UTC; dates for expenses are stored as `DATE` (no time-of-day semantics required for V1).
- Primary keys use UUID (`gen_random_uuid()` / `uuid4()`) for forward compatibility with a future multi-user system, without introducing multi-user logic in V1.

## 6.2 Entities

### 6.2.1 `users`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, default `gen_random_uuid()` |
| email | VARCHAR(255) | NOT NULL, UNIQUE, index |
| password_hash | VARCHAR(255) | NULL (nullable for OAuth-only users) |
| google_id | VARCHAR(255) | NULL, UNIQUE, index |
| display_name | VARCHAR(100) | NOT NULL, default `'User'` |
| avatar_url | VARCHAR(512) | NULL |
| is_verified | BOOLEAN | NOT NULL, default `true` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

### 6.2.2 `refresh_tokens`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, default `gen_random_uuid()` |
| user_id | UUID | NOT NULL, FK → `users.id` (`ON DELETE CASCADE`) |
| token_hash | VARCHAR(255) | NOT NULL, index |
| expires_at | TIMESTAMPTZ | NOT NULL |
| is_revoked | BOOLEAN | NOT NULL, default `false` |
| user_agent | VARCHAR(255) | NULL |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

**Rules**: Stores only the cryptographic hash (SHA-256) of active/revoked refresh tokens. When rotated, the old token hash is marked `is_revoked = true` or replaced. During logout/revocation, tokens for the target session or all user sessions are marked revoked.

### 6.2.3 `password_reset_tokens`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, default `gen_random_uuid()` |
| user_id | UUID | NOT NULL, FK → `users.id` (`ON DELETE CASCADE`) |
| token_hash | VARCHAR(255) | NOT NULL, index |
| expires_at | TIMESTAMPTZ | NOT NULL |
| is_used | BOOLEAN | NOT NULL, default `false` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |

### 6.2.4 `categories`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | NULL, FK → `users.id` (`ON DELETE CASCADE`) |
| name | VARCHAR(60) | NOT NULL |
| is_default | BOOLEAN | NOT NULL, default `false` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

**Rules**: Starter categories have `is_default = true` and `user_id = NULL`. Custom categories have `is_default = false` and `user_id = current_user.id`. Starter categories cannot be deleted.

### 6.2.5 `payment_methods`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | NULL, FK → `users.id` (`ON DELETE CASCADE`) |
| name | VARCHAR(60) | NOT NULL |
| is_default | BOOLEAN | NOT NULL, default `false` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

Same protection and ownership rules as `categories`.

### 6.2.6 `expenses`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | NOT NULL, FK → `users.id` (`ON DELETE CASCADE`) |
| amount | NUMERIC(12,2) | NOT NULL, `CHECK (amount > 0)` |
| category_id | UUID | NOT NULL, FK → `categories.id` (`ON DELETE RESTRICT`) |
| payment_method_id | UUID | NOT NULL, FK → `payment_methods.id` (`ON DELETE RESTRICT`) |
| date | DATE | NOT NULL, application-level `CHECK`: not later than current date |
| description | VARCHAR(255) | NULL |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

**Indexes**: `idx_expenses_user_id`, `idx_expenses_date`, `idx_expenses_category_id`, `idx_expenses_payment_method_id`, composite `idx_expenses_user_date` (`user_id`, `date`), composite `idx_expenses_user_category` (`user_id`, `category_id`).

### 6.2.7 `budgets`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | NOT NULL, FK → `users.id` (`ON DELETE CASCADE`) |
| period_type | VARCHAR(10) | NOT NULL, default `'month'` |
| period_key | VARCHAR(20) | NOT NULL, index |
| month | VARCHAR(7) | NULL |
| amount | NUMERIC(12,2) | NOT NULL, `CHECK (amount >= 0)` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

**Constraints**: `CHECK (amount >= 0)`, `UniqueConstraint("user_id", "period_type", "period_key", name="uq_user_budget_period")`.

## 6.3 Entity Relationship Summary

```
users (1) ──< (many) expenses
users (1) ──< (many) budgets
users (1) ──< (many) custom categories
users (1) ──< (many) custom payment_methods
users (1) ──< (many) refresh_tokens
users (1) ──< (many) password_reset_tokens
categories (1) ──< (many) expenses
payment_methods (1) ──< (many) expenses
```

## 6.4 Referential Integrity Rule (Category/Payment Method Deletion)

Per Section 10.3 (business rules), deletion of a **custom** category or payment method that is still referenced by one or more expenses is handled as follows:

1. Service layer reassigns all referencing expenses to the reserved fallback (`Uncategorized` category / `Other` payment method — both `is_default = true`, non-deletable).
2. The now-unreferenced category/payment method row is deleted.
3. This operation is performed inside a single database transaction to avoid inconsistent intermediate states.

This satisfies FR-08 ("Category removal must not make existing expense records unclear or inconsistent") without requiring the user to manually recategorize expenses first.

## 6.5 Connection Pooling, Concurrency, and Idempotent Seeding

### 6.5.1 Connection Pooling / Configuration

- The backend uses SQLAlchemy 2.0's async engine (`create_async_engine`, `asyncpg` driver) with a standard connection pool — a modest fixed pool size (e.g., `pool_size` in the small single digits, with a small `max_overflow`) is sufficient for a single-user application and avoids exhausting Supabase's connection limits on a small-tier plan.
- Pool sizing, `pool_timeout`, and `pool_recycle` are configured via `app/core/config.py`/environment variables (Section 19.1), not hardcoded, so they can be tuned per environment (local vs. Supabase-hosted) without a code change.
- One shared engine/session-factory instance is used per running backend process (`app/db/session.py`); a new engine is not created per request.

### 6.5.2 Concurrency Control

- **V1 does not require advanced distributed concurrency control** (no optimistic-locking version columns, no distributed locks, no multi-writer conflict resolution). This is appropriate because Paradox V1 has exactly one user and, practically, one client session interacting with the database at a time.
- Standard database-transaction guarantees (Section 12.3: single-transaction mutations, e.g. the category/payment-method reassign-then-delete flow in Section 10.3) are sufficient to prevent partial writes; they are not a substitute for multi-user concurrency control and must not be assumed sufficient if Phase 3 introduces concurrent multi-user writes (Section 26).

### 6.5.3 Idempotent Seed Data (Starter Categories / Payment Methods)

- The starter category and payment method seed (Sections 27.2, 27.3) is implemented as an Alembic **data migration**, which by Alembic's own migration-history mechanism runs **exactly once** per database (tracked in the `alembic_version` table) — re-running `alembic upgrade head` on an already-migrated database does not re-execute already-applied migrations, so the seed cannot be duplicated by normal migration workflow.
- As a defense-in-depth measure against any non-standard initialization path (e.g., a manually re-run seed script), the seed migration/script must be **idempotent**: it inserts starter rows using an "insert if not exists" pattern keyed on `name` (or is wrapped in a check that skips insertion if starter rows already exist), so that accidentally executing it twice never produces duplicate `is_default = true` rows. The `UNIQUE` constraint on `categories.name` / `payment_methods.name` (Section 6.2.2, 6.2.3) provides a final database-level backstop against duplication regardless of application-level logic.

---

# 7. API Specification

## 7.1 Conventions & Authorization

- Base path: `/api/v1`
- All request/response bodies are JSON.
- All monetary fields are serialized as strings with two decimal places (e.g., `"amount": "42.50"`).
- All dates are ISO-8601 (`YYYY-MM-DD`); all timestamps are ISO-8601 UTC with `Z` suffix.
- Resource identifiers are UUID strings.
- **Protected Endpoints**: Require header `Authorization: Bearer <access_token>`. Missing, invalid, or expired tokens return `401 Unauthorized`.
- **User Scoping**: Every protected endpoint strictly accesses and mutates only resources belonging to `current_user.id` derived from the validated JWT. Attempting to query, mutate, or delete another user's entity returns `404 Not Found`.

## 7.2 Authentication Endpoints (`/api/v1/auth`)

### `POST /api/v1/auth/register`
- **Purpose**: Register a new user account.
- **Request body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "display_name": "Alex"
}
```
- **Validation**: `email` valid format; `password` min 8 chars with complexity; `display_name` optional (1–100 chars).
- **Success**: `201 Created`, returns `UserRead` + `access_token` and sets `HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth` cookie `paradox_refresh_token`.
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "display_name": "Alex",
    "avatar_url": null,
    "created_at": "2026-08-31T12:00:00Z"
  },
  "access_token": "eyJhbGciOi...",
  "token_type": "bearer",
  "expires_in": 900
}
```
- **Errors**: `409 Conflict` if email already registered; `422` validation error.

### `POST /api/v1/auth/login`
- **Purpose**: Authenticate existing user with email and password.
- **Request body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```
- **Success**: `200 OK`, returns `TokenResponse` (same shape as register) and sets rotated `paradox_refresh_token` cookie.
- **Errors**: `401 Unauthorized` ("Invalid email or password"); `429 Too Many Requests` (rate limited).

### `POST /api/v1/auth/google`
- **Purpose**: Authenticate via Google OAuth 2.0 / OpenID Connect.
- **Request body**:
```json
{
  "id_token": "eyJhbGciOiJSUzI1NiIs..."
}
```
- **Validation**: Backend cryptographically verifies `id_token` against Google public keys and validates audience against `GOOGLE_CLIENT_ID`.
- **Behavior**: Retrieves verified `email`, `sub` (Google ID), `name`, `picture`. If user exists, links Google ID if not set; if new, provisions account. Issues application access token + refresh cookie.
- **Success**: `200 OK`, returns `TokenResponse` + sets refresh cookie.
- **Errors**: `401 Unauthorized` if Google ID token is invalid or expired.

### `POST /api/v1/auth/refresh`
- **Purpose**: Rotate refresh token and issue a new access token.
- **Request**: Cookie `paradox_refresh_token`.
- **Behavior**: Hashes cookie value (SHA-256), finds active token in `refresh_tokens` table, marks old token revoked/used, creates new token hash, sets new refresh cookie, and returns fresh access token.
- **Success**: `200 OK`, returns `{ "access_token": "...", "token_type": "bearer", "expires_in": 900 }`.
- **Errors**: `401 Unauthorized` if refresh token is missing, expired, or revoked.

### `POST /api/v1/auth/logout`
- **Purpose**: Securely log out current device session.
- **Headers**: Optional `Authorization: Bearer <token>`, Cookie `paradox_refresh_token`.
- **Behavior**: Marks active refresh token hash as `is_revoked = true` in DB and clears `paradox_refresh_token` cookie with max-age=0.
- **Success**: `200 OK` or `204 No Content`.

### `POST /api/v1/auth/logout-all`
- **Purpose**: Revoke all active sessions for the authenticated user across all devices.
- **Headers**: `Authorization: Bearer <token>`.
- **Behavior**: Marks all refresh tokens for `current_user.id` as `is_revoked = true` and clears client cookie.
- **Success**: `200 OK`, `{ "message": "Successfully logged out from all devices" }`.
- **Errors**: `401 Unauthorized`.

### `GET /api/v1/auth/me`
- **Purpose**: Retrieve the profile of the currently authenticated user.
- **Headers**: `Authorization: Bearer <token>`.
- **Success**: `200 OK`, returns `UserRead`.
- **Errors**: `401 Unauthorized`.

### `POST /api/v1/auth/forgot-password`
- **Purpose**: Request a password reset token.
- **Request body**: `{ "email": "user@example.com" }`
- **Behavior**: Generates a secure random reset token, stores SHA-256 hash in `password_reset_tokens` with 1-hour expiry. (In development/testing, token may be logged or returned in safe non-prod environments; in production, emailed). Returns generic success to prevent email enumeration.
- **Success**: `200 OK`, `{ "message": "If this email is registered, a password reset link has been sent." }`.
- **Errors**: `429 Too Many Requests` (rate limited).

### `POST /api/v1/auth/reset-password`
- **Purpose**: Set a new password using a valid reset token.
- **Request body**:
```json
{
  "token": "reset_token_string",
  "new_password": "NewSecurePassword123!"
}
```
- **Validation**: Token exists, `is_used = false`, not expired; `new_password` meets strength rules.
- **Behavior**: Hashes new password, updates user, marks token used, and revokes all active refresh tokens for the user.
- **Success**: `200 OK`, `{ "message": "Password has been successfully reset." }`.
- **Errors**: `400 Bad Request` or `401 Unauthorized` if token is invalid or expired.

### `POST /api/v1/auth/change-password`
- **Purpose**: Change password for currently authenticated user.
- **Headers**: `Authorization: Bearer <token>`.
- **Request body**:
```json
{
  "current_password": "OldPassword123!",
  "new_password": "NewPassword123!"
}
```
- **Success**: `200 OK`, `{ "message": "Password changed successfully." }`.
- **Errors**: `400 Bad Request` if current password incorrect; `401 Unauthorized`.

---

## 7.3 Expenses

All expense endpoints require `Authorization: Bearer <token>` and operate strictly on rows where `expenses.user_id == current_user.id`.

### `POST /api/v1/expenses`
- **Purpose**: Create a new expense for the authenticated user.
- **Request body**:
```json
{
  "amount": "24.50",
  "category_id": "uuid",
  "payment_method_id": "uuid",
  "date": "2026-08-20",
  "description": "Lunch"
}
```
- **Validation**: `amount > 0`; `date <= today`; `category_id` and `payment_method_id` must reference valid categories/methods (system default or user-owned); `description` optional (≤ 255 chars).
- **Success**: `201 Created`, returns `ExpenseRead` (with `user_id = current_user.id`).
- **Errors**: `401 Unauthorized`; `422 Validation Error`; `404` if referenced category/method not found.

### `GET /api/v1/expenses`
- **Purpose**: List authenticated user's expenses with search, filter, sort, pagination.
- **Query parameters**: `search`, `category_id`, `date_from`, `date_to`, `sort_by` (`date` | `amount` | `category`), `sort_order` (`asc` | `desc`), `page`, `page_size`.
- **Success**: `200 OK`, returns paginated envelope scoped strictly to `current_user.id`.

### `GET /api/v1/expenses/{expense_id}`
- **Purpose**: Retrieve a single expense by ID.
- **Success**: `200 OK`.
- **Errors**: `404 Not Found` if expense does not exist or belongs to another user (never leaks data).

### `PATCH /api/v1/expenses/{expense_id}`
- **Purpose**: Partially update an expense owned by the authenticated user.
- **Success**: `200 OK`, returns updated expense.
- **Errors**: `404 Not Found` if expense belongs to another user; `422 Validation Error`.

### `DELETE /api/v1/expenses/{expense_id}`
- **Purpose**: Delete an expense owned by the authenticated user.
- **Success**: `204 No Content`.
- **Errors**: `404 Not Found` if expense belongs to another user.

---

## 7.4 Categories

### `POST /api/v1/categories`
- **Purpose**: Create a custom category owned by the authenticated user (`user_id = current_user.id`, `is_default = false`).
- **Request body**: `{ "name": "Travel" }`
- **Success**: `201 Created`.
- **Errors**: `409 Conflict` if category with same name exists for this user.

### `GET /api/v1/categories`
- **Purpose**: List system starter categories (`is_default = true`) plus custom categories owned by `current_user.id`.
- **Success**: `200 OK`, array of categories.

### `GET /api/v1/categories/{category_id}`
- **Purpose**: Retrieve a category if default or owned by user.
- **Success**: `200 OK`.
- **Errors**: `404 Not Found`.

### `PATCH /api/v1/categories/{category_id}`
- **Purpose**: Rename a category (if starter, or custom owned by user).
- **Success**: `200 OK`.
- **Errors**: `404 Not Found` if custom category belongs to another user.

### `DELETE /api/v1/categories/{category_id}`
- **Purpose**: Delete a custom category owned by the authenticated user. If expenses reference it, they are reassigned to `Uncategorized` inside the transaction.
- **Errors**: `409 Conflict` if `is_default = true`; `404 Not Found` if owned by another user.

---

## 7.5 Payment Methods

Follows the identical pattern as Categories:
- `POST /api/v1/payment-methods`: Create custom payment method for `current_user.id`.
- `GET /api/v1/payment-methods`: List system defaults + user custom payment methods.
- `PATCH /api/v1/payment-methods/{id}`: Rename.
- `DELETE /api/v1/payment-methods/{id}`: Delete custom method; reassigns referencing expenses to `Other`.

---

## 7.6 Budget (Multi-Granularity)

### `GET /api/v1/budget`
- **Purpose**: Retrieve budget for a specific period granularity (`month` | `week` | `day`) and `period_key` for `current_user.id`.
- **Query params**: `period_type` (default `month`), `period_key` (e.g. `2026-08`, `2026-W35`, `2026-08-31`).
- **Success**: `200 OK`, returns budget resource or `amount: null` if unconfigured.

### `GET /api/v1/budget/all`
- **Purpose**: List all configured budgets for the authenticated user (optional `period_type` filter).
- **Success**: `200 OK`, array of budgets.

### `PUT /api/v1/budget`
- **Purpose**: Upsert budget target for `current_user.id`.
- **Request body**:
```json
{
  "period_type": "month",
  "period_key": "2026-08",
  "amount": "2500.00"
}
```
- **Success**: `200 OK`, returns updated budget.

### `DELETE /api/v1/budget`
- **Purpose**: Delete a configured budget for a given `period_type` and `period_key`.
- **Success**: `204 No Content`.

---

## 7.7 Dashboard

### `GET /api/v1/dashboard`
- **Purpose**: Single aggregated read model for dashboard screen strictly filtered by `user_id == current_user.id`.
- **Query parameters**: `period` (`current_month` default, `last_30_days`, `current_week`).
- **Success**: `200 OK`. Empty-safe when user has no expenses (totals `"0.00"`, empty lists).

---

## 7.8 System

### `GET /api/v1/health`
- **Purpose**: Liveness/readiness check (public endpoint, no auth required).
- **Response**: `{ "status": "ok", "database": "connected" }` with `200 OK`.

---

# 8. API Standards

## 8.1 Versioning

API versioning is applied exclusively at the routing/contract layer (`/api/v1/...` prefix registered in `app/api/router.py`). The physical source tree under `app/api/*.py` remains version-independent (e.g., `expenses.py`, not `v1/expenses.py`), per the finalized decision.

## 8.2 Standard Response Envelope (single resource)

```json
{
  "data": { "...resource fields..." }
}
```

Single-resource `GET`/`POST`/`PATCH` responses return the resource directly under `data` for consistency; `DELETE` returns no body (`204`).

## 8.3 Standard Paginated Envelope (list endpoints)

```json
{
  "data": [ "...items..." ],
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_items": 57,
    "total_pages": 3
  }
}
```

## 8.4 Standard Error Envelope

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "amount must be greater than 0",
    "details": [
      { "field": "amount", "message": "must be greater than 0" }
    ]
  }
}
```

See Section 15 for the full error taxonomy.

## 8.5 HTTP Status Code Usage

| Code | Usage |
|---|---|
| 200 | Successful GET/PATCH/PUT |
| 201 | Successful POST (resource created) |
| 204 | Successful DELETE (no content) |
| 400 | Malformed request (e.g. invalid current password) |
| 401 | Unauthorized (missing, invalid, expired, or revoked token / bad credentials) |
| 403 | Forbidden (valid token, but access to requested action is denied) |
| 404 | Resource not found (or entity belongs to another user) |
| 409 | Conflict (e.g. duplicate email, duplicate category name, deleting protected starter entity) |
| 422 | Validation error (schema or business rule failure) |
| 429 | Too Many Requests (rate limited login or password reset endpoint) |
| 500 | Unhandled server error |
| 503 | Health check failure |

---

# 9. Validation Requirements

## 9.1 Principle

Three layers, each with a distinct responsibility, none a substitute for the others:

1. **Frontend validation** exists solely to improve user experience — immediate, understandable feedback before a request is even sent (Section 9.2). It is never relied upon as a security or integrity safeguard.
2. **Backend validation is authoritative.** Every request is validated server-side (schema + business rules, Section 9.3) regardless of what the frontend already checked; the backend must behave correctly even if called by a client other than the Paradox frontend.
3. **Database constraints enforce critical integrity rules** as a final backstop (`CHECK`, `NOT NULL`, `UNIQUE`, foreign keys — Section 6.1) independent of application-code correctness, so a bug in service-layer logic cannot silently corrupt data.

## 9.2 Frontend Validation

- Frontend validation is implemented using **Zod** schemas (`src/features/<domain>/schemas/`), shared between form validation and TypeScript typing via `z.infer`. Zod is a frontend dependency declared in `frontend/package.json`.
- Required-field validation before submission is allowed.
- Email fields validate RFC 5322 compliance client-side.
- Password fields enforce minimum 8 characters and complexity (numbers, mixed case, special characters).
- Numeric/monetary inputs must reject non-numeric characters and enforce a positive-value constraint client-side.
- Date pickers must disallow selecting a future date at the UI level.
- Field-level error messages render adjacent to the relevant input.
- On validation failure, previously entered valid values are preserved (no form reset).
- Submit controls show a loading/disabled state while a request is in flight, to prevent duplicate submissions.

## 9.3 Backend Validation

- All request bodies are validated via Pydantic schemas (`app/schemas/*`) before reaching the service layer.
- Email normalization (lowercased, trimmed) and password hashing verification are handled in the security/service layer.
- Business rules not expressible purely in schema terms (e.g., "category must exist and belong to user or be default", "date not in the future" relative to server clock) are enforced in the service layer.
- Database-level constraints (`CHECK`, `NOT NULL`, `UNIQUE`, foreign keys) provide a final integrity backstop independent of application code correctness.

## 9.4 Field-Level Rules Summary

| Field | Rule |
|---|---|
| `user.email` | Required; valid email format; trimmed; lowercased; max 255 chars; unique in database |
| `user.password` | Required on registration/reset/change; min 8 characters; max 128 characters; hashed with BCrypt/Argon2 |
| `user.display_name` | Optional; 1 to 100 characters; trimmed |
| `auth.token` | Required for reset-password / Google OAuth verification; non-empty string |
| `expense.amount` | Required; decimal > 0; max 2 decimal places; reasonable upper bound (e.g., < 10,000,000) |
| `expense.date` | Required; valid ISO date; must not be after current server date |
| `expense.category_id` | Required; must reference an existing starter or user-owned category |
| `expense.payment_method_id` | Required; must reference an existing starter or user-owned payment method |
| `expense.description` | Optional; max 255 characters |
| `category.name` / `payment_method.name` | Required; non-empty after trim; max 60 characters; unique per user |
| `budget.amount` | Required; decimal ≥ 0; max 2 decimal places |
| `budget.period_type` | Required; enum: `'month'`, `'week'`, `'day'` |
| `budget.period_key` | Required; regex validated (`YYYY-MM`, `YYYY-Www`, `YYYY-MM-DD`) |

---

# 10. Business Logic

## 10.1 Financial Calculation Accuracy

- All monetary arithmetic (sums, percentages, remaining budget) is performed server-side using Python's `Decimal` type, never `float`.
- Rounding, where necessary for display (e.g., percentage breakdowns), occurs only at the presentation boundary (serialization), never in intermediate calculations.

## 10.2 Budget Status Derivation

Given `budget_amount` and `amount_spent` for the current calendar month:

- `remaining_amount = budget_amount - amount_spent`
- `status = "under_budget"` if `amount_spent < 0.9 * budget_amount`
- `status = "near_limit"` if `0.9 * budget_amount <= amount_spent <= budget_amount`
- `status = "over_budget"` if `amount_spent > budget_amount`
- If no budget has been set, `status` is omitted/`null` and the frontend renders a "no budget set" state rather than a fabricated status.

## 10.3 Category / Payment Method Deletion Rule

(Restated from Section 6.4 as the authoritative business rule.)

1. Starter (`is_default = true`) categories and payment methods — including the reserved `Uncategorized` category and `Other` payment method — can never be deleted. Attempting to do so returns `409 Conflict`.
2. Deleting a custom category or payment method that has zero referencing expenses simply removes the row.
3. Deleting a custom category or payment method that has one or more referencing expenses reassigns those expenses to the reserved fallback (`Uncategorized` / `Other`) inside a single transaction, then removes the row. This guarantees FR-08 ("must not make existing expense records unclear or inconsistent") without requiring manual user cleanup.

## 10.4 Starter Category/Payment Method Seed

Seeded once via an Alembic data migration (not application runtime logic), so the seed is versioned and reproducible across environments. See Section 27.2–27.3 for the exact seed lists.

## 10.5 Single Fixed Currency

No currency field exists on any monetary value in V1. All amounts are implicitly in the single fixed currency established at deployment configuration (documented, not user-configurable, in Phase 1).

## 10.6 Time Period Semantics

- "Current month" = the calendar month containing the server's current date, in the server's configured timezone (UTC by default; see Section 19).
- Trend buckets (Section 7.6) use ISO week boundaries within the selected month for `current_month`, or rolling 7-day buckets for `last_30_days`.

---

# 11. Frontend Requirements

## 11.1 UI/UX Requirements

> **Design System reference**: All frontend UI implementation (visual styling, color, typography, spacing scale, component variants, iconography) must follow `docs/DESIGN_SYSTEM.md`. This SRS defines *functional* and *behavioral* UI/UX requirements only (what must exist, how it must behave, what states it must support); it deliberately does not duplicate visual design rules, which live exclusively in the Design System document. Where this SRS and the Design System appear to overlap, the Design System governs visual specifics and this SRS governs behavior/functionality.

- Every primary screen (Dashboard, Expenses, Categories, Budget) must be understandable without documentation, per the PRD's simplicity principle.
- Visual hierarchy must clearly distinguish primary actions (e.g., "Add Expense") from secondary actions (e.g., filters, sort).
- Spacing, typography, and component behavior must be consistent across all screens (shared `components/ui` primitives).
- Navigation must make the four primary areas (Dashboard, Expenses, Categories/Payment Methods, Budget) reachable within one interaction from any screen.
- Forms must use accessible layouts: associated `<label>` elements, grouped related fields, and inline validation messages.
- Financial information (totals, budget status, category breakdowns) must be presented with clear, unambiguous labeling — no unexplained abbreviations or icons-only indicators for monetary state.
- Every user action that mutates data (create/update/delete) must produce visible feedback (toast, inline confirmation, or state change) — no silent success/failure.
- Destructive actions (delete expense, delete category/payment method) must require explicit confirmation via a dialog before the request is sent.
- The UI must avoid unnecessary visual complexity (no decorative charts/effects that do not aid understanding of spending).

## 11.2 Screen-Level Requirements

| Screen | Route | Core Content |
|---|---|---|
| Dashboard | `/dashboard` (or `/` redirect) | Total spent, budget status card, category breakdown chart, recent expenses list |
| Expense List | `/expenses` | Filter/search/sort bar, paginated expense table/list, "Add Expense" action |
| Add Expense | `/expenses/new` | Expense form |
| Expense Detail/Edit | `/expenses/[id]` | Pre-filled expense form, delete action |
| Categories | `/categories` | List of categories, create/rename/delete actions |
| Budget | `/budget` | Current budget amount, edit form, current status summary |

## 11.3 Responsive Design Requirements

- The frontend must render correctly at common breakpoints: mobile (≤ 480px), large mobile/small tablet (481–768px), tablet (769–1024px), and desktop (≥ 1025px).
- Navigation collapses to a mobile-appropriate pattern (e.g., bottom nav or hamburger menu) below tablet width; it is not simply a shrunk desktop nav bar.
- The expense list renders as a stacked card layout on mobile and a tabular layout from tablet width upward — not a horizontally scrolling table.
- Dashboard summary cards reflow from a multi-column grid (desktop) to a single column (mobile).
- Category breakdown visualizations must remain legible on narrow viewports (e.g., switching from a multi-series chart to a simple ranked list if needed).
- No primary workflow (add/edit/delete expense, review dashboard, set budget) should require horizontal scrolling at any supported breakpoint.
- Touch targets on mobile meet a minimum comfortable size (approx. 44×44px) for all interactive controls; desktop retains precise mouse/keyboard interaction (hover states, keyboard shortcuts for form navigation where relevant).

## 11.4 Form and Input Validation (Frontend Responsibilities)

See Section 9.2. Additionally:
- Amount inputs use a numeric input mode appropriate to mobile keyboards.
- Date inputs default to today and use a native or accessible date-picker component.
- Category/payment method selectors are searchable/filterable once the list grows beyond a handful of items.

## 11.5 Micro-interactions and Framer Motion

- **Use Framer Motion for**: page/section transitions between primary routes; modal/dialog open-close transitions (add/edit expense modal or delete confirmation); list item insertion/removal animations in the expense list; dashboard card entrance animation on initial load; expand/collapse of filter panels; subtle success/error feedback animation on form submission; subtle hover/tap feedback on interactive cards and buttons.
- **Do not use animation for**: decorative purposes unrelated to state communication; anything that delays the user's ability to complete the next action.
- All animations must respect the `prefers-reduced-motion` media query — when set, transitions are replaced with immediate state changes or minimal fades.
- Animation durations are kept short (target ≤ 250ms for micro-interactions, ≤ 400ms for section transitions) to preserve the "under 30 seconds to record an expense" product goal.

## 11.6 Accessibility Requirements

- All interactive elements (buttons, form controls, links, modal triggers) must be reachable and operable via keyboard alone.
- Visible focus indicators must be present on all focusable elements (never suppressed via `outline: none` without a replacement indicator).
- Semantic HTML elements are used for structure (`<nav>`, `<main>`, `<form>`, `<table>` or appropriate ARIA roles for list/card-based data).
- All form controls have programmatically associated labels.
- Error, success, loading, and empty states are announced in a screen-reader-friendly manner (e.g., `aria-live` regions for toasts/status messages).
- Color is never the sole indicator of budget status or validation state; text/iconography accompanies color coding.
- `prefers-reduced-motion` is respected as stated in Section 11.5.

## 11.7 Frontend Quality Principle

Implementation priority order, per the finalized decision: **Clarity → Usability → Responsiveness → Feedback → Polish.** Visual polish must never be implemented at the expense of an earlier-priority item.

## 11.8 Frontend Server-State Management

The frontend requires a consistent, simple approach for fetching, caching, mutating, and refreshing server data (expenses, categories, payment methods, budget, dashboard). Given the single-user MVP scope, this is kept deliberately minimal — no global client-state store (Redux, Zustand, etc.) is introduced for server data.

- **Library**: **TanStack Query (React Query)** is used as the server-state layer, wrapping the API client (`src/lib/api/*`). It is the standard, low-overhead solution for fetch/cache/mutate/invalidate in a Next.js App Router project and avoids hand-rolled caching logic.
- **Fetching**: Each feature module's `hooks/` directory exposes query hooks (e.g., `useExpenses(filters)`, `useDashboard(period)`, `useCategories()`) that wrap TanStack Query's `useQuery`, keyed by resource and relevant parameters (e.g., `["expenses", { filters, sort, page }]`).
- **Caching**: Default cache/staleness behavior is intentionally simple — a short `staleTime` (e.g., treat data as fresh for a small window, such as 30 seconds) is sufficient for a single-user app with no concurrent external writers; there is no need for aggressive background-sync or offline-first caching in V1.
- **Mutation**: Create/update/delete operations (add expense, edit expense, delete expense, manage categories/payment methods, set budget) use TanStack Query's `useMutation`, wrapping the corresponding `lib/api/*` call.
- **Refetch / Invalidation**: On successful mutation, the relevant query keys are invalidated so dependent views stay consistent without a manual page reload — e.g., a successful expense create/update/delete invalidates the `expenses` list query, the `dashboard` query, and the `budget` query (since totals and status depend on expense data); a category/payment-method mutation invalidates the corresponding list query and the `expenses` query (labels shown in the list may reference it) and the `dashboard` query.
- **Loading/Error mapping**: Each query/mutation hook's `isLoading`/`isPending` and `isError`/`error` states drive the loading and error UI states required by Section 11.1 and FR-24 (SRS-FN-29) — components do not implement their own separate loading/error tracking on top of this.
- **Scope boundary**: This approach governs **server** state only. Local/ephemeral UI state (form input before submission, filter-panel open/closed, modal visibility) continues to use plain React state (`useState`/`useReducer`) within the owning component or feature hook — it is not placed in the server-state cache.

---

# 12. Backend Requirements

## 12.1 Structural Requirements

The backend must follow the structure defined in Section 23.1 exactly, with the **Router → Service → Repository → Database** layering defined in Section 5.2.

## 12.2 Core Module Responsibilities

| Module | Responsibility |
|---|---|
| `app/main.py` | FastAPI app instantiation, middleware registration (CORS, Rate Limiting), router inclusion |
| `app/api/router.py` | Aggregates all resource routers under `/api/v1` |
| `app/api/auth.py` | Authentication router (`/register`, `/login`, `/google`, `/refresh`, `/logout`, `/logout-all`, `/me`, `/forgot-password`, `/reset-password`, `/change-password`) |
| `app/api/deps.py` | Shared dependencies (`get_db`, `get_current_user` for JWT security context, pagination params) |
| `app/core/config.py` | Environment-driven settings (Pydantic `BaseSettings`) including JWT secrets and OAuth keys |
| `app/core/security.py` | Cryptographic utilities (BCrypt password hashing, JWT encoding/decoding, SHA-256 token hashing, Google ID token verification) |
| `app/core/exceptions.py` | Domain exception classes (`NotFoundError`, `ValidationError`, `ConflictError`, `AuthenticationError`, `AuthorizationError`, `RateLimitError`) |
| `app/core/error_handlers.py` | FastAPI exception handlers translating domain exceptions to the standard error envelope (Section 8.4) |
| `app/core/logging.py` | Structured logging configuration (sanitizing tokens/passwords) |
| `app/db/session.py` | Async SQLAlchemy engine/session factory |
| `app/db/base.py` | Declarative base and model registry for Alembic autogeneration |
| `app/db/models/*` | SQLAlchemy ORM entities (`User`, `RefreshToken`, `PasswordResetToken`, `Expense`, `Budget`, `Category`, `PaymentMethod`) |
| `app/schemas/*` | Pydantic request/response models per resource and auth domain |
| `app/services/*` | Business logic per resource (`AuthService`, `ExpenseService`, `CategoryService`, `PaymentMethodService`, `BudgetService`, `DashboardService`) |
| `app/repositories/*` | Database access per resource enforcing `user_id` scoping |
| `app/utils/pagination.py` | Shared pagination helper logic |
| `app/utils/datetime.py` | Date/time normalization helpers |
| `app/utils/money.py` | Decimal-safe monetary helpers |
| `app/constants/categories.py` | Starter category seed definitions |
| `app/constants/payment_methods.py` | Starter payment method seed definitions |

## 12.3 Backend Behavioral Requirements

- All I/O (database calls) is asynchronous (`asyncpg` + SQLAlchemy async session).
- Every service method that mutates data must be wrapped in a single database transaction; partial writes must not be possible.
- Every protected service and repository method accepts an explicit `user_id: UUID` from the authenticated security context.
- Services return domain-level results or raise domain exceptions; routers translate these into HTTP responses via standard envelopes.
- Pagination defaults (`page=1`, `page_size=20`, `max page_size=100`) are enforced centrally in `utils/pagination.py`.

---

# 13. Authentication, Authorization & User Data Isolation

## 13.1 Authentication Architecture

Paradox implements a production-ready **JWT + HttpOnly Rotated Refresh Token** authentication model:

```
User Login / OAuth ──► Validates Credentials / Google ID Token
                     │
                     ├─► Issues Access Token (JWT, 15 min lifetime)
                     │     └─► Sent in JSON response body ➔ Stored in-memory in frontend AuthContext
                     │
                     └─► Issues Refresh Token (Opaque string, 7–30 day lifetime)
                           ├─► SHA-256 Hash stored in `refresh_tokens` table
                           └─► Sent via `Set-Cookie: paradox_refresh_token=...; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth`
```

## 13.2 Token Lifecycle, Rotation & Revocation

1. **Short-Lived Access Token**:
   - Lifetime: 10–15 minutes (configurable via `ACCESS_TOKEN_EXPIRE_MINUTES`).
   - Signed with `JWT_SECRET_KEY` using algorithm `HS256`.
   - Claims: `sub` (`user_id`), `email`, `exp`, `iat`, `type: "access"`.
   - Passed in `Authorization: Bearer <access_token>` header on every API call.
2. **Long-Lived Refresh Token**:
   - Lifetime: 7–30 days (configurable via `REFRESH_TOKEN_EXPIRE_DAYS`).
   - Stored in an `HttpOnly`, `Secure`, `SameSite="Lax"` cookie scoped to `Path=/api/v1/auth`.
   - **Database Security**: The database never stores plaintext refresh tokens; only a cryptographic hash `SHA-256(token)` is persisted in `refresh_tokens`.
3. **Refresh Token Rotation**:
   - Calling `/api/v1/auth/refresh` looks up the hashed token in `refresh_tokens`.
   - The used token is revoked or replaced.
   - A brand-new refresh token is generated, hashed, persisted, and set in the response cookie along with a fresh access token.
   - If an expired, revoked, or tampered token is presented, the request is rejected with `401 Unauthorized`.
4. **Server-Side Session Revocation**:
   - `/api/v1/auth/logout`: Revokes the current session's refresh token hash and clears the cookie.
   - `/api/v1/auth/logout-all`: Marks all active refresh tokens for `current_user.id` as revoked in the database (`is_revoked = true`), immediately terminating all active mobile/desktop sessions.

## 13.3 Password Security & Hashing

- All passwords are encrypted with `bcrypt` (or `argon2`) utilizing automatic salt generation.
- Plaintext passwords are never stored, logged, or included in any API response or telemetry.
- Minimum password length is 8 characters with strength requirements enforced in Zod (frontend) and Pydantic (backend).

## 13.4 Google Sign-In (OAuth 2.0 / OpenID Connect)

- Frontend obtains Google ID Token via Google Identity Services (`@react-oauth/google`).
- ID Token is transmitted to `POST /api/v1/auth/google`.
- Backend validates the token signature directly against Google's public JSON Web Key Sets (JWKS) and verifies `aud == GOOGLE_CLIENT_ID` and `iss in ["accounts.google.com", "https://accounts.google.com"]`.
- The user is matched by verified email:
  - If user exists, links `google_id` if missing.
  - If user does not exist, provisions a new account with `is_verified = true` and `password_hash = null`.
- Issues standard application Access Token and Refresh Token cookie.

## 13.5 Row-Level Multi-Tenant User Data Isolation (CRITICAL)

- **Zero Trust in Client Identifiers**: The backend never accepts or trusts a `user_id` query parameter or body field sent by the frontend for access control.
- **SecurityContext Derivation**: FastAPI dependency `get_current_user` parses and cryptographically validates the Bearer JWT, fetches the user from the database, and injects `current_user: User` into routers.
- **Strict Query Scoping**:
  * **Expenses**: `select(Expense).where(Expense.user_id == current_user.id, ...)`
  * **Budgets**: `select(Budget).where(Budget.user_id == current_user.id, ...)`
  * **Categories**: `select(Category).where(or_(Category.is_default == True, Category.user_id == current_user.id))`
  * **Payment Methods**: `select(PaymentMethod).where(or_(PaymentMethod.is_default == True, PaymentMethod.user_id == current_user.id))`
- **Unauthorized Mutation Protection**: Attempting to view, edit, or delete another user's expense or custom entity returns `404 Not Found` (ensuring resource existence is never leaked).

---

# 14. Security Requirements

| ID | Requirement |
|---|---|
| SRS-SEC-01 | No secrets (JWT secret keys, database credentials, Google OAuth secrets) may be committed to version control. |
| SRS-SEC-02 | All configuration is sourced from environment variables via `app/core/config.py`. |
| SRS-SEC-03 | All request bodies and query parameters are validated server-side via Pydantic schemas. |
| SRS-SEC-04 | Rate limiting is enforced on auth endpoints (`/auth/login`, `/auth/register`, `/auth/forgot-password`) to prevent brute-force attacks. |
| SRS-SEC-05 | Error responses must not leak stack traces, SQL fragments, internal paths, or token secrets. |
| SRS-SEC-06 | CORS is restricted to known frontend origins (`CORS_ALLOWED_ORIGINS`). |
| SRS-SEC-07 | Refresh tokens are stored exclusively in `HttpOnly`, `Secure`, `SameSite=Lax` cookies to prevent XSS exfiltration. |
| SRS-SEC-08 | Cookie-based refresh and logout endpoints are protected against CSRF via SameSite attributes and custom origin validation. |
| SRS-SEC-09 | Passwords are hashed with BCrypt/Argon2 with high work factor before database insertion. |
| SRS-SEC-10 | Dependency versions are pinned in `requirements.txt` and `package.json`. |

## 14.1 Threat-Category Protections

| Threat Category | Protection Mechanism |
|---|---|
| **CORS** | Configured via `CORS_ALLOWED_ORIGINS` with credentials support (`allow_credentials=True`) strictly for whitelisted origins. |
| **XSS** | Frontend auto-escapes all strings; Refresh Tokens are `HttpOnly` so Javascript cannot access them; Access Tokens are stored purely in memory. |
| **CSRF** | SameSite cookie policy (`SameSite=Lax`) + requirement of `Content-Type: application/json` on state-changing endpoints prevents cross-site submission. |
| **SQL Injection** | Parameterized queries constructed strictly via SQLAlchemy 2.0 ORM. |
| **Brute Force** | Rate limiting applied via `slowapi` or Redis/in-memory limiter on sensitive authentication routes. |
| **Token Hijacking** | Access tokens are short-lived (15 mins); refresh tokens are rotated on every use and hashed in the database; single and global session revocation enabled. |

---

# 15. Error Handling

## 15.1 Error Handling Philosophy

Errors must be predictable, consistently shaped (Section 8.4), and understandable — both to a developer integrating against the API and to the non-technical end user viewing the frontend's rendering of that error (FR-24).

## 15.2 Domain Exception Taxonomy (`app/core/exceptions.py`)

| Exception | HTTP Status | `error.code` |
|---|---|---|
| `NotFoundError` | 404 | `NOT_FOUND` |
| `ValidationError` | 422 | `VALIDATION_ERROR` |
| `ConflictError` (e.g., duplicate email, duplicate category name) | 409 | `CONFLICT` |
| `AuthenticationError` (invalid credentials, expired/revoked token) | 401 | `UNAUTHORIZED` |
| `AuthorizationError` (access denied to resource) | 403 | `FORBIDDEN` |
| `RateLimitError` (too many failed requests) | 429 | `RATE_LIMITED` |
| `UnprocessableRequestError` | 422 | `INVALID_REQUEST` |
| Unhandled exception | 500 | `INTERNAL_ERROR` |

Pydantic schema validation failures are caught by a FastAPI exception handler and mapped to the same `422` / `VALIDATION_ERROR` shape as domain-level `ValidationError`, so API consumers see one consistent validation error format regardless of whether the failure originated in schema parsing or business-rule enforcement.

## 15.3 Frontend Error Handling

- The API client (`lib/api/client.ts`) normalizes all error responses into a single internal error type before they reach feature components.
- `VALIDATION_ERROR` responses are mapped to field-level form errors when `details[].field` matches a known form field; otherwise shown as a form-level message.
- `NOT_FOUND` on a detail/edit screen navigates the user back to the relevant list with a clear message rather than showing a raw error page.
- `CONFLICT` (e.g., attempting to delete a starter category) is shown as an explanatory inline message, not a generic failure toast.
- `INTERNAL_ERROR` / network failures show a generic, non-technical "something went wrong, please try again" state with a retry action, never raw error text or stack traces.

## 15.4 Information Disclosure Rule

No error response, in any environment, may include: raw exception messages from third-party libraries, SQL statements, file system paths, or internal stack traces. In non-production environments, additional debug detail may be logged server-side (Section 18) but is never included in the HTTP response body.

---

# 16. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | Recording an ordinary expense (submit → success feedback) must complete in a time consistent with the PRD's "under 30 seconds" end-to-end usability target, with backend response times for standard CRUD operations targeted at < 300ms under normal local/staging load. |
| Usability | Every primary screen must be operable by a non-technical user without external documentation (PRD Section 12: Simplicity). |
| Accuracy | Totals, category summaries, and budget comparisons must always exactly reflect the sum of currently persisted expense records (PRD Section 12: Accuracy). |
| Consistency | The same expense/category/payment-method data must render identically (same values, same formatting) across list, detail, dashboard, and budget views. |
| Reliability | A successfully persisted expense must never be silently lost; all mutating endpoints return an explicit success/failure result, and the frontend never assumes success without a confirmed response. |
| Maintainability | The layered backend architecture (Section 5.2) and feature-organized frontend (Section 5.3) must be preserved as the codebase grows, to keep Phase 2+ changes low-risk. |
| Portability | Local development requires no Docker (Next.js, FastAPI, and PostgreSQL run natively); this must not regress as dependencies are added. |
| Scalability path | While V1 is explicitly single-user, the schema (UUID keys, isolated `users` table) and layered architecture must not require a rewrite to support Phase 3's multi-user requirements — only additive changes (auth middleware, `user_id` foreign keys, row-level filtering). |
| Availability | The `/api/v1/health` endpoint must accurately reflect service and database availability for use by deployment-platform health checks. |

---

# 17. Testing Strategy

## 17.1 Testing Layers

| Layer | Location | Focus |
|---|---|---|
| Unit tests | `backend/tests/unit/` | Pure business logic in `services/`, `utils/money.py`, `utils/datetime.py`, budget status derivation (Section 10.2), pagination helpers |
| Repository/integration tests | `backend/tests/integration/` | Repository methods against a real (test) PostgreSQL instance; constraint enforcement (unique names, FK restrictions, `CHECK` constraints) |
| API tests | `backend/tests/api/` | Full request/response cycle per endpoint (Section 7), including validation and error-envelope shape |
| Frontend component tests | `frontend` (co-located or `__tests__`) | Form validation behavior, empty/loading/error state rendering, budget status display logic |
| End-to-end tests | Critical journeys only | Add expense → appears in list → appears in dashboard → affects budget status; delete a referenced custom category → expenses reassigned to `Uncategorized` |

## 17.2 Priority Test Coverage (must-have for MVP sign-off)

- Expense creation rejects non-positive amount and future-dated expenses (SRS-FN-02, SRS-FN-03).
- Category/payment-method deletion protection for `is_default = true` rows (SRS-FN-12, 10.3).
- Category/payment-method deletion reassignment transaction correctness (10.3/6.4) — no expense is left referencing a deleted row.
- Budget status thresholds (10.2) computed correctly at boundary values (exactly 90%, exactly 100%, just over 100%).
- Dashboard aggregation correctness against a known seeded dataset (totals, category breakdown percentages sum to ~100%, top categories ordering).
- Pagination, search, single-dimension filter (date range OR category), and sort parameter combinations on `GET /api/v1/expenses`, including the `422` rejection when both `category_id` and a date-range parameter are supplied together (Section 3.4.1).
- Monetary values never lose precision across create → read → update round-trips (Decimal fidelity test).

## 17.3 Testing Tools (implementation guidance, not code)

- Backend: `pytest`, `pytest-asyncio`, an isolated test database (or transactional rollback per test) driven by Alembic migrations.
- Frontend: a component-testing framework compatible with Next.js/React (e.g., React Testing Library) for component/unit tests; an E2E framework for the critical journeys above.

---

# 18. Logging and Monitoring

## 18.1 Logging

- Structured (JSON) logging is configured in `app/core/logging.py`, with distinct log levels (`DEBUG`, `INFO`, `WARNING`, `ERROR`).
- Every request is logged with method, path, status code, and duration at `INFO` level (or `WARNING`/`ERROR` for 4xx/5xx).
- Unhandled exceptions are logged at `ERROR` level with full server-side context (safe to log internally; never returned to the client per Section 15.4).
- No sensitive configuration values (database passwords, connection strings) are ever logged, even at `DEBUG` level.

## 18.2 Monitoring (Phase-appropriate scope)

- The `/api/v1/health` endpoint is the primary monitoring surface for V1, used by the deployment platform's built-in health checks (Render) — no separate monitoring stack is introduced in Phase 1, consistent with avoiding premature operational complexity.
- Deployment-platform-native logs (Render/Vercel dashboards) serve as the log aggregation mechanism for V1; a dedicated log-aggregation service is deferred to Phase 5 ("Production-Ready Product").

---

# 19. Configuration and Environment Management

## 19.1 Backend Environment Variables

| Category | Variable | Required | Default / Example | Purpose |
|---|---|---|---|---|
| Database | `DATABASE_URL` | Yes | `postgresql+asyncpg://postgres:postgres@localhost:5432/paradox` | asyncpg PostgreSQL connection string |
| Application | `APP_ENV` | Yes | `local` (`local`, `staging`, `production`) | Environment toggle |
| Application | `DEBUG` | No | `true` | Verbose debug flag |
| API | `API_V1_PREFIX` | No | `/api/v1` | Routing prefix |
| API | `APP_NAME` | No | `Paradox` | Application metadata name |
| CORS | `CORS_ALLOWED_ORIGINS` | Yes | `http://localhost:3000` | Whitelisted frontend origins |
| Security / JWT | `JWT_SECRET_KEY` | Yes | `<random-32-byte-hex-secret>` | Secret key for signing Access Tokens |
| Security / JWT | `JWT_ALGORITHM` | No | `HS256` | JWT signing algorithm |
| Security / JWT | `ACCESS_TOKEN_EXPIRE_MINUTES`| No | `15` | Lifetime of short-lived access token |
| Security / JWT | `REFRESH_TOKEN_EXPIRE_DAYS` | No | `7` | Lifetime of rotated refresh token cookie |
| Google OAuth | `GOOGLE_CLIENT_ID` | No | `...apps.googleusercontent.com` | Google OAuth 2.0 Web Client ID |
| Google OAuth | `GOOGLE_CLIENT_SECRET` | No | `...` | Google OAuth Client Secret |
| Application | `FRONTEND_URL` | No | `http://localhost:3000` | Base URL for password reset links |
| Timezone | `APP_TIMEZONE` | No | `UTC` | Server reference timezone |

## 19.2 Frontend Environment Variables

| Category | Variable | Required | Default / Example | Purpose |
|---|---|---|---|---|
| API base URL | `NEXT_PUBLIC_API_BASE_URL` | Yes | `http://127.0.0.1:8000/api/v1` | Target backend REST endpoint |
| Google OAuth | `NEXT_PUBLIC_GOOGLE_CLIENT_ID`| No | `...apps.googleusercontent.com` | Google Identity button Client ID |

## 19.3 File Distinctions

| File | Purpose | Committed? |
|---|---|---|
| `.env.example` | Documents all required variables with placeholder/dummy values | Yes |
| `.env` (backend) | Actual local backend configuration | No (gitignored) |
| `.env.local` (frontend) | Actual local frontend configuration (Next.js convention) | No (gitignored) |

---

# 20. Deployment Requirements

## 20.1 Local Development Topology

```
Local:
Next.js (native)  +  FastAPI (native, uvicorn)  +  PostgreSQL (native/local service)
No Docker required for local development.
```

- Backend: run via `uvicorn app.main:app --reload` against a local or locally-reachable PostgreSQL instance.
- Frontend: run via the Next.js dev server (`next dev`), configured with `NEXT_PUBLIC_API_BASE_URL` pointing at the local backend.
- Database: a local PostgreSQL instance (developer-managed), with Alembic migrations applied via `alembic upgrade head`.

## 20.2 Deployment Topology

```
Deployment:
Next.js   → Vercel
FastAPI   → Docker image → Render
PostgreSQL → Supabase
```

- **Frontend (Vercel)**: builds and deploys directly from `frontend/` using Next.js build (`next build`); environment variables configured in Vercel project settings.
- **Backend (Render)**: built and deployed via `backend/Dockerfile`, running ASGI uvicorn workers; environment variables configured in Render service dashboard.
- **Database (Supabase)**: managed PostgreSQL with Alembic migrations executed as a pre-deploy or deploy step.

---

# 21. API ↔ Database ↔ UI Traceability

| API Endpoint | Database Entities Touched | Primary UI Surface |
|---|---|---|
| `POST /api/v1/auth/register` | `users`, `refresh_tokens` | Register Screen (`/register`) |
| `POST /api/v1/auth/login` | `users`, `refresh_tokens` | Login Screen (`/login`) |
| `POST /api/v1/auth/google` | `users`, `refresh_tokens` | Google Sign-In Button on Login/Register |
| `POST /api/v1/auth/refresh` | `refresh_tokens` | Background Axios/Fetch Interceptor |
| `POST /api/v1/auth/logout` | `refresh_tokens` | Topbar / Navigation Drawer Logout |
| `POST /api/v1/auth/logout-all` | `refresh_tokens` | User Profile / Security Settings |
| `POST /api/v1/auth/forgot-password` | `users`, `password_reset_tokens` | Forgot Password Form (`/forgot-password`) |
| `POST /api/v1/auth/reset-password` | `users`, `password_reset_tokens`, `refresh_tokens` | Reset Password Form (`/reset-password`) |
| `POST /api/v1/auth/change-password`| `users` | User Profile / Security Settings |
| `GET /api/v1/auth/me` | `users` | Topbar User Profile Avatar / Initial |
| `POST/GET/PATCH/DELETE /api/v1/expenses[/…]` | `expenses` (scoped to `user_id`) | Expense List, Add Expense, Expense Detail/Edit |
| `POST/GET/PATCH/DELETE /api/v1/categories[/…]` | `categories` (scoped to `user_id` & defaults) | Categories Screen; Category selector in Expense form |
| `POST/GET/PATCH/DELETE /api/v1/payment-methods[/…]` | `payment_methods` (scoped to `user_id` & defaults) | Categories/Payment Methods screen; Dropdown |
| `GET/PUT/DELETE /api/v1/budget[/all]` | `budgets` (scoped to `user_id`) | Budget Planner Screen; Dashboard budget card |
| `GET /api/v1/dashboard` | Reads `expenses`, `categories`, `budgets` for `user_id` | Dashboard screen |
| `GET /api/v1/health` | Connectivity check only | N/A (operational) |

---

# 22. Requirement Traceability Matrix

| ID | Requirement | SRS Requirement(s) | API Area | DB Entity | Test Coverage Area |
|---|---|---|---|---|---|
| AUTH-01 | Sign up / Register | SRS-FN-AUTH-01 | `POST /auth/register` | `users`, `refresh_tokens` | Unit, API test |
| AUTH-02 | Login with email/pass | SRS-FN-AUTH-02 | `POST /auth/login` | `users`, `refresh_tokens` | API test, token verification |
| AUTH-03 | Google OAuth / OIDC | SRS-FN-AUTH-03 | `POST /auth/google` | `users`, `refresh_tokens` | Mocked OIDC token verification test |
| AUTH-04 | Refresh token rotation | SRS-FN-AUTH-04 | `POST /auth/refresh` | `refresh_tokens` | Rotation & reuse attack test |
| AUTH-05 | Secure logout | SRS-FN-AUTH-05 | `POST /auth/logout` | `refresh_tokens` | Cookie clear & DB revocation test |
| AUTH-06 | Logout all devices | SRS-FN-AUTH-06 | `POST /auth/logout-all` | `refresh_tokens` | Multi-session revocation test |
| AUTH-07 | Forgot & reset password | SRS-FN-AUTH-07 | `/auth/forgot-password`, `/auth/reset-password` | `users`, `password_reset_tokens` | Password recovery lifecycle test |
| AUTH-08 | Change password | SRS-FN-AUTH-08 | `POST /auth/change-password` | `users` | Current password check test |
| AUTH-09 | User data isolation | SRS-FN-AUTH-09 | All `/expenses`, `/budget`, `/categories` | All entities | Multi-user tenant separation test |
| AUTH-10 | Unauthorized access rejection | SRS-FN-AUTH-10 | All protected endpoints | All entities | 401 & cross-user 404 test |
| FR-01 | Create expense | SRS-FN-01 | `POST /expenses` | `expenses` | API tests, user_id check |
| FR-02 | Prevent invalid expenses | SRS-FN-02, SRS-FN-03 | `POST/PATCH /expenses` | `expenses` | Unit + API tests |
| FR-03 | View expense history | SRS-FN-04 | `GET /expenses`, `GET /expenses/{id}` | `expenses` | User-scoped API tests |
| FR-04 | Edit expense | SRS-FN-05 | `PATCH /expenses/{id}` | `expenses` | User ownership check |
| FR-05 | Delete expense with confirmation | SRS-FN-06 | `DELETE /expenses/{id}` | `expenses` | User ownership check |
| FR-06 | Starter categories | SRS-FN-07, SRS-FN-09 | `POST/GET /categories` | `categories` | Seed migration test, shared visibility |
| FR-07 | Custom categories | SRS-FN-10, SRS-FN-11 | `POST/PATCH /categories` | `categories` | User-scoped category test |
| FR-08 | Category deletion integrity | SRS-FN-12, SRS-FN-13 | `DELETE /categories/{id}` | `categories`, `expenses` | Cascade reassignment in-user test |
| FR-09 | Payment methods | SRS-FN-14, SRS-FN-15 | `/payment-methods` | `payment_methods` | User-scoped payment method test |
| FR-10 | Review by date range | SRS-FN-16 | `GET /expenses?date_from&date_to` | `expenses` | User-scoped date filter test |
| FR-11 | Search history | SRS-FN-17 | `GET /expenses?search` | `expenses` | User-scoped text search test |
| FR-12 | Single-dimension filter | SRS-FN-18 | `GET /expenses` | `expenses` | Filter mutual exclusion test |
| FR-13 | Sort expenses | SRS-FN-19 | `GET /expenses?sort_by&sort_order` | `expenses` | Sorting test |
| FR-14 | Accurate totals | SRS-FN-20 | `GET /dashboard` | `expenses` | Decimal sum fidelity test |
| FR-15 | Category breakdown | SRS-FN-21 | `GET /dashboard` | `expenses`, `categories` | User-scoped aggregate test |
| FR-16 | Spending trends | SRS-FN-22 | `GET /dashboard?period` | `expenses` | Trend bucket test |
| FR-17 | Top categories | SRS-FN-23 | `GET /dashboard` | `expenses`, `categories` | Top ranking test |
| FR-18 | Multi-granularity budget | SRS-FN-24 | `GET/PUT/DELETE /budget` | `budgets` | Multi-granularity upsert test |
| FR-19 | Budget tracking | SRS-FN-25 | `GET /budget`, `GET /dashboard` | `budgets`, `expenses` | Status calculation test |
| FR-20 | Budget status values | SRS-FN-26 | `GET /budget` | `budgets` | Threshold tests |
| FR-21 | Dashboard | SRS-FN-27 | `GET /dashboard` | all read entities | Aggregation test |
| FR-22 | Session persistence | SRS-FN-08 | all mutating endpoints | all | Refresh token lifecycle test |
| FR-23 | No fabricated data | SRS-FN-28 | `GET /dashboard`, `GET /budget` | all | Zero-data empty test |
| FR-24 | Meaningful UI states | SRS-FN-29 | all screens | N/A | Frontend UI test |

---

# 23. Project Structure

## 23.1 Backend Structure (authoritative)

```text
backend/
├── app/
│   ├── main.py
│   ├── api/
│   │   ├── deps.py                  # get_db, get_current_user SecurityContext
│   │   ├── router.py                # Aggregates /auth, /expenses, /categories, /payment_methods, /budget, /dashboard, /health
│   │   ├── auth.py                  # /auth/register, /login, /google, /refresh, /logout, /me, /forgot, /reset, /change
│   │   ├── health.py
│   │   ├── expenses.py
│   │   ├── categories.py
│   │   ├── payment_methods.py
│   │   ├── budget.py
│   │   └── dashboard.py
│   ├── core/
│   │   ├── config.py                # Settings: DB, JWT, OAuth, CORS
│   │   ├── security.py              # BCrypt, JWT encode/decode, token hashing, Google OIDC validation
│   │   ├── exceptions.py            # Domain exceptions (incl. AuthenticationError, AuthorizationError, RateLimitError)
│   │   ├── error_handlers.py
│   │   └── logging.py
│   ├── db/
│   │   ├── session.py
│   │   ├── base.py
│   │   └── models/
│   │       ├── user.py              # User entity with email, password_hash, google_id
│   │       ├── refresh_token.py     # Hashed refresh tokens for session rotation/revocation
│   │       ├── password_reset_token.py # Secure reset tokens
│   │       ├── expense.py           # Expense with user_id FK
│   │       ├── budget.py            # Budget with user_id FK & period constraints
│   │       ├── category.py          # Category with nullable user_id FK
│   │       └── payment_method.py    # PaymentMethod with nullable user_id FK
│   ├── schemas/
│   │   ├── common.py
│   │   ├── auth.py                  # UserRegister, UserLogin, TokenResponse, GoogleLogin, PasswordReset, etc.
│   │   ├── user.py
│   │   ├── expense.py
│   │   ├── category.py
│   │   ├── payment_method.py
│   │   ├── budget.py
│   │   └── dashboard.py
│   ├── services/
│   │   ├── auth_service.py          # Login, Register, Google OAuth, Refresh, Password management
│   │   ├── expense_service.py       # Scoped to user_id
│   │   ├── category_service.py      # Scoped to user_id & defaults
│   │   ├── payment_method_service.py# Scoped to user_id & defaults
│   │   ├── budget_service.py        # Scoped to user_id
│   │   └── dashboard_service.py     # Scoped to user_id
│   ├── repositories/
│   │   ├── auth_repository.py       # Users, refresh_tokens, reset_tokens DB operations
│   │   ├── user_repository.py
│   │   ├── expense_repository.py    # WHERE expense.user_id == current_user.id
│   │   ├── category_repository.py   # WHERE category.user_id == current_user.id OR is_default
│   │   ├── payment_method_repository.py
│   │   └── budget_repository.py     # WHERE budget.user_id == current_user.id
│   ├── utils/
│   │   ├── pagination.py
│   │   ├── datetime.py
│   │   └── money.py
│   └── constants/
│       ├── categories.py
│       └── payment_methods.py
├── migrations/
│   └── versions/
├── tests/
│   ├── unit/
│   │   ├── test_auth.py
│   │   ├── test_user_isolation.py
│   │   ├── test_money.py
│   │   └── test_budget_granularity.py
│   ├── integration/
│   └── api/
├── Dockerfile
├── .gitignore
├── .env.example
├── alembic.ini
├── requirements.txt
├── pyproject.toml
└── README.md
```

## 23.2 Frontend Structure (authoritative)

```text
frontend/
├── src/
│   ├── app/
│   │   ├── layout.tsx               # AuthProvider + QueryProvider + ThemeProvider + Shell
│   │   ├── page.tsx                 # Root redirect to /dashboard or /login
│   │   ├── login/
│   │   │   └── page.tsx             # Login & Google Sign-In Screen
│   │   ├── register/
│   │   │   └── page.tsx             # Registration Screen
│   │   ├── forgot-password/
│   │   │   └── page.tsx             # Password Recovery Request Screen
│   │   ├── reset-password/
│   │   │   └── page.tsx             # New Password Submission Screen
│   │   ├── dashboard/
│   │   │   └── page.tsx             # Protected Dashboard
│   │   ├── expenses/
│   │   │   ├── page.tsx             # Protected Expense List
│   │   │   ├── new/
│   │   │   │   └── page.tsx
│   │   │   └── [id]/
│   │   │       └── page.tsx
│   │   ├── categories/
│   │   │   └── page.tsx             # Protected Categories
│   │   └── budget/
│   │       └── page.tsx             # Protected Multi-Granularity Budget Planner
│   ├── features/
│   │   ├── auth/                    # Auth Context, Forms, Hooks, Schemas, Types, GoogleSignInButton
│   │   │   ├── components/
│   │   │   ├── context/
│   │   │   ├── hooks/
│   │   │   ├── schemas/
│   │   │   └── types.ts
│   │   ├── expenses/
│   │   ├── categories/
│   │   ├── payment-methods/
│   │   ├── budget/
│   │   └── dashboard/
│   ├── components/
│   │   ├── ui/
│   │   ├── layout/                  # Shell, Topbar (with User Avatar & Logout), Navigation Drawer
│   │   └── common/                  # ProtectedRoute, PwaRegister, ThemeProvider
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts            # Typed HTTP client with Bearer JWT injection & 401 Auto-Refresh
│   │   │   ├── auth.ts              # Auth API calls
│   │   │   ├── expenses.ts
│   │   │   ├── categories.ts
│   │   │   ├── payment-methods.ts
│   │   │   ├── budget.ts
│   │   │   └── dashboard.ts
│   │   ├── constants.ts
│   │   └── utils.ts
│   ├── types/
│   └── styles/
├── public/
├── package.json
├── next.config.ts
├── tsconfig.json
└── README.md
```

---

# 24. API Documentation Requirements

- FastAPI's built-in OpenAPI schema generation must document all endpoints under `/api/v1/auth`, `/api/v1/expenses`, `/api/v1/categories`, `/api/v1/payment-methods`, `/api/v1/budget`, and `/api/v1/dashboard`.
- OpenAPI schema includes HTTP Bearer security scheme definition (`HTTPBearer`).
- Interactive Swagger UI (`/docs`) and ReDoc (`/redoc`) are available in local and staging environments.

---

# 25. Data Backup and Recovery

- **Backup**: Managed PostgreSQL backups on Supabase with point-in-time recovery.
- **Migration Safety**: All schema evolutions (including user foreign keys and refresh token tables) managed via idempotent Alembic migrations.

---

# 26. Future Compatibility Requirements

| Future Phase | Compatibility Consideration |
|---|---|
| Phase 4 (Advanced Analytics) | Multi-series trend charts, recurring expenses, and export tools built on top of user-isolated tables. |
| Phase 5 (Production Enterprise) | MFA / TOTP two-factor authentication, email verification links via SendGrid/SES, and Redis-backed session blacklist. |
| Phase 6 (Multi-Currency) | Per-user currency preferences stored in `users.currency_code`. |

---

# 27. Appendices

## 27.1 Glossary

See Section 1.3.

## 27.2 Starter Category Seed List

1. Food & Dining
2. Transportation
3. Shopping
4. Entertainment
5. Bills & Utilities
6. Health
7. Education
8. Groceries
9. Uncategorized *(reserved fallback for category-deletion reassignment, Section 10.3)*

## 27.3 Starter Payment Method Seed List

1. Cash
2. Debit Card
3. Credit Card
4. Bank Transfer
5. Digital Wallet
6. Other *(reserved fallback for payment-method-deletion reassignment, Section 10.3)*

## 27.4 Budget Status Values

| Value | Meaning |
|---|---|
| `under_budget` | Spending is below 90% of the configured budget for the period |
| `near_limit` | Spending is between 90% and 100% (inclusive) of the configured budget |
| `over_budget` | Spending exceeds 100% of the configured budget |
| `null` / omitted | No budget has been configured for the selected period |

## 27.5 Standard Error Codes

| `error.code` | HTTP Status | Meaning |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Schema or business-rule validation failure |
| `INVALID_REQUEST` | 422 | Contradictory or malformed query parameters |
| `UNAUTHORIZED` | 401 | Missing, invalid, expired, or revoked authentication token |
| `FORBIDDEN` | 403 | Authenticated user lacks permission for requested resource |
| `NOT_FOUND` | 404 | Requested resource does not exist (or belongs to another user) |
| `CONFLICT` | 409 | Duplicate entity (email, category name) or deletion of protected entity |
| `RATE_LIMITED` | 429 | Too many requests on sensitive authentication endpoints |
| `INTERNAL_ERROR` | 500 | Unhandled server-side failure |

## 27.6 Definition of Done

A feature or release is considered done when:

- [ ] User registration, email/password login, Google Sign-In, and token refresh are fully functional.
- [ ] Access Tokens expire in 15 minutes; Refresh Tokens rotate on every refresh call and store only SHA-256 hashes in DB.
- [ ] Single session logout and "Logout from all devices" correctly revoke token hashes in database and clear cookies.
- [ ] Strict row-level multi-tenant user data isolation is enforced across all endpoints (`expenses`, `budgets`, `categories`, `payment_methods`).
- [ ] Attempting to access another user's resources returns `404 Not Found` without data leakage.
- [ ] Idempotent Alembic migration upgrades schema cleanly.
- [ ] Pytest unit tests, user isolation tests, and Postman API collection pass 100% green.
- [ ] Next.js frontend builds cleanly with 0 TypeScript/ESLint warnings and protected routes redirect unauthenticated users to `/login`.

---

## Document Status

**Version 2.0 — Final, Implementation-Ready SRS for Paradox (Authentication & Row-Level Multi-Tenant Data Isolation).**

This document serves as the authoritative technical source of truth for implementing authentication, session lifecycle, and data isolation in Paradox. All architectural layers and endpoint contracts defined herein are binding.
