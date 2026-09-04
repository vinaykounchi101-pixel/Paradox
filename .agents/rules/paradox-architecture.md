# Paradox Architecture Rules & Guidelines

These rules govern all agentic modifications in the Paradox repository:

## 1. Core Architecture
- **Layered Flow**: Strictly maintain `API Router ➔ Service Layer ➔ Repository Layer ➔ Database`.
- **FastAPI Thin Controllers**: Route handlers validate input schemas and delegate to services.
- **Service Layer**: Pure business logic, authorization checks, and external AI/email integrations.
- **Repository Layer**: Pure SQLAlchemy 2.0 database queries scoped strictly to `user_id == current_user.id`.
- **Alembic Migrations**: All schema changes must be applied via Alembic migrations. Zero manual schema modifications.

## 2. Monetary Precision & Arithmetic
- Never use floating-point types for currency or monetary transactions.
- Always use `Numeric(12, 2)` (Python `Decimal`) with database `CHECK (amount > 0)` and `CHECK (amount >= 0)`.
- Financial metrics (burn velocity, safe-to-spend, health score, 50/30/20 breakdown, simulators) MUST be calculated deterministically in backend Python service logic.
- LLMs provide qualitative synthesis, commentary, and conversational advice only.

## 3. Security & Multi-Tenancy
- Short-lived JWT access tokens (15-minute expiry) in memory.
- Long-lived refresh tokens stored as `HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth` cookies.
- Refresh tokens are single-use with immediate rotation and SHA-256 database hashing (`token_hash`).
- Never log, commit, or expose secrets, passwords, or PII.
- Dynamic Vercel preview deployments use `allow_origin_regex=r"https:\/\/.*\.vercel\.app"` in FastAPI CORSMiddleware.

## 4. UI/UX & Design System
- Dark mode first (`#09090b` zinc background, `#6366f1` indigo primary accents).
- Dynamic multi-currency support (`₹ INR`, `$ USD`, `€ EUR`, `£ GBP`) via `CurrencyContext`. Never hardcode `$` or `₹`.
- Responsive across mobile, tablet, and desktop with smooth Framer Motion transitions.
