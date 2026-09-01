# Paradox — Product Requirements Document (PRD)

**Document Version:** 2.0  
**Status:** Product Definition with Multi-User Authentication & Data Isolation  
**Product:** Paradox  
**Document Purpose:** Define what Paradox solves, why it exists, what users can do, what success looks like, and how the product evolves through clearly defined phases.

---

## 1. Product Overview

Paradox is a personal expense-tracking product designed to help people understand, organize, and control their spending without making financial management unnecessarily complicated.

The product has validated its core financial recording loop (**Record → Organize → Review → Understand → Adjust**) and now establishes a **production-ready multi-user foundation with secure authentication and strict data isolation**.

Each user creates an independent account (via email/password or Google Sign-In), logs in securely, and manages their own private financial records (expenses, custom categories, payment methods, multi-granularity budgets, and dashboards). No user can access or view another user's financial data.

### Product principle

> Simplicity, absolute data isolation, financial accuracy, and low-friction expense management.

---

## 2. Problem Statement

Managing personal expenses is often harder than it should be.

People spend money across cash, cards, bank accounts, digital wallets, subscriptions, food, transportation, education, entertainment, shopping, and many other categories. Although financial information exists in many places, users may still struggle to answer simple questions such as:

- Where did my money go this month?
- How much did I spend in each category?
- Am I spending more than I intended?
- Which categories are consuming most of my money?
- How has my spending changed over time?
- Am I staying within my budget?

Existing expense-tracking solutions may also feel too complex, contain unnecessary features, require too much manual organization, or make basic financial information difficult to understand.

### Core problem

**Users need a simple, secure, and dependable way to record personal expenses and turn those records into understandable spending information so they can make better financial decisions.**

---

## 3. Why Paradox Exists

Paradox exists to reduce the gap between **spending money** and **understanding spending**.

The product should make expense tracking easy enough that a user can maintain the habit without feeling that financial management itself has become a second job.

Its core responsibilities are to perform important tasks extremely clearly:

1. Provide secure and seamless authentication (Email/Password & Google Sign-In).
2. Ensure strict private data isolation for each user.
3. Make recording an expense straightforward (under 30 seconds).
4. Keep expense information organized by categories and payment methods.
5. Help the user review where money is going across flexible timeframes (Daily, Weekly, Monthly).
6. Provide simple context around spending and budgeting.
7. Give the user enough information to make better everyday decisions.

---

## 4. Product Vision

### Short-term vision

Provide a simple, secure, fast, and reliable personal expense tracker with user authentication and multi-granularity budgeting for everyday use.

### Long-term vision

Evolve Paradox into a comprehensive personal finance product that helps users **track, understand, plan, and improve their financial behavior** with intelligent insights and deeper analytics.

---

## 5. Product Goals

### Primary goals

1. **Seamless, secure user onboarding & authentication**: Users can register, log in with email/password or Google, recover lost passwords, and manage sessions securely.
2. **Absolute multi-tenant data isolation**: Ensure User A can never view, mutate, or access User B's financial records under any circumstance.
3. **Frictionless expense tracking**: Enable users to record and categorize an expense in under 30 seconds.
4. **Actionable spending visibility**: Provide clear dashboard insights, category breakdowns, and multi-granularity budget tracking.

---

## 6. Non-Goals for the Current Product

The product avoids premature operational complexity and focuses strictly on personal expense tracking:

- Complex investment management
- Loans and credit line management
- Tax planning
- Automated bank scraping / third-party sync (manual/API entry remains the primary input)
- Social / shared finance or multi-user shared accounts
- Role-Based Access Control (RBAC) / Admin systems (all accounts are equal and isolated)

---

## 7. Target User

A person who wants a simple, secure, and private way to maintain a record of personal spending and understand where their money goes without feeling overwhelmed.

---

## 8. Product Solution

Paradox solves the problem through a secure financial tracking loop:

**Authenticate → Record → Organize → Review → Understand → Adjust**

---

## 9. Core Product Experience

### 9.0 User Authentication & Account Security
- **Sign Up / Register**: Create an account with email, password, and display name.
- **Login**: Fast, secure login with email/password or one-click **Sign in with Google** (OAuth 2.0 / OpenID Connect).
- **Session Management**: Automatic background token refresh, secure single-session logout, and "Logout from all devices" to terminate active sessions.
- **Password Recovery**: Self-service forgot and reset password flows.
- **Data Isolation**: All dashboard metrics, expenses, categories, payment methods, and budgets are strictly isolated per user account.

### 9.1 Add an Expense
- Amount, Category, Payment Method, Date, and optional note.
- Fast entry target: **under 30 seconds**.

### 9.2 View Expenses
- Paginated, filterable, searchable expense history scoped to the logged-in user.

### 9.3 Edit & Delete Expenses
- Edit expense details or delete with clear confirmation.

### 9.4 Categories & Payment Methods
- Default starter categories/methods available to all users.
- User-created custom categories and payment methods private to the creator.

### 9.5 Multi-Granularity Budgeting
- Plan spending across **Monthly**, **Weekly**, or **Daily** periods with real-time audit meters and status alerts (`under_budget`, `near_limit`, `over_budget`).

### 9.6 Dashboard Overview
- Summary cards, category breakdowns, spending trends, and recent expense feed.

---

## 10. User Stories

### Authentication & Account Security
- **As a new user**, I want to register with my email and password or Google account so that I can create a private financial space.
- **As a returning user**, I want to log in quickly and stay logged in securely across page reloads without entering my password every 15 minutes.
- **As a user**, I want to log in with one click using Google Sign-In so that I don't have to manage another password.
- **As a user who forgot my password**, I want to request a password reset link so that I can regain access to my account.
- **As a security-conscious user**, I want to log out from all devices at once if I suspect an unauthorized session.
- **As a user**, I want to be certain that no other user can see, modify, or delete my expenses or financial records.

### Expense Tracking & Review
- **As a user**, I want to add an expense with amount, category, payment method, date, and description in under 30 seconds.
- **As a user**, I want to edit or delete existing expenses.
- **As a user**, I want to search and filter my expense history by date range or category.

### Budgeting & Insights
- **As a user**, I want to set spending targets for the month, week, or day so that I can stay on budget.
- **As a user**, I want a visual dashboard showing total spending, category breakdowns, and recent transactions.

---

## 11. Functional Product Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-AUTH-01 | The product must allow new users to register with email, password, and optional display name. | P0 |
| FR-AUTH-02 | The product must allow users to log in securely with email and password. | P0 |
| FR-AUTH-03 | The product must support Google Sign-In via OAuth 2.0 / OpenID Connect. | P0 |
| FR-AUTH-04 | The product must maintain authenticated sessions securely with automatic token refresh. | P0 |
| FR-AUTH-05 | The product must allow users to log out from the current device or all devices. | P0 |
| FR-AUTH-06 | The product must support password recovery (forgot password and reset password). | P0 |
| FR-AUTH-07 | The product must allow authenticated users to change their password. | P0 |
| FR-AUTH-08 | Every user's data (expenses, budgets, custom categories, payment methods) must be strictly isolated. | P0 |
| FR-01 | The user must be able to create an expense. | P0 |
| FR-02 | The product must prevent clearly invalid expense records from being saved. | P0 |
| FR-03 | The user must be able to view recorded expenses in an understandable history. | P0 |
| FR-04 | The user must be able to edit an existing expense. | P0 |
| FR-05 | The user must be able to delete an existing expense with clear confirmation. | P0 |
| FR-06 | Every expense must have a meaningful category. | P0 |
| FR-07 | The user must be able to create and rename custom categories. | P0 |
| FR-08 | Category removal must not make existing expense records unclear or inconsistent. | P0 |
| FR-09 | The user should be able to associate expenses with payment methods. | P0 |
| FR-10 | The user must be able to review expenses by date range. | P0 |
| FR-11 | Expense history should support search by title or notes. | P1 |
| FR-12 | Expense history should support filtering by date or category. | P1 |
| FR-13 | Expense history should support sorting by date, amount, and category. | P1 |
| FR-14 | Spending totals must be calculated and displayed accurately. | P0 |
| FR-15 | The product must show category-based spending. | P0 |
| FR-16 | The product should show spending trends across meaningful periods. | P1 |
| FR-17 | The product should identify the user's highest-spending categories. | P1 |
| FR-18 | The product must allow defining spending budgets (Monthly, Weekly, Daily). | P0 |
| FR-19 | The product must compare actual spending with the configured budget. | P0 |
| FR-20 | The product must show budget amount, amount spent, remaining amount, and a simple status. | P0 |
| FR-21 | The product must provide a dashboard summarizing important spending information. | P0 |
| FR-22 | Financial records must remain available when the user returns to the product. | P0 |
| FR-23 | Normal product use must reflect the user's real records rather than fabricated financial data. | P0 |
| FR-24 | Empty, validation, loading, and failure states should be understandable to a non-technical user. | P0 |

---

## 12. Product Quality Requirements

### Security & Privacy
- User passwords must be securely hashed with zero plaintext exposure.
- Sessions must use short-lived access tokens with rotated HttpOnly refresh cookies.
- All financial records are private; cross-tenant data leakage is strictly prevented.

### Simplicity & Usability
- Main screens (Login, Register, Dashboard, Expenses, Categories, Budget) must be intuitive without external documentation.
- Recording an expense must take under 30 seconds.

### Accuracy & Reliability
- Financial calculations use fixed-precision decimal arithmetic.
- Persisted records are never lost.

---

## 13. Scope by Product Area

### In Scope
- User Registration, Email/Password Login, and Google OAuth 2.0 Sign-In
- Session Management (Auto-Refresh, Secure Logout, Logout from all devices)
- Self-service Password Recovery (Forgot/Reset Password) and Password Change
- Strict Row-Level Multi-Tenant Data Isolation
- Expense CRUD (Create, Read, Update, Delete)
- Starter and Custom Categories
- Starter and Custom Payment Methods
- Multi-Granularity Budget Planner (Monthly, Weekly, Daily)
- Dashboard Overview with Real-Time Metrics & Charts
- Expense Search, Filtering (single-dimension), and Sorting
- Responsive Web Application & PWA Support
- Clear empty, validation, loading, and error states

### Out of Scope
- Role-Based Access Control (RBAC) / Admin systems (all users have equal ownership of only their data)
- Multi-user shared/joint accounts
- Bank account direct synchronization / auto-scraping
- Investment, loan, and tax management
- AI financial advice or complex forecasting
- SMS notifications

---

# 14. Product Development Phases

The phases are deliberately ordered around **validation first and expansion second**.

---

## Phase 1 — Single-User Core MVP

### Objective

Build the smallest complete product that can be used by one real person to track and understand personal expenses.

### Problem being tested

Can a person consistently use Paradox to record expenses and gain useful visibility into their spending?

### Product capabilities

- Add expenses
- View expenses
- Edit expenses
- Delete expenses
- Use starter categories
- Create and rename custom categories
- Select payment methods
- Search expense history
- Filter by date, category, amount, and payment method
- Sort by date, amount, or category
- Review spending by day, week, or month
- View total spending
- View category-wise spending
- View recent expenses
- View spending trends and useful period comparisons
- See top spending categories
- Set one monthly budget
- Compare spending against the budget
- See remaining budget and simple status
- View a basic dashboard

### Validation focus

The goal is not feature quantity. The goal is to validate the core behavior:

**The user records expenses consistently and uses the resulting information to understand spending.**

### Exit criteria

Phase 1 is successful when:

- The core expense workflow can be completed without confusion.
- The user can maintain an accurate expense record over a meaningful period.
- The dashboard and summaries are understandable.
- The user can identify where their money is going.
- The user considers the product useful enough to continue using.
- Major usability problems have been identified and addressed.

### Phase 1 outcome

A validated single-user MVP with evidence about whether the core product deserves further development.

---

## Phase 2 — Refinement, Multi-Granularity Budgeting & PWA

### Objective

Enhance the expense workflow, introduce multi-granularity budgeting, and deliver responsive PWA capability.

### Product capabilities completed

- Flexible spending budgets (**Monthly**, **Weekly**, **Daily**) with real-time threshold calculations.
- Progressive Web App (PWA) manifest and mobile responsiveness.
- Improved filtering, search, sorting, and category reassignment safeguards.
- Polished dashboard with visual breakdown charts and dark mode aesthetic tokens.

---

## Phase 3 — Multi-User Product Foundation & Secure Authentication (Active)

### Objective

Deliver a production-ready, multi-user application with secure authentication, session management, and strict row-level data isolation.

### Product focus & capabilities

- **Self-Service Registration & Login**: Email/password authentication and Google OAuth 2.0 / OpenID Connect.
- **Session Management**: Automatic background token refresh (short-lived access tokens + rotated HttpOnly refresh cookies), secure single logout, and "Logout from all devices".
- **Account Recovery**: Self-service forgot and reset password flows with secure tokens.
- **Strict Row-Level Data Isolation**: Absolute multi-tenant data separation. User A cannot view, modify, or infer the existence of User B's records.
- **Zero RBAC / Equal Ownership**: Every user has full ownership of their own private expenses, custom categories, payment methods, and budgets.

### Exit criteria

- Users can register, log in, sign in with Google, refresh tokens, and log out seamlessly.
- Every API endpoint strictly isolates user data with zero cross-tenant leakage.
- Automated tests verify token lifecycles and cross-user data boundaries.

---

## Phase 4 — Advanced Financial Understanding

### Objective

Move beyond basic tracking toward deeper personal financial insights.

### Potential capabilities

- Multi-series spending trends and period-over-period comparisons
- Recurring expense detection and reminders
- Export reports (CSV / PDF)
- Spending anomaly detection

---

## Phase 5 — Production-Ready Enterprise Hardening

### Objective

Enterprise-grade resilience, multi-factor authentication (MFA), and transactional email delivery.

### Product focus

- Multi-factor authentication (TOTP / SMS)
- Real transactional email dispatch via SendGrid/SES for password resets
- Redis-backed distributed session cache
- Advanced audit logging

---

## Phase 6 — Long-Term Product Evolution

- Multi-currency support and localized conversion
- Intelligent financial categorization recommendations
- Optional third-party financial service integrations

---

# 15. Core User Journey

The primary user journey for an authenticated Paradox user:

1. **Access**: User visits Paradox, registers or signs in (Email/Password or Google).
2. **Onboard**: The user's private dashboard loads with default starter categories and empty state guidance.
3. **Record**: User quickly logs an expense (Amount, Category, Payment Method, Date, Note) in under 30 seconds.
4. **Organize**: User creates custom categories or payment methods as needed.
5. **Plan**: User configures Monthly, Weekly, or Daily budgets.
6. **Understand**: User views real-time totals, category breakdowns, and budget meters.
7. **Secure**: User can manage sessions, change passwords, or log out from all devices.

---

# 16. Acceptance Criteria

Paradox is functionally complete when:

- [x] A user can register, log in with password, or sign in via Google.
- [x] Sessions refresh automatically in the background without interrupting the user.
- [x] Logging out or logging out from all devices invalidates active sessions.
- [x] A user can record, edit, and delete expenses in under 30 seconds.
- [x] Expenses can be reviewed across date ranges, searched by keyword, filtered by category, and sorted.
- [x] Users can define and track Monthly, Weekly, and Daily budgets.
- [x] All data is strictly isolated: User A cannot see or mutate User B's data.
- [x] Attempting to access another user's records returns `404 Not Found`.
- [x] Responsive layout works seamlessly across mobile, tablet, and desktop.

---

# 17. Success Metrics

## 17.1 Primary Success Indicators

- User onboarding and login completion rate > 95%.
- Average time to log an expense is under 30 seconds.
- 0 incidents of cross-tenant data leakage.
- Active weekly habit retention.

---

# 18. Product Principles

1. **Simplicity over feature count**: A clean, fast product that people actually use.
2. **Absolute privacy & data isolation**: Financial data is strictly private to each account.
3. **Zero trust in client identity**: Security is strictly enforced on the server.
4. **Financial accuracy**: Exact decimal precision across all calculations.
5. **No unnecessary complexity**: No complex RBAC or bloated enterprise overhead where a simple isolated model is best.

---

# 19. Product Roadmap Summary

| Phase | Primary Goal | Main Outcome | Status |
|---|---|---|---|
| **Phase 1** | Validate core expense loop | Single-user MVP | **Completed** |
| **Phase 2** | Multi-granularity budgeting & PWA | Refined Budget Planner & Mobile UX | **Completed** |
| **Phase 3** | Multi-user foundation & Auth | JWT + Google OAuth + Strict Data Isolation | **Active / In-Progress** |
| **Phase 4** | Advanced insights & reports | Trends, comparisons, recurring expenses | Planned |
| **Phase 5** | Production hardening | MFA, SendGrid email, distributed caching | Future |
| **Phase 6** | Broader financial expansion | Multi-currency, intelligent insights | Future |

---

## Document Status

**Version 2.0 — Final Product Requirements Document (PRD) for Paradox.**

This document is the product-level source of truth for Paradox. Technical design and architecture decisions are documented in [`PARADOX_SRS.md`](file:///e:/Projects/Paradox/docs/PARADOX_SRS.md) and trace directly back to the requirements and goals defined here.
