# Technical Debt Analysis: Paradox Project (Phase 1 core MVP)

This document details technical debt items, architectural tradeoffs, short-term shortcuts taken during the Phase 1 implementation, and recommendations for future phases.

---

## 1. Authentication & Multi-Tenant Isolation
* **Debt / Tradeoff**: Currently, all API endpoints assume a mock single-user model using a pre-seeded `Primary User` record in the database.
* **Shortcut**: There are no authentication headers, login screens, or session cookies.
* **Future Work**: 
  - Implement JWT token authorization (OAuth2 / OpenID Connect) in the backend.
  - Modify database repositories to filter all queries by an authenticated `user_id` instead of a static mock fallback.
  - Setup frontend interceptors to attach bearer tokens to all client requests.

---

## 2. API Data Synchronization (Polling vs WebSockets)
* **Debt / Tradeoff**: TanStack Query is configured with a `staleTime` of 30 seconds for caching. It relies on standard refetch events (tab refocus, page reload, cache expiry) to synchronize state.
* **Impact**: If multiple clients write to the database simultaneously, updates will be delayed up to 30 seconds.
* **Future Work**: 
  - For high-frequency transactions or live updates, transition the dashboard widgets to standard Server-Sent Events (SSE) or WebSockets.

---

## 3. Database Table Locking & Cascade Reassignments
* **Debt / Tradeoff**: Deleting custom metadata categories or payment methods triggers an atomic, in-transaction cascade reassignment:
  - Reference updates (`category_id` reassigned to defaults) are executed directly in the main transaction block on the PostgreSQL database layer.
* **Impact**: For users with millions of expense records, this updates multiple rows synchronously, which can lock the `expenses` table and block concurrent read/write actions.
* **Future Work**:
  - Defer custom metadata deletions and cascade reassignments to background async task queues (e.g. Celery / RQ / BullMQ) to ensure fast API responses and prevent database locking.

---

## 4. Custom SVG Chart Limitations vs D3.js
* **Debt / Tradeoff**: Custom inline SVG charts (bezier curves and vertical bars) were built directly in React with Framer Motion. This keeps package size small and resolves React 19 / Turbopack dependencies.
* **Impact**: Lacks advanced chart features like zoom, pan, brush ranges, or complex multi-series overlay capabilities.
* **Future Work**:
  - If analytics scale to show year-over-year data or multi-axis comparisons, migrate to D3.js or a robust Canvas/WebGL charting library to maintain high render performance with large datasets.

---

## 5. Mutually Exclusive Search Filters
* **Debt / Tradeoff**: Following the SRS v1.1 search constraints, selecting a category resets date filters, and selecting dates resets category filters.
* **Impact**: Prevents compound filters (e.g., searching for "Shopping" expenses *only* in "December").
* **Future Work**:
  - Update repository logic and frontend components to support composite filtering if the product requirements relax this constraint.
