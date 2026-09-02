# Technical Debt Analysis: Paradox Project

This document details technical debt items, architectural tradeoffs, short-term shortcuts taken during implementation, and recommendations for future phases.

---

## 1. Authentication & Multi-Tenant Isolation (RESOLVED in Phase 3)
* **Status**: **RESOLVED** in Phase 3.
* **Resolution**:
  - Implemented secure JWT access token authentication (`HS256`, 15 min expiry) stored in memory.
  - Implemented secure long-lived refresh tokens (7-30 days) stored in `HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth` cookies.
  - Implemented refresh token rotation with single-use SHA-256 database hashing.
  - Implemented multi-device session revocation (`/logout-all`).
  - Implemented Google OAuth 2.0 / OpenID Connect ID token signature verification.
  - Enforced row-level multi-tenant isolation across all entity repositories and domain services (`expenses`, `budgets`, `categories`, `payment_methods`, `dashboard`).

---

## 2. Production Email Dispatch Provider (RESOLVED via Multi-Provider Engine)
* **Status**: **RESOLVED**.
* **Resolution**:
  - Implemented `EmailService` ([`backend/app/services/email_service.py`](file:///e:/Projects/Paradox/backend/app/services/email_service.py)) with flexible multi-provider dispatch: Brevo REST API (`BREVO_API_KEY`), Resend REST API (`RESEND_API_KEY`), and standard SMTP (`SMTP_*`).
  - Brevo enables production transactional delivery from verified Gmail accounts without requiring custom domains.
  - Supports Gmail App Passwords with auto-sanitization of spaces, STARTTLS (port 587), and SSL (port 465).
  - Formats branded responsive HTML templates matching the Paradox dark design system, with plain-text fallback.
  - Asynchronously dispatches pre-registration verification emails and password reset emails without blocking the event loop.

---

## 3. API Data Synchronization (Polling vs WebSockets)
* **Debt / Tradeoff**: TanStack Query is configured with a `staleTime` of 30 seconds for caching. It relies on standard refetch events (tab refocus, page reload, cache expiry) to synchronize state.
* **Impact**: If multiple clients or browser tabs write to the database simultaneously, updates will be delayed up to 30 seconds.
* **Future Work**: 
  - For high-frequency transactions or live updates, transition the dashboard widgets to standard Server-Sent Events (SSE) or WebSockets.

---

## 4. Database Table Locking & Cascade Reassignments
* **Debt / Tradeoff**: Deleting custom metadata categories or payment methods triggers an atomic, in-transaction cascade reassignment:
  - Reference updates (`category_id` reassigned to defaults) are executed directly in the main transaction block on the PostgreSQL database layer.
* **Impact**: For users with millions of expense records, this updates multiple rows synchronously, which can lock the `expenses` table and block concurrent read/write actions.
* **Future Work**:
  - Defer custom metadata deletions and cascade reassignments to background async task queues (e.g. Celery / RQ / BullMQ) to ensure fast API responses and prevent database locking.

---

## 5. Custom SVG Chart Limitations vs D3.js
* **Debt / Tradeoff**: Custom inline SVG charts (bezier curves and vertical bars) were built directly in React with Framer Motion. This keeps package size small and resolves React 19 / Turbopack dependencies.
* **Impact**: Lacks advanced chart features like zoom, pan, brush ranges, or complex multi-series overlay capabilities.
* **Future Work**:
  - If analytics scale to show year-over-year data or multi-axis comparisons, migrate to D3.js or a robust Canvas/WebGL charting library to maintain high render performance with large datasets.

---

## 6. Mutually Exclusive Search Filters
* **Debt / Tradeoff**: Following the SRS search constraints, selecting a category resets date filters, and selecting dates resets category filters.
* **Impact**: Prevents compound filters (e.g., searching for "Shopping" expenses *only* in "December").
* **Future Work**:
  - Update repository logic and frontend components to support composite filtering if the product requirements relax this constraint.

---

## 7. Frontend Environment Variable Coupling (Vercel Static Builds)
* **Debt / Tradeoff**: The `NEXT_PUBLIC_API_BASE_URL` environment variable is baked into the Next.js static build output at compile time. Vercel pre-renders all static pages (`○`) during the build step, embedding the backend URL directly into the generated HTML/JS bundles.
* **Impact**: Any change to the backend URL (e.g., new Render service, domain migration) requires a full frontend **redeploy** on Vercel — not just an environment variable update — for the change to take effect. Static pages will not dynamically pick up the new value at runtime.
* **Future Work**:
  - Consider moving backend URL resolution to a server-side Next.js API route or middleware so it can be updated at runtime without rebuilding the frontend bundle.

---

## 8. Single Dockerfile — Multi-Stage Build Optimization
* **Debt / Tradeoff**: The current `backend/Dockerfile` uses a single-stage build (`python:3.11-slim`). It copies all source files into the image.
* **Impact**: Docker image size is larger than necessary.
* **Future Work**:
  - Migrate to a multi-stage Dockerfile: a `builder` stage installs dependencies; a `runtime` stage copies only the necessary artifacts into a minimal base image.
