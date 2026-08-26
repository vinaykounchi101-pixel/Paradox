# Paradox — Software Requirements Specification (SRS)

**Document Version:** 1.1
**Status:** Final — Ready for Implementation (Phase 1 / MVP)
**Product:** Paradox
**Source of Truth (Product):** `PARADOX_PRD_FINAL.md` (v1.0)
**Source of Truth (Technical Decisions):** `PARADOX_SRS_CLAUDE_PROMPT.md`
**Guiding Principle:** Validate Paradox with one real user before adding unnecessary complexity.

---

## Document Control

| Field | Value |
|---|---|
| Document Type | Software Requirements Specification |
| Product | Paradox — Personal Expense Tracker |
| Phase Covered | Phase 1 — Single-User Core MVP |
| Authoritative Product Spec | PARADOX_PRD_FINAL.md |
| Authoritative Technical Decisions | PARADOX_SRS_CLAUDE_PROMPT.md |
| Intended Consumer | Development team / AI coding agent (e.g. Antigravity) |
| Out-of-date Policy | Any change to product scope must first be reflected in the PRD, then propagated here |

## Revision History

| Version | Change Summary |
|---|---|
| 1.0 | Initial complete SRS for Phase 1. |
| 1.1 | Narrowed V1 expense filtering to a single dimension (date OR category, not combined — Section 3.4.1); added explicit default-vs-custom create/edit/delete rule tables for categories (3.2.1) and payment methods (3.3.1); added frontend server-state management approach (11.8, TanStack Query); added CI/CD pipeline (20.6); expanded baseline security threat-category coverage (14.1: XSS, SQL injection, CSRF, secure headers, CORS, secret management); added database connection pooling, concurrency-control scoping, and idempotent-seeding clarifications (6.5); reinforced the three-layer validation principle (9.1); added Design System cross-reference (11.1) without duplicating visual rules; strengthened environment-file/secret-handling language (19.3); confirmed Zod as an explicit frontend dependency for schema validation (2.2, 9.2). No existing architecture, folder structure, API surface (beyond the filter narrowing above), testing strategy, deployment decisions, Docker decisions, or `.gitignore` structure were altered. |

---

# 1. Introduction

## 1.1 Purpose

This SRS defines **how** Paradox will be technically implemented. It translates the product-level requirements defined in the PRD into a concrete, implementation-ready technical specification: architecture, database schema, API contracts, validation rules, business logic, frontend behavior, security, error handling, testing, and deployment.

This document is written so that a developer or an AI coding agent can implement Phase 1 of Paradox **without having to guess or invent requirements**. Anything not explicitly defined here or in the PRD is out of scope for this phase.

## 1.2 Scope

This SRS covers **Phase 1 — Single-User Core MVP** only, as defined in PRD Section 14. It documents:

- Expense creation, viewing, editing, deletion
- Starter and custom categories
- Payment methods
- Search, filter, sort, pagination of expenses
- Spending summaries and category breakdowns
- One overall monthly budget
- A single dashboard endpoint/screen
- Loading, empty, validation, and error states
- Single fixed currency
- Responsive web frontend
- Local development and deployment topology

This SRS does **not** cover multi-user authentication systems, multiple budgets, recurring expenses, notifications, multi-currency, bank integrations, or any other item listed as out of scope in PRD Section 13 / Section 6. Where such items are relevant to avoid an obvious future rewrite, they are documented as **Future Compatibility Considerations** (Section 26) and are explicitly **not** MVP requirements.

## 1.3 Definitions, Acronyms, and Abbreviations

| Term | Meaning |
|---|---|
| MVP | Minimum Viable Product (Phase 1 of the PRD roadmap) |
| PRD | Product Requirements Document (`PARADOX_PRD_FINAL.md`) |
| SRS | This document |
| FR-xx | Functional Requirement ID from the PRD |
| API | Application Programming Interface |
| ORM | Object-Relational Mapper |
| CRUD | Create, Read, Update, Delete |
| DTO | Data Transfer Object (represented via Pydantic schemas) |
| JWT | JSON Web Token (reserved for future multi-user auth, not used in V1) |
| Starter Category | A default category seeded at application initialization |
| Single User | The one real person Paradox is validated with in Phase 1 |

## 1.4 References

- `PARADOX_PRD_FINAL.md` — Product Requirements Document v1.0
- `PARADOX_SRS_CLAUDE_PROMPT.md` — Finalized technical decisions used to author this SRS
- FastAPI, SQLAlchemy 2.0, Alembic, Pydantic, Next.js, Framer Motion official documentation (implementation reference only; not reproduced here)

## 1.5 Intended Audience

- Development team implementing Paradox
- AI coding agents (e.g., Antigravity) generating the codebase from this document
- QA/testing contributors
- Product owner, for verifying technical fidelity to the PRD

## 1.6 Document Conventions

- Requirement IDs use the prefix `SRS-` followed by a section-scoped number (e.g., `SRS-API-01`).
- Every technical requirement traces to a PRD `FR-xx` where applicable (see Section 22).
- "Must" denotes a mandatory (P0-equivalent) requirement. "Should" denotes a strongly recommended (P1-equivalent) requirement. "May" denotes an optional, non-blocking enhancement.
- All monetary values are represented using fixed-precision decimal types — never floating point — throughout the stack.

---

# 2. System Overview

## 2.1 Product Summary

Paradox is a single-user personal expense-tracking web application implementing the core loop **Record → Organize → Review → Understand → Adjust** (PRD Section 8). Phase 1 delivers a complete, reliable, and simple experience for recording expenses, organizing them by category and payment method, reviewing spending history, understanding spending through summaries and a dashboard, and comparing spending against one overall monthly budget.

## 2.2 High-Level Architecture

Paradox uses a **decoupled frontend/backend architecture** communicating over a versioned REST API:

```
┌─────────────────────┐        HTTPS / REST / JSON        ┌──────────────────────┐
│   Next.js Frontend   │ ───────────────────────────────▶ │   FastAPI Backend     │
│   (App Router, TS)   │ ◀─────────────────────────────── │  (Router→Service→Repo)│
└─────────────────────┘        /api/v1/*                  └──────────┬───────────┘
                                                                       │ SQLAlchemy 2.0 (async)
                                                                       ▼
                                                             ┌──────────────────┐
                                                             │   PostgreSQL      │
                                                             │  (Alembic-managed)│
                                                             └──────────────────┘
```

- **Frontend**: Next.js (App Router) + TypeScript + Framer Motion + Zod (client-side schema validation), responsible for UI rendering, client-side validation, and calling the backend REST API.
- **Backend**: Python + FastAPI, responsible for business logic, validation, persistence, and API contracts, structured as **Router → Service → Repository → Database**.
- **Database**: PostgreSQL, accessed exclusively through SQLAlchemy 2.0 (async, via `asyncpg`), with schema evolution managed by Alembic.
- **Communication**: REST over JSON, versioned at the routing/contract layer as `/api/v1/...`.

## 2.3 System Boundaries

**In scope (Phase 1):**
- Web frontend (responsive; no native mobile app)
- Single backend service
- Single PostgreSQL database
- Single fixed currency, single user

**Out of scope (Phase 1):**
- Authentication/authorization enforcement (see Section 13 for the documented boundary)
- Multi-tenant data isolation
- External bank/financial integrations
- Notifications/reminders

## 2.4 Assumptions and Dependencies

- Local development does not require Docker; Docker is only used for backend deployment (Section 20).
- The application is used by exactly one real user for the duration of Phase 1; a `User` entity may exist in the schema for future compatibility but is not exposed through authentication flows in V1.
- One fixed currency is assumed application-wide; no currency field/conversion logic is required in V1.

---

# 3. Functional Requirements

Each functional requirement below implements one or more PRD `FR-xx` items (PRD Section 11). IDs are grouped by module.

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

## 4.1 UC-01: Add Expense

- **Actor**: The single user
- **Preconditions**: At least one category and one payment method exist (guaranteed by starter seed data).
- **Main Flow**:
  1. User opens the "Add Expense" form.
  2. User enters amount, selects category, selects payment method, selects/confirms date (defaults to today), optionally enters a description.
  3. Frontend validates input locally.
  4. Frontend calls `POST /api/v1/expenses`.
  5. Backend validates and persists the expense.
  6. Frontend displays success feedback and updates relevant views (list, dashboard, budget) without requiring a full page reload.
- **Alternate Flow**: Validation fails (client or server) → field-level error is shown; valid fields are preserved.
- **Postcondition**: A new expense exists and is reflected in all dependent aggregates (totals, category breakdown, budget, dashboard).

## 4.2 UC-02: View / Search / Filter / Sort Expenses

- **Actor**: The single user
- **Main Flow**:
  1. User navigates to the expense list.
  2. User optionally applies search text, date range, category, payment method, or amount filters, and a sort order.
  3. Frontend calls `GET /api/v1/expenses` with corresponding query parameters.
  4. Backend returns a paginated, filtered, sorted result set.
  5. Frontend renders the results, or an empty state if no results match.

## 4.3 UC-03: Edit Expense

- **Actor**: The single user
- **Main Flow**:
  1. User selects an existing expense.
  2. Form pre-fills with current values.
  3. User edits one or more fields.
  4. Frontend calls `PATCH /api/v1/expenses/{expense_id}`.
  5. Backend validates and persists changes; dependent aggregates recompute on next fetch.

## 4.4 UC-04: Delete Expense

- **Actor**: The single user
- **Main Flow**:
  1. User initiates delete on an expense.
  2. Frontend shows a confirmation prompt (destructive action).
  3. On confirmation, frontend calls `DELETE /api/v1/expenses/{expense_id}`.
  4. Backend removes the record; dependent aggregates recompute on next fetch.

## 4.5 UC-05: Manage Categories

- **Actor**: The single user
- **Main Flow**: Create category (unique name) → rename category → attempt delete (blocked for starter categories; for custom categories, referenced expenses are reassigned to `Uncategorized` per Section 10.3, then the category is removed).

## 4.6 UC-06: Manage Payment Methods

- Same shape as UC-05, applied to payment methods.

## 4.7 UC-07: Set / Update Monthly Budget

- **Actor**: The single user
- **Main Flow**:
  1. User navigates to Budget screen.
  2. User enters or edits the overall monthly budget amount.
  3. Frontend calls `PUT /api/v1/budget` (idempotent upsert of the singular budget resource).
  4. Backend persists the value; budget status becomes available on `GET /api/v1/budget` and the dashboard.

## 4.8 UC-08: View Dashboard

- **Actor**: The single user
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

### 6.2.1 `users` (present for future compatibility only — not used by any V1 auth flow)

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK, default `gen_random_uuid()` |
| display_name | VARCHAR(100) | NOT NULL, default `'Primary User'` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

> V1 seeds exactly one row in `users` at initialization and does not expose any user-management endpoint. No foreign key from `expenses`/`budgets` to `users` is enforced as a hard business requirement in V1 routing/business logic, but the column exists (nullable-safe default to the single seeded user) to avoid a breaking schema change in Phase 3.

### 6.2.2 `categories`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| name | VARCHAR(60) | NOT NULL, UNIQUE |
| is_default | BOOLEAN | NOT NULL, default `false` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

**Rules**: Rows with `is_default = true` (starter categories, including the reserved `Uncategorized` fallback) cannot be deleted at the service layer, regardless of any client request. Rename is permitted on all rows.

### 6.2.3 `payment_methods`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| name | VARCHAR(60) | NOT NULL, UNIQUE |
| is_default | BOOLEAN | NOT NULL, default `false` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

Same protection rules as `categories`.

### 6.2.4 `expenses`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| amount | NUMERIC(12,2) | NOT NULL, `CHECK (amount > 0)` |
| category_id | UUID | NOT NULL, FK → `categories.id` (`ON DELETE RESTRICT` at DB level; reassignment is handled by the service layer *before* deletion — see 10.3) |
| payment_method_id | UUID | NOT NULL, FK → `payment_methods.id` (`ON DELETE RESTRICT`, same reassignment pattern) |
| date | DATE | NOT NULL, application-level `CHECK`: not later than current date (enforced in service layer; DB-level check avoided since "current date" is not constant at migration time) |
| description | VARCHAR(255) | NULL |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

**Indexes**: `idx_expenses_date`, `idx_expenses_category_id`, `idx_expenses_payment_method_id`, composite `idx_expenses_date_category` to support common dashboard/summary queries.

### 6.2.5 `budgets`

Singular resource, modeled as a table to preserve auditability and forward compatibility, but exposed through the API as a single logical resource (`GET/PUT /api/v1/budget`).

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| amount | NUMERIC(12,2) | NOT NULL, `CHECK (amount >= 0)` |
| created_at | TIMESTAMPTZ | NOT NULL, default `now()` |
| updated_at | TIMESTAMPTZ | NOT NULL, default `now()` |

**Rule**: The service layer guarantees at most one row exists in V1 (upsert semantics on `PUT /api/v1/budget`). This avoids premature introduction of multiple/period-scoped budgets, per PRD Section 9.9 and Assumption in Section 18 ("One overall monthly budget is sufficient").

## 6.3 Entity Relationship Summary

```
categories (1) ──< (many) expenses
payment_methods (1) ──< (many) expenses
budgets: standalone singular resource, no FK to expenses (computed comparison happens in service layer)
users: standalone seed row, reserved for future FK relationships (Phase 3+)
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

## 7.1 Conventions

- Base path: `/api/v1`
- All request/response bodies are JSON.
- All monetary fields are serialized as strings with two decimal places (e.g., `"amount": "42.50"`) to avoid floating-point precision loss in transit; the frontend parses these as decimal-safe values.
- All dates are ISO-8601 (`YYYY-MM-DD`); all timestamps are ISO-8601 UTC with `Z` suffix.
- Resource identifiers are UUID strings.

## 7.2 Expenses

### `POST /api/v1/expenses`
- **Purpose**: Create a new expense.
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
- **Validation**: `amount > 0`; `date <= today`; `category_id` and `payment_method_id` must reference existing rows; `description` optional, max 255 chars.
- **Success**: `201 Created`, returns the created expense (`ExpenseRead`).
- **Errors**: `422` validation error; `404` if referenced category/payment method does not exist.

### `GET /api/v1/expenses`
- **Purpose**: List expenses with search, filter, sort, pagination.
- **Query parameters**:

| Param | Type | Notes |
|---|---|---|
| `search` | string | matches `description` (case-insensitive substring); always combinable with any filter/sort/pagination |
| `category_id` | UUID | single-dimension filter (V1) — mutually exclusive with `date_from`/`date_to` (Section 3.4.1) |
| `date_from` | date | inclusive; single-dimension filter (V1) — mutually exclusive with `category_id` |
| `date_to` | date | inclusive; single-dimension filter (V1) — mutually exclusive with `category_id` |
| `sort_by` | enum: `date`, `amount`, `category` | default `date` |
| `sort_order` | enum: `asc`, `desc` | default `desc` |
| `page` | int | default `1` |
| `page_size` | int | default `20`, max `100` |

> `payment_method_id`, `amount_min`, and `amount_max` are **not** V1 query parameters (see Section 3.4.1). Introducing them is a future-phase, non-MVP change.

- **Success**: `200 OK`, returns a paginated envelope (Section 8.3).
- **Errors**: `422` on invalid query parameters — e.g., `date_from > date_to`, or **both** `category_id` and a date-range parameter supplied in the same request (Section 3.4.1).

### `GET /api/v1/expenses/{expense_id}`
- **Purpose**: Retrieve a single expense.
- **Success**: `200 OK`.
- **Errors**: `404` if not found.

### `PATCH /api/v1/expenses/{expense_id}`
- **Purpose**: Partially update an expense.
- **Request body**: any subset of `amount`, `category_id`, `payment_method_id`, `date`, `description`.
- **Validation**: same rules as creation, applied to the resulting merged record.
- **Success**: `200 OK`, returns updated expense.
- **Errors**: `404` not found; `422` validation.

### `DELETE /api/v1/expenses/{expense_id}`
- **Purpose**: Delete an expense.
- **Success**: `204 No Content`.
- **Errors**: `404` not found.

## 7.3 Categories

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/categories` | Create custom category (`name`, unique, non-empty, ≤ 60 chars) |
| GET | `/api/v1/categories` | List all categories (starter + custom), includes `is_default` |
| GET | `/api/v1/categories/{category_id}` | Retrieve one category |
| PATCH | `/api/v1/categories/{category_id}` | Rename a category (starter or custom) |
| DELETE | `/api/v1/categories/{category_id}` | Delete a custom category; `409 Conflict` if `is_default = true` |

Deleting a referenced custom category triggers the reassignment rule in Section 6.4 and returns `204 No Content` on success.

## 7.4 Payment Methods

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/payment-methods` | Create custom payment method |
| GET | `/api/v1/payment-methods` | List all payment methods |
| PATCH | `/api/v1/payment-methods/{payment_method_id}` | Rename |
| DELETE | `/api/v1/payment-methods/{payment_method_id}` | Delete (same protection/reassignment rules as categories) |

> Note: per the finalized prompt, no single-resource `GET /{id}` was specified for payment methods; `GET /api/v1/payment-methods` (list) is sufficient for V1 and is retained as specified.

## 7.5 Budget

### `GET /api/v1/budget`
- Returns the current singular budget resource: `{ "amount": "2000.00", "updated_at": "..." }`. If no budget has ever been set, returns `200 OK` with `amount: null` (documented empty state, not a `404`, since budget is a singular always-addressable resource).

### `PUT /api/v1/budget`
- **Request body**: `{ "amount": "2000.00" }`
- **Validation**: `amount >= 0`.
- **Behavior**: Upsert — creates the budget row if none exists, otherwise updates the existing one.
- **Success**: `200 OK`, returns the updated budget resource.

## 7.6 Dashboard

### `GET /api/v1/dashboard`
- **Purpose**: Single aggregated read model for the dashboard screen.
- **Query parameters**: `period` (optional, enum: `current_month` default, `last_30_days`, `current_week`) — scopes totals/trend/top-categories; recent expenses are always the most recent N regardless of `period`.
- **Response body**:
```json
{
  "period": "current_month",
  "total_spent": "845.00",
  "budget": {
    "amount": "2000.00",
    "spent": "845.00",
    "remaining": "1155.00",
    "status": "under_budget"
  },
  "category_breakdown": [
    { "category_id": "uuid", "category_name": "Food", "total": "320.00", "percentage": 37.9 }
  ],
  "top_categories": [
    { "category_id": "uuid", "category_name": "Food", "total": "320.00" }
  ],
  "trend": [
    { "label": "Week 1", "total": "210.00" }
  ],
  "recent_expenses": [ { "...": "ExpenseRead objects, limited to 5" } ]
}
```
- **Success**: `200 OK`. Empty-safe: with zero expenses, all totals are `"0.00"`, arrays are empty, and `budget.status` is omitted or `null` if no budget is set — never fabricated data (FR-23).

## 7.7 System

### `GET /api/v1/health`
- **Purpose**: Liveness/readiness check for deployment platforms (Render) and monitoring.
- **Response**: `{ "status": "ok", "database": "connected" }` with `200 OK`, or `503 Service Unavailable` with `"database": "unreachable"` if the DB connection check fails.

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
| 400 | Malformed request (rare; prefer 422) |
| 404 | Resource not found |
| 409 | Conflict (e.g., duplicate category name, deleting a protected starter entity) |
| 422 | Validation error (schema or business rule) |
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
- Numeric/monetary inputs must reject non-numeric characters and enforce a positive-value constraint client-side.
- Date pickers must disallow selecting a future date at the UI level.
- Field-level error messages render adjacent to the relevant input.
- On validation failure, previously entered valid values are preserved (no form reset).
- Submit controls show a loading/disabled state while a request is in flight, to prevent duplicate submissions.

## 9.3 Backend Validation

- All request bodies are validated via Pydantic schemas (`app/schemas/*`) before reaching the service layer.
- Business rules not expressible purely in schema terms (e.g., "category must exist", "date not in the future" relative to server clock) are enforced in the service layer.
- Database-level constraints (`CHECK`, `NOT NULL`, `UNIQUE`, foreign keys) provide a final integrity backstop independent of application code correctness.

## 9.4 Field-Level Rules Summary

| Field | Rule |
|---|---|
| `expense.amount` | Required; decimal > 0; max 2 decimal places; reasonable upper bound (e.g., < 10,000,000) to catch obvious data-entry errors |
| `expense.date` | Required; valid ISO date; must not be after current server date |
| `expense.category_id` | Required; must reference an existing category |
| `expense.payment_method_id` | Required; must reference an existing payment method |
| `expense.description` | Optional; max 255 characters |
| `category.name` / `payment_method.name` | Required; non-empty after trim; max 60 characters; unique (case-insensitive) |
| `budget.amount` | Required; decimal ≥ 0; max 2 decimal places |

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
| `app/main.py` | FastAPI app instantiation, middleware registration, router inclusion |
| `app/api/router.py` | Aggregates all resource routers under `/api/v1` |
| `app/api/deps.py` | Shared dependencies (DB session provider, pagination params, etc.) |
| `app/core/config.py` | Environment-driven settings (Pydantic `BaseSettings`) |
| `app/core/exceptions.py` | Domain exception classes (e.g., `NotFoundError`, `ValidationError`, `ConflictError`) |
| `app/core/error_handlers.py` | FastAPI exception handlers translating domain exceptions to the standard error envelope (Section 8.4) |
| `app/core/logging.py` | Structured logging configuration |
| `app/core/security.py` | Reserved for future auth-related utilities (not exercised by V1 endpoints) |
| `app/db/session.py` | Async SQLAlchemy engine/session factory |
| `app/db/base.py` | Declarative base and model registry for Alembic autogeneration |
| `app/db/models/*` | SQLAlchemy ORM entities |
| `app/schemas/*` | Pydantic request/response models per resource |
| `app/services/*` | Business logic per resource |
| `app/repositories/*` | Database access per resource |
| `app/utils/pagination.py` | Shared pagination helper logic |
| `app/utils/datetime.py` | Date/time normalization helpers |
| `app/utils/money.py` | Decimal-safe monetary helpers (parsing, formatting, rounding) |
| `app/constants/categories.py` | Starter category seed definitions, referenced by the seed migration |
| `app/constants/payment_methods.py` | Starter payment method seed definitions |

## 12.3 Backend Behavioral Requirements

- All I/O (database calls) is asynchronous (`asyncpg` + SQLAlchemy async session).
- Every service method that mutates data must be wrapped in a single database transaction; partial writes must not be possible (relevant especially to Section 10.3's reassign-then-delete operation).
- Services return domain-level results or raise domain exceptions; routers translate these into HTTP responses via the standard envelopes.
- Pagination defaults (`page=1`, `page_size=20`, `max page_size=100`) are enforced centrally in `utils/pagination.py`, not duplicated per router.

---

# 13. Authentication and Authorization

## 13.1 V1 Boundary (Explicit)

- Paradox V1 is designed and implemented for **exactly one real user**, per PRD Phase 1 scope.
- **No authentication/authorization enforcement is implemented in V1.** There is no login flow, no session/token issuance, and no per-request identity check on any endpoint.
- The API is not intended for public/multi-tenant exposure in this phase; access control at the network/deployment level (e.g., environment restriction, not sharing the deployed URL) is the operator's responsibility, not an application feature of V1.

## 13.2 Why a `users` Table Exists Despite No Auth

A `users` table (Section 6.2.1) is included purely as a **forward-compatible schema placeholder** so that Phase 3 ("Multi-User Product Foundation") does not require a breaking schema migration. It is seeded with exactly one row and is not referenced by any V1 endpoint's authorization logic — this satisfies the finalized rule: *"Do not add authentication/multi-user implementation to V1 merely because a `User` entity may exist for future compatibility."*

## 13.3 Future Phase Boundary (Non-MVP, documented for continuity only)

Phase 3 of the PRD will require: user registration/login, session or token-based authentication (e.g., JWT), per-request identity resolution, and row-level data isolation (e.g., a `user_id` foreign key enforced on `expenses`, `budgets`). None of this is implemented, stubbed, or partially wired in V1. See Section 26.

---

# 14. Security Requirements

Even though V1 has no authentication layer, the following baseline security requirements apply:

| ID | Requirement |
|---|---|
| SRS-SEC-01 | No secrets (database credentials, API keys) may be committed to version control. `.env` files are gitignored; only `.env.example` (placeholder values) is committed. |
| SRS-SEC-02 | All configuration is sourced from environment variables via `app/core/config.py`, never hardcoded. |
| SRS-SEC-03 | All request bodies are validated server-side (Section 9.3) regardless of frontend validation state. |
| SRS-SEC-04 | Database access is limited to the application's own service account/connection string; no direct external database exposure beyond what the deployment platform (Supabase) requires. |
| SRS-SEC-05 | Error responses returned to clients must not leak stack traces, SQL fragments, internal file paths, or other implementation details (Section 15.4). |
| SRS-SEC-06 | CORS must be explicitly configured to allow only the known frontend origin(s) (local dev URL, deployed Vercel URL), not a wildcard, in non-development environments. |
| SRS-SEC-07 | Financial data is treated as private; no endpoint exposes another entity's data since V1 is single-user, but the reassignment/deletion logic (Section 10.3) must never silently discard financial history — only reassign category/payment-method association. |
| SRS-SEC-08 | Deployment configuration (Render, Vercel, Supabase) must use platform-managed secret storage rather than plaintext configuration files. |
| SRS-SEC-09 | Dependency versions are pinned (`requirements.txt` / `pyproject.toml`, `package.json`) to avoid unreviewed transitive upgrades. |

## 14.1 Baseline Threat-Category Protections

The following clarifies V1's baseline posture per specific threat category. None of this introduces enterprise-grade security infrastructure — it is the minimum reasonable protection for a web application handling private financial data, appropriate to the single-user MVP scope.

| Threat Category | V1 Protection |
|---|---|
| **CORS** | `CORS_ALLOWED_ORIGINS` (Section 19.1) explicitly whitelists the known frontend origin(s) only — local dev URL and the deployed Vercel URL. No wildcard (`*`) origin in any non-local environment (restates SRS-SEC-06). |
| **XSS (Cross-Site Scripting)** | The frontend renders all user-supplied text (expense descriptions, category/payment-method names) through React's default escaping — no `dangerouslySetInnerHTML` or equivalent raw-HTML injection is used anywhere in the frontend for user-supplied content. Backend responses are always `application/json`, never rendered as HTML, eliminating reflected-XSS vectors through the API. |
| **SQL Injection** | All database access goes through SQLAlchemy 2.0's parameterized query construction (ORM queries / `select()` constructs) as required by Section 6.1 and Section 12.1 — no raw string-interpolated SQL is used anywhere in `repositories/`. |
| **CSRF (Cross-Site Request Forgery)** | V1 has no authenticated session/cookie-based identity (Section 13.1), so classic cookie-based CSRF does not apply to its mutating endpoints. As a baseline precaution appropriate to this architecture, the API only accepts state-changing requests with `Content-Type: application/json` (not form-encoded), and CORS (above) restricts which origins' browser-based requests are permitted to reach the API at all. If Phase 3 introduces cookie/session-based authentication, CSRF tokens must be added at that time (Section 26) — this is explicitly a future-phase concern, not a V1 gap. |
| **Secure Headers** | The backend sets baseline secure HTTP response headers (e.g., `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` or an equivalent `Content-Security-Policy frame-ancestors` directive, `Referrer-Policy: strict-origin-when-cross-origin`). HSTS (`Strict-Transport-Security`) is enabled in production, where TLS is guaranteed by the hosting platforms (Vercel/Render both terminate TLS by default). |
| **Secret Management** | Restates and consolidates SRS-SEC-01, SRS-SEC-02, SRS-SEC-08: secrets live only in environment variables, sourced from platform-managed secret storage (Vercel/Render/Supabase project settings) in deployed environments and from a local, gitignored `.env`/`.env.local` in development; `.env.example` documents required variable names with placeholder values only (see Section 19.3 for the full file-level policy). |

No enterprise-grade security architecture (WAF, rate limiting infrastructure, SOC2-oriented controls, secrets rotation automation) is required in V1, per the finalized decision to avoid unjustified complexity.

---

# 15. Error Handling

## 15.1 Error Handling Philosophy

Errors must be predictable, consistently shaped (Section 8.4), and understandable — both to a developer integrating against the API and to the non-technical end user viewing the frontend's rendering of that error (FR-24).

## 15.2 Domain Exception Taxonomy (`app/core/exceptions.py`)

| Exception | HTTP Status | `error.code` |
|---|---|---|
| `NotFoundError` | 404 | `NOT_FOUND` |
| `ValidationError` (business-rule level) | 422 | `VALIDATION_ERROR` |
| `ConflictError` (e.g., duplicate name, protected entity deletion) | 409 | `CONFLICT` |
| `UnprocessableRequestError` (query-parameter contradictions, e.g. `date_from > date_to`) | 422 | `INVALID_REQUEST` |
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

| Category | Example Variables | Purpose |
|---|---|---|
| Database connection | `DATABASE_URL` | asyncpg-compatible PostgreSQL connection string |
| Application environment | `APP_ENV` (`local`, `staging`, `production`), `DEBUG` | Behavior toggles (e.g., verbose error detail only outside production) |
| API configuration | `API_V1_PREFIX`, `APP_NAME` | Routing/metadata configuration |
| CORS | `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins |
| Logging | `LOG_LEVEL` | Logging verbosity |
| Timezone | `APP_TIMEZONE` | Server-side "current date"/"current month" semantics (Section 10.6), default `UTC` |

## 19.2 Frontend Environment Variables

| Category | Example Variables | Purpose |
|---|---|---|
| Backend/API base URL | `NEXT_PUBLIC_API_BASE_URL` | Base URL the frontend API client targets |
| Environment configuration | `NEXT_PUBLIC_APP_ENV` | Non-sensitive environment flag for client-side behavior |

## 19.3 File Distinctions

| File | Purpose | Committed? |
|---|---|---|
| `.env.example` | Documents all required variables with placeholder/dummy values | Yes |
| `.env` (backend) | Actual local backend configuration | No (gitignored) |
| `.env.local` (frontend) | Actual local frontend configuration (Next.js convention) | No (gitignored) |

**`.env`, `.env.local`, and any other local environment file are local configuration examples/instances only — they exist purely so a developer's own machine can run the application, and must never contain committed secrets.** Concretely:

- `.env` (backend) and `.env.local` (frontend) are gitignored in `backend/.gitignore` and `frontend/.gitignore` respectively (Section 23.3) and must never be committed, staged, or pushed under any circumstance.
- `.env.example` is the **only** environment file committed to the repository, and it must contain variable **names** with placeholder/dummy values only (e.g., `DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/paradox`) — never a real credential, key, or connection string.
- Real secrets are never placed in the repository at any layer, in any branch, or in commit history; production/staging secrets are configured directly and exclusively in the Vercel/Render/Supabase platform dashboards (Section 20, SRS-SEC-08).
- If a secret is ever accidentally committed, it must be treated as compromised and rotated — not merely removed from a future commit — though the operational process for rotation is outside this SRS's scope.

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

- **Frontend (Vercel)**: builds and deploys directly from the `frontend/` directory using Next.js's standard build (`next build`); environment variables configured in the Vercel project settings, including `NEXT_PUBLIC_API_BASE_URL` pointing at the deployed Render backend URL.
- **Backend (Render)**: built and deployed via `backend/Dockerfile`, producing a container that runs the FastAPI app with an ASGI server (e.g., `uvicorn`/`gunicorn+uvicorn workers`); environment variables (Section 19.1) configured in the Render service settings, including `DATABASE_URL` pointing at Supabase.
- **Database (Supabase)**: managed PostgreSQL; Alembic migrations are run against the Supabase connection string as part of the deployment process (pre-deploy step or manual/CI-triggered migration run — the exact automation mechanism is an implementation detail left to the build pipeline, but migrations must run before the new backend version serves traffic).

## 20.3 Docker Responsibilities

- `frontend/Dockerfile` exists for completeness/portability but is **not** the deployment path used by Vercel (Vercel builds natively); it must still build a valid production image if used for alternative hosting.
- `backend/Dockerfile` is the **required** deployment artifact for Render and must produce a production-ready image (installing dependencies from `requirements.txt`/`pyproject.toml`, copying the `app/` source, exposing the correct port, running the ASGI server as the entrypoint).
- Frontend and backend Docker images are built and deployed independently; neither depends on the other's Dockerfile.

## 20.4 Build/Start Expectations Summary

| Component | Local | Deployed |
|---|---|---|
| Frontend | `next dev` | Vercel-managed build (`next build`) |
| Backend | `uvicorn app.main:app --reload` | Docker image → Render-managed process |
| Database | Local PostgreSQL | Supabase-managed PostgreSQL |
| Migrations | `alembic upgrade head` (manual, local) | `alembic upgrade head` run as part of the deploy step, before new backend traffic is served |

## 20.5 Production Considerations (Phase-appropriate)

- The health endpoint (`/api/v1/health`, Section 7.7) is used by Render's health-check mechanism to determine service readiness.
- CORS is restricted to the known Vercel deployment URL (and local dev URL in non-production) per Section 14 (SRS-SEC-06).
- No auto-scaling, multi-region, or blue/green deployment strategy is required for V1 — a single Render service instance is sufficient for single-user validation, consistent with avoiding premature scaling (PRD Section 18 risk: "Premature scaling").

## 20.6 CI/CD

A single, simple CI/CD pipeline is sufficient for V1 — no multi-stage release trains, canary deploys, or feature-flag infrastructure are introduced, consistent with avoiding premature operational complexity.

### 20.6.1 Pull-Request Checks (required to merge)

On every pull request targeting the main branch, CI must run and pass:

| Check | Scope |
|---|---|
| Frontend lint | ESLint against `frontend/` |
| Frontend type-check | `tsc --noEmit` against `frontend/` |
| Frontend tests | Component/unit tests (Section 17.1) against `frontend/` |
| Frontend build | `next build` against `frontend/`, to catch build-breaking errors before merge |
| Backend lint | A configured Python linter/formatter check (e.g., `ruff`/`flake8`) against `backend/` |
| Backend tests | `pytest` (unit, integration, API layers — Section 17.1) against `backend/`, run against a disposable/CI-provisioned PostgreSQL instance with Alembic migrations applied |

A pull request must not be merged if any of the above checks fail. Checks run independently for `frontend/` and `backend/` so a change to one does not require rebuilding/retesting the other unnecessarily.

### 20.6.2 Production Deployment Flow

- Merging to the main branch triggers deployment:
  - **Frontend**: Vercel's native Git integration builds and deploys `frontend/` automatically on merge (no separate CI step needed to trigger this — Vercel handles it).
  - **Backend**: On merge, the CI/CD pipeline (or Render's native Git integration) builds `backend/Dockerfile` and deploys the resulting image to Render.
- Database migrations (`alembic upgrade head`) are run against the Supabase connection as a pre-deploy or deploy-time step, **before** the new backend version begins serving traffic, consistent with Section 20.2/20.4.
- Deployment is considered successful only once the newly deployed backend responds healthy on `/api/v1/health` (Section 7.7).
- No manual production deployment step is required beyond merging an approved, passing pull request — but rollback (redeploying the previous Render image / previous Vercel deployment) remains a manual, developer-triggered action in V1; no automated rollback is implemented.

---

# 21. API ↔ Database ↔ UI Traceability

| API Endpoint | Database Entities Touched | Primary UI Surface |
|---|---|---|
| `POST/GET/PATCH/DELETE /api/v1/expenses[/…]` | `expenses`, reads `categories`/`payment_methods` for validation | Expense List, Add Expense, Expense Detail/Edit |
| `POST/GET/PATCH/DELETE /api/v1/categories[/…]` | `categories`, cascading update to `expenses.category_id` on protected deletion | Categories screen; category selector in Expense form |
| `POST/GET/PATCH/DELETE /api/v1/payment-methods[/…]` | `payment_methods`, cascading update to `expenses.payment_method_id` on protected deletion | Categories/Payment Methods screen; payment method selector in Expense form |
| `GET/PUT /api/v1/budget` | `budgets` | Budget screen; Dashboard budget card |
| `GET /api/v1/dashboard` | Reads `expenses`, `categories`, `budgets` (aggregate/read-only) | Dashboard screen |
| `GET /api/v1/health` | Connectivity check only, no domain tables | N/A (operational) |

---

# 22. Requirement Traceability Matrix

| PRD ID | PRD Requirement | SRS Requirement(s) | API Area | DB Entity | Test Coverage Area |
|---|---|---|---|---|---|
| FR-01 | Create expense | SRS-FN-01 | `POST /expenses` | `expenses` | API tests, unit (validation) |
| FR-02 | Prevent invalid expenses | SRS-FN-02, SRS-FN-03 | `POST/PATCH /expenses` | `expenses` | Unit + API tests (Section 17.2) |
| FR-03 | View expense history | SRS-FN-04 | `GET /expenses`, `GET /expenses/{id}` | `expenses` | API tests |
| FR-04 | Edit expense | SRS-FN-05 | `PATCH /expenses/{id}` | `expenses` | API tests |
| FR-05 | Delete expense with confirmation | SRS-FN-06 | `DELETE /expenses/{id}` | `expenses` | API tests, E2E, frontend confirmation dialog test |
| FR-06 | Meaningful categories; starter categories | SRS-FN-07, SRS-FN-09 | `POST/GET /categories` | `categories` | Seed migration test, API tests |
| FR-07 | Create/rename custom categories | SRS-FN-10, SRS-FN-11 | `POST/PATCH /categories` | `categories` | API tests |
| FR-08 | Category removal integrity | SRS-FN-12, SRS-FN-13, Section 10.3 | `DELETE /categories/{id}` | `categories`, `expenses` | Integration test (reassignment transaction) |
| FR-09 | Payment methods | SRS-FN-14, SRS-FN-15 | `/payment-methods` | `payment_methods` | API tests |
| FR-10 | Review by date range | SRS-FN-16 | `GET /expenses?date_from&date_to` | `expenses` | API tests |
| FR-11 | Search history | SRS-FN-17 | `GET /expenses?search` | `expenses` | API tests |
| FR-12 | Filter by date/category/amount/payment method | SRS-FN-18 (narrowed to date-OR-category, single dimension, for V1 — Section 3.4.1) | `GET /expenses` | `expenses` | API tests (incl. mutual-exclusion `422` case) |
| FR-13 | Sort by date/amount/category | SRS-FN-19 | `GET /expenses?sort_by&sort_order` | `expenses` | API tests |
| FR-14 | Accurate totals | SRS-FN-20, Section 10.1 | `GET /dashboard` | `expenses` | Unit test (Decimal fidelity) |
| FR-15 | Category-based spending | SRS-FN-21 | `GET /dashboard` | `expenses`, `categories` | Unit + API tests |
| FR-16 | Spending trends | SRS-FN-22 | `GET /dashboard?period` | `expenses` | Unit + API tests |
| FR-17 | Top spending categories | SRS-FN-23 | `GET /dashboard` | `expenses`, `categories` | Unit + API tests |
| FR-18 | One overall monthly budget | SRS-FN-24 | `GET/PUT /budget` | `budgets` | API tests |
| FR-19 | Compare spending vs budget | SRS-FN-25 | `GET /budget`, `GET /dashboard` | `budgets`, `expenses` | Unit test (Section 10.2 thresholds) |
| FR-20 | Budget amount/spent/remaining/status | SRS-FN-26, Section 10.2 | `GET /budget`, `GET /dashboard` | `budgets` | Unit + API tests |
| FR-21 | Dashboard | SRS-FN-27 | `GET /dashboard` | all read entities | API + E2E |
| FR-22 | Records persist across sessions | SRS-FN-08 | all mutating endpoints | all | Reliability/E2E test |
| FR-23 | No fabricated financial data | SRS-FN-28, Section 7.6 empty-safe rule | `GET /dashboard`, `GET /budget` | all | API test (zero-data scenario) |
| FR-24 | Understandable empty/validation/loading/error states | SRS-FN-29, Section 11.1, Section 15.3 | all | N/A | Frontend component tests |

---

# 23. Project Structure

## 23.1 Backend Structure (authoritative)

```text
backend/
├── app/
│   ├── main.py
│   ├── api/
│   │   ├── deps.py
│   │   ├── router.py
│   │   ├── health.py
│   │   ├── expenses.py
│   │   ├── categories.py
│   │   ├── payment_methods.py
│   │   ├── budget.py
│   │   └── dashboard.py
│   ├── core/
│   │   ├── config.py
│   │   ├── exceptions.py
│   │   ├── error_handlers.py
│   │   ├── logging.py
│   │   └── security.py
│   ├── db/
│   │   ├── session.py
│   │   ├── base.py
│   │   └── models/
│   │       ├── expense.py
│   │       ├── category.py
│   │       ├── payment_method.py
│   │       ├── budget.py
│   │       └── user.py
│   ├── schemas/
│   │   ├── common.py
│   │   ├── expense.py
│   │   ├── category.py
│   │   ├── payment_method.py
│   │   ├── budget.py
│   │   ├── dashboard.py
│   │   └── user.py
│   ├── services/
│   │   ├── expense_service.py
│   │   ├── category_service.py
│   │   ├── payment_method_service.py
│   │   ├── budget_service.py
│   │   └── dashboard_service.py
│   ├── repositories/
│   │   ├── expense_repository.py
│   │   ├── category_repository.py
│   │   ├── payment_method_repository.py
│   │   ├── budget_repository.py
│   │   └── user_repository.py
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
│   ├── integration/
│   └── api/
├── Dockerfile
├── .gitignore
├── .env
├── .env.example
├── alembic.ini
├── requirements.txt
├── pyproject.toml
└── README.md
```

> Note: exactly one `backend/.gitignore` exists (do not duplicate).

## 23.2 Frontend Structure (authoritative)

```text
frontend/
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   ├── dashboard/
│   │   │   └── page.tsx
│   │   ├── expenses/
│   │   │   ├── page.tsx
│   │   │   ├── new/
│   │   │   │   └── page.tsx
│   │   │   └── [id]/
│   │   │       └── page.tsx
│   │   ├── categories/
│   │   │   └── page.tsx
│   │   └── budget/
│   │       └── page.tsx
│   ├── features/
│   │   ├── expenses/
│   │   │   ├── components/
│   │   │   ├── hooks/
│   │   │   ├── services/
│   │   │   ├── schemas/
│   │   │   └── types.ts
│   │   ├── categories/
│   │   ├── payment-methods/
│   │   ├── budget/
│   │   └── dashboard/
│   ├── components/
│   │   ├── ui/
│   │   ├── layout/
│   │   └── common/
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts
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
├── Dockerfile
├── .gitignore
├── .env.local
├── .env.example
├── package.json
├── next.config.ts
├── tsconfig.json
└── README.md
```

## 23.3 Repository-Root Structure

```text
paradox/
├── frontend/
├── backend/
├── .gitignore                (root: OS/editor/general artifacts)
└── README.md
```

Three `.gitignore` files total: root, `frontend/.gitignore` (Next.js/Node artifacts, `.env*` except `.env.example`), and `backend/.gitignore` (Python/venv/cache artifacts, `.env*` except `.env.example`) — no unnecessary duplication of rules across files.

---

# 24. API Documentation Requirements

- FastAPI's built-in OpenAPI schema generation must be enabled and accurate: every router must declare response models (Pydantic schemas), status codes, and parameter types so the auto-generated schema is a faithful contract.
- Interactive documentation (Swagger UI at `/docs`, ReDoc at `/redoc`) must be reachable in local and staging environments; may be disabled or access-restricted in production per standard FastAPI configuration, at the team's discretion, since V1 has no auth layer to protect it otherwise.
- Every endpoint in Section 7 must have a docstring/`summary`/`description` in the FastAPI route decorator matching the purpose stated in this SRS.
- Example request/response bodies shown in this SRS (Section 7) should be reflected as OpenAPI examples where FastAPI/Pydantic supports it, to keep generated documentation and this SRS in sync.

---

# 25. Data Backup and Recovery

Scope is intentionally minimal for a single-user validation phase, consistent with avoiding premature operational complexity:

- **Backup**: Rely on Supabase's managed PostgreSQL backup capabilities (platform-provided automatic backups/point-in-time recovery as available on the selected Supabase plan). No custom backup tooling is built for V1.
- **Recovery**: In the event of data loss, recovery is performed via Supabase's restore mechanism; there is no custom disaster-recovery automation in Phase 1.
- **Migration safety**: All schema changes go through Alembic migrations, which are reversible where practical (`downgrade()` implemented for each migration) to allow rollback of a bad deployment.
- **Data export**: Not a V1 requirement (PRD Section 13: "Advanced export/reporting" is out of scope); may be considered in a later phase.

This is deliberately lightweight because Paradox V1 serves one user and has not yet been validated as worth production-grade backup infrastructure — escalate this section's scope in Phase 5 ("Production-Ready Product").

---

# 26. Future Compatibility Requirements

The following are **documented for architectural continuity only** and are **explicitly not implemented in V1**. They exist so that Phase 1 code does not require a disruptive rewrite when later phases begin, per the PRD roadmap (Section 14) and the finalized instruction not to implement future complexity prematurely.

| Future Phase | Compatibility Consideration | Why it's safe to defer |
|---|---|---|
| Phase 2 (Refinement) | No structural changes anticipated; the layered architecture and feature-organized frontend are already positioned to absorb UX refinements without rearchitecting. | Architecture already supports iterative UI/UX change |
| Phase 3 (Multi-User Foundation) | `users` table already exists (Section 6.2.1); UUID primary keys throughout avoid awkward key migrations; adding `user_id` foreign keys to `expenses`/`budgets`/`categories`/`payment_methods` and an auth middleware layer (`app/core/security.py` is reserved) is additive, not a rewrite. | Schema and layering were chosen specifically to avoid an obvious rewrite, without adding auth logic now |
| Phase 2 (Refinement) — filter expansion | Combined multi-dimension filtering (date + category + payment method + amount together) and reintroducing `payment_method_id`/`amount_min`/`amount_max` as query parameters on `GET /expenses` are candidates once real usage shows single-dimension filtering (Section 3.4.1) is insufficient. | The `expense_repository` query-building logic is isolated enough to extend without changing the endpoint's contract style |
| Phase 4 (Advanced Financial Understanding) | The `dashboard_service`/`expense_repository` separation allows new aggregate queries (recurring expenses, deeper trend analysis) to be added as new service methods without touching the API contract style established in Section 7. | Aggregation logic is isolated in the service layer |
| Phase 5 (Production-Ready) | Logging (Section 18), health checks (Section 7.7), and environment separation (Section 19) are already in place as the minimal scaffolding a production hardening pass would extend. | Baseline operational hooks exist without full production tooling |
| Phase 6 (Long-Term Evolution) | REST/JSON API versioned at `/api/v1` leaves room for a future `/api/v2` without disturbing V1 consumers if a breaking model change is ever needed. | Versioning strategy was chosen precisely for this purpose |

No code, schema column, or endpoint beyond what is listed above may be added "just in case." Each future item requires its own PRD-level validation before becoming an SRS requirement, per PRD Principle 3 ("Validation before expansion") and Principle 6 ("Complexity must be earned").

---

# 27. Appendices

## 27.1 Glossary

See Section 1.3.

## 27.2 Starter Category Seed List

Seeded via Alembic data migration; `is_default = true` for all entries below (non-deletable, renamable):

1. Food & Dining
2. Transportation
3. Shopping
4. Entertainment
5. Bills & Utilities
6. Health
7. Education
8. Groceries
9. Uncategorized *(reserved fallback for category-deletion reassignment, Section 10.3 — must remain first-class and non-deletable)*

## 27.3 Starter Payment Method Seed List

Seeded via Alembic data migration; `is_default = true` for all entries below:

1. Cash
2. Debit Card
3. Credit Card
4. Bank Transfer
5. Digital Wallet
6. Other *(reserved fallback for payment-method-deletion reassignment, Section 10.3 — must remain first-class and non-deletable)*

## 27.4 Budget Status Values

| Value | Meaning |
|---|---|
| `under_budget` | Spending is below 90% of the configured monthly budget |
| `near_limit` | Spending is between 90% and 100% (inclusive) of the configured monthly budget |
| `over_budget` | Spending exceeds 100% of the configured monthly budget |
| `null` / omitted | No budget has been configured yet |

## 27.5 Standard Error Codes

| `error.code` | HTTP Status | Meaning |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Schema or business-rule validation failure |
| `INVALID_REQUEST` | 422 | Contradictory or malformed query parameters |
| `NOT_FOUND` | 404 | Requested resource does not exist |
| `CONFLICT` | 409 | Duplicate name, or attempted deletion of a protected (`is_default`) entity |
| `INTERNAL_ERROR` | 500 | Unhandled server-side failure |

## 27.6 Definition of Done (Phase 1)

A feature or the overall Phase 1 release is considered done when:

- [ ] All applicable functional requirements in Section 3 are implemented and pass their traced tests (Section 22).
- [ ] Frontend and backend validation are both implemented and consistent (Section 9).
- [ ] API behavior matches Section 7 exactly, including status codes and error envelope shape.
- [ ] Alembic migrations exist for every schema change, including the starter category/payment-method seed data (Section 27.2–27.3), and both `upgrade()`/`downgrade()` are implemented.
- [ ] Unit, integration, and API tests pass, with priority coverage (Section 17.2) green.
- [ ] Critical financial calculations (totals, budget status, category percentages) are verified against known test data.
- [ ] Loading, empty, validation, and error states are implemented and visually verified for every primary screen (Section 11.2).
- [ ] Frontend successfully integrates against the deployed/local backend end-to-end for the core loop (Add → Review → Understand → Adjust).
- [ ] All required environment variables are documented in `.env.example` for both frontend and backend.
- [ ] `backend/Dockerfile` builds successfully and runs the FastAPI app correctly in a container.
- [ ] No secrets are present anywhere in the committed repository.
- [ ] This SRS and its traceability matrix (Section 22) are updated if any implementation deviation was necessary, with the deviation and reason documented.

---

## Document Status

**Version 1.1 — Final, Implementation-Ready SRS for Paradox Phase 1 (Single-User Core MVP).**

This document is the technical source of truth for **how** Paradox is built. Product scope, priorities, and rationale remain governed by `PARADOX_PRD_FINAL.md`. Any conflict between this SRS and the PRD on product scope must be resolved in favor of the PRD; any conflict on technical implementation detail must be resolved in favor of this SRS, and reconciled with the original `PARADOX_SRS_CLAUDE_PROMPT.md` decisions where applicable.

Version 1.1 incorporates a targeted set of modification instructions (see Revision History above) as in-place amendments to v1.0. All previously finalized architecture, project structure, API surface, testing strategy, deployment topology, Docker decisions, and `.gitignore` structure from v1.0 remain unchanged except where this revision explicitly narrows the V1 expense-filtering contract (Section 3.4.1, Section 7.2). No future-phase feature was introduced as a V1 requirement in this revision.
