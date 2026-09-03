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

---

## 9. AI Multi-Provider Integration & Natural Language Parser
* **Status**: **IMPLEMENTED in Phase 4 (AI Quick Add & Smart Categorization)**.
* **Architecture / Tradeoff**:
  - `AIService` ([`backend/app/services/ai_service.py`](file:///e:/Projects/Paradox/backend/app/services/ai_service.py)) supports dynamic multi-provider configuration via environment variables: Google Gemini (`GEMINI_API_KEY`), OpenAI (`OPENAI_API_KEY`), and Anthropic Claude (`ANTHROPIC_API_KEY`).
  - Zero hardcoding: Defaults to heuristic rule-based parsing and category scoring if no keys are provided or if an external API call times out / errors.
  - Heuristic parser uses regex-based extraction for monetary values, payment methods, relative/absolute dates, and strips action verbs and prepositions to isolate the clean merchant or item name (e.g. `"Paid 450 for Zomato pizza via UPI yesterday"` -> `"Zomato pizza"`).
* **Future Work**:
  - Implement caching (Redis / in-memory LRU) for repeated merchant-to-category lookups to minimize external LLM token costs and latency.
  - Add background rate-limiting and token quota tracking per user session.

---

## 10. Multimodal Vision OCR Payloads & Timeout Elimination
* **Status**: **RESOLVED in Phase 5**.
* **Architecture / Tradeoff**:
  - High-resolution smartphone cameras take 5MB–12MB photos. Uploading raw base64 payloads to Google Gemini Vision endpoints frequently resulted in `httpx.ReadTimeout` and slow UI latency.
  - **Resolution**:
    - Implemented a client-side HTML5 `<canvas>` downscaler ([`ExpenseFormDialog.tsx`](file:///e:/Projects/Paradox/frontend/src/features/expenses/components/ExpenseFormDialog.tsx)) that resizes any high-resolution image down to max 1280px dimension at 85% JPEG quality in ~50ms, yielding ~120KB files.
    - Updated backend model routing to Google's active **Gemini 3.6 Flash** Vision API with `gemini-flash-latest` fallback and robust JSON regex extraction.
    - Installed `Pillow` on backend for auxiliary server-side image operations.
* **Future Work**:
  - Add offline client-side WASM OCR (e.g. Tesseract.js) for instantaneous 100% offline receipt parsing without network requests.

---

## 11. Deterministic Math & AI Commentary Separation (Phase 6 AI Evolution)
* **Status**: **RESOLVED in Phase 6**.
* **Architecture / Tradeoff**:
  - LLMs frequently hallucinate arithmetic calculations or return inconsistent numerical valuations when computing financial metrics.
  - **Resolution**:
    - Strictly compute all financial health indicators (`SafeToSpendResponse`, `FinancialHealthScoreResponse`, `SimulatePurchaseResponse`, `LeakAnalysisResponse`, `SubscriptionAuditResponse`) deterministically in backend Python service logic.
    - AI models (Gemini 3.6 Flash / Heuristics) provide qualitative commentary, category context, and behavioural recommendations based strictly on verified numbers.
* **Future Work**:
  - Add user-configurable scoring pillar weights (customizing the default 40/35/25 split) for the Financial Health Score.

---

## 12. Multi-Account In-App Switching & Client Vault Architecture
* **Status**: **RESOLVED in Phase 6**.
* **Architecture / Tradeoff**:
  - Previously, logging into another account in the same browser session would overwrite the single HttpOnly refresh token cookie, forcing a full logout/login cycle.
  - **Resolution**:
    - Created a secure client-side multi-session vault in [`AuthContext.tsx`](file:///e:/Projects/Paradox/frontend/src/features/auth/context/AuthContext.tsx) (`paradox_saved_accounts`).
    - Added dedicated backend endpoint `POST /api/v1/auth/switch-account` with cryptographic token rotation.
    - Integrated native dropdown profile switcher in [`shell.tsx`](file:///e:/Projects/Paradox/frontend/src/components/layout/shell.tsx) and [`AddAccountModal.tsx`](file:///e:/Projects/Paradox/frontend/src/features/auth/components/AddAccountModal.tsx) supporting both Google OAuth and email/password.
* **Future Work**:
  - Add biometrics (WebAuthn / Passkeys) for 1-touch switching on supported mobile and desktop browsers.



