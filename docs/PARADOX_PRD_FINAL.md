# Paradox — Product Requirements Document (PRD)

**Document Version:** 2.3  
**Status:** Paradox V2 Supercharged Platform (Multi-Currency, Recurring Subscriptions, CSV Statement Import/Export, Multimodal Vision OCR, Omnichannel Dual-Mode Authentication, In-App Multi-Account Switcher & Universal Email Delivery)  
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
- **Sign Up / Register**: Pre-registration email verification to prevent fraudulent or typo accounts. Supports **Dual-Mode Omnichannel Verification**:
  - **Instant 6-Digit OTP**: Displayed prominently in the email subject line and body for fast completion directly on the originating desktop screen without opening separate browser tabs.
  - **1-Click Magic Link**: Mobile users can click the verification link in their email; open desktop tabs automatically detect confirmation via real-time background status polling and navigate into the app without device-switching friction.
- **Login**: Fast, secure login with email/password or one-click **Sign in with Google** (OAuth 2.0 / OpenID Connect).
- **In-App Multi-Account Switcher & Session Vault**: Users can register or log into multiple accounts (e.g. personal, freelance, business) and seamlessly switch active workspaces from the profile dropdown in 1 click without losing sessions or re-entering credentials.
- **Session Management**: Automatic background token refresh (short-lived JWT access tokens + rotated HttpOnly refresh cookies), secure single-session logout, and "Logout from all devices" to terminate active sessions.
- **Password Recovery**: Self-service forgot and reset password flows.
- **Universal Email Delivery**: Multi-provider email engine delivering transactional verification codes via Brevo REST API, Resend, or SMTP to Gmail without requiring custom domain DNS setups.
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
- **As a user registering on my laptop**, I want to enter the 6-digit verification code from my email notification so that I can complete registration immediately on my laptop without device-switching.
- **As a user who taps a magic verification link on my phone**, I want my open laptop screen to automatically detect my verification in real time and take me straight to the dashboard without starting over.
- **As a returning user**, I want to log in quickly and stay logged in securely across page reloads without entering my password every 15 minutes.
- **As a user**, I want to log in with one click using Google Sign-In so that I don't have to manage another password.
- **As a user with multiple accounts (e.g. personal and business)**, I want to switch between my accounts from the profile dropdown without logging out and re-entering credentials.
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
| FR-AUTH-01 | The product must allow new users to initiate registration with email verification before account creation. | P0 |
| FR-AUTH-02 | The product must allow users to log in securely with email and password. | P0 |
| FR-AUTH-03 | The product must support Google Sign-In via OAuth 2.0 / OpenID Connect. | P0 |
| FR-AUTH-04 | The product must maintain authenticated sessions securely with automatic token refresh. | P0 |
| FR-AUTH-05 | The product must allow users to log out from the current device or all devices. | P0 |
| FR-AUTH-06 | The product must support password recovery (forgot password and reset password). | P0 |
| FR-AUTH-07 | The product must allow authenticated users to change their password. | P0 |
| FR-AUTH-08 | Every user's data (expenses, budgets, custom categories, payment methods) must be strictly isolated. | P0 |
| FR-AUTH-09 | The product must support dual-mode omnichannel registration with a 6-digit OTP code and a single-use magic link. | P0 |
| FR-AUTH-10 | The product must automatically synchronize registration completion across devices in real time via background status polling. | P0 |
| FR-AUTH-11 | The product must support in-app multi-account switching with a client-side session vault and cryptographic token exchange. | P0 |
| FR-AUTH-12 | The product must support resilient transactional email delivery via Brevo REST API with delivery to Gmail without requiring custom DNS domains. | P0 |
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
| FR-AI-01 | The product must provide real-time category recommendations as users enter notes or descriptions. | P1 |
| FR-AI-02 | The product must support natural language expense parsing ("AI Quick Add") from freeform sentences. | P1 |
| FR-AI-03 | The product must allow scanning receipt and invoice images via Multimodal Vision OCR to auto-fill records. | P1 |
| FR-AI-04 | The product must provide an AI Financial Copilot card evaluating burn velocity, period projection, and savings tips. | P1 |
| FR-CURR-01 | The user must be able to switch and persist their active currency preference (INR ₹, USD $, EUR €, GBP £). | P0 |
| FR-RECUR-01 | The user must be able to flag expenses as recurring subscriptions (monthly, weekly, yearly). | P1 |
| FR-RECUR-02 | The dashboard must calculate and display active subscriptions and monthly recurring financial commitments. | P1 |
| FR-CSV-01 | The user must be able to export their transactions into a standard CSV spreadsheet. | P1 |
| FR-CSV-02 | The user must be able to import bank statements via CSV with AI auto-categorization of transactions. | P1 |
| FR-REP-01 | The product must generate a printable / PDF-exportable Monthly Financial Health Report. | P1 |

---

## 12. Product Quality Requirements

### Security & Privacy
- User passwords must be securely hashed with zero plaintext exposure.
- Sessions must use short-lived access tokens with rotated HttpOnly refresh cookies.
- All financial records are private; cross-tenant data leakage is strictly prevented.

### Simplicity & Usability
- Main screens (Login, Register, Dashboard, Expenses, Categories, Budget) must be intuitive without external documentation.
- Recording an expense must take under 30 seconds (under 5 seconds via AI Quick Add or Receipt Scanner).

### Accuracy & Reliability
- Financial calculations use fixed-precision decimal arithmetic.
- Persisted records are never lost.

---

## 13. Scope by Product Area

### In Scope
- User Registration with Dual-Mode Omnichannel Verification (6-digit OTP code + Magic Link cross-device sync)
- User Login (Email/Password) and Google OAuth 2.0 Sign-In
- In-App Multi-Account Switcher & Session Vault (fast account swapping without full logout)
- Session Management (Auto-Refresh, Secure Logout, Logout from all devices)
- Self-service Password Recovery (Forgot/Reset Password) and Password Change
- Strict Row-Level Multi-Tenant Data Isolation
- Universal Transactional Email Dispatch (Brevo REST API, Resend, SMTP)
- Dynamic Multi-Currency System (`₹ INR`, `$ USD`, `€ EUR`, `£ GBP`) with user profile persistence
- Expense CRUD (Create, Read, Update, Delete) with Recurring Subscriptions support
- CSV Statement Import with AI categorizer and CSV Export
- Multimodal Receipt & Invoice Vision OCR Scanner with client-side image optimization
- AI Quick Add Natural Language Parser & Real-Time Category Inference
- AI Financial Copilot (burn velocity, period projection, saving insights)
- Monthly Financial Health Report (Print / PDF)
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
- Investment, loan, and tax management
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

## Phase 4 — AI Financial Intelligence & Smart Automation (Completed & Live)

### Objective

Accelerate the expense logging loop and empower users with proactive, intelligent financial insights powered by an environment-driven, multi-provider AI engine (Google Gemini, OpenAI, Anthropic Claude, and resilient offline semantic heuristics).

### Implemented & Live Capabilities

1. **Multi-Provider AI Service Layer**:
   - Zero hardcoding, 100% environment-driven provider auto-detection (`AI_PROVIDER=auto|gemini|openai|anthropic`).
   - Dynamic credentials support (`GEMINI_API_KEY`, `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `AI_MODEL`).
   - Upgraded to **Google Gemini 3.6 Flash** Vision & text models with automatic offline fallback to built-in semantic keyword heuristic engine, guaranteeing zero downtime and offline resilience without external dependencies.
2. **Intelligent Financial Categorization Recommendations**:
   - Analyzes expense note/merchant descriptions (e.g., *"Starbucks iced latte"*, *"HP Petrol pump"*, *"Swiggy dinner"*) and scores the best-matching user category with confidence ratings and reasoning.
   - Frontend integrates debounced real-time suggestions below description input with 1-click apply action.
3. **Natural Language Expense Parser ("AI Quick Add")**:
   - Users can type or speak freeform sentences (e.g. *"Paid 1000 for a new gaming mouse via upi 2 days ago"* or *"Starbucks coffee 250 cash"*).
   - Resolves relative date references (`N days ago`, `yesterday`, `N weeks ago`, `N months ago`).
   - AI extracts structured values: `amount`, `category`, `payment_method`, `date`, and `description`, pre-filling the expense modal in ~1 second.
4. **AI Financial Copilot & Smart Insights Card**:
   - Proactive dashboard card delivering actionable spending analysis:
     - *Health Status*: Evaluates spending velocity against budget limits (`Healthy`, `Warning`, `Critical`).
     - *Daily Burn Velocity*: Calculates real-time daily burn rate (`₹XXX / day`).
     - *Projected Period Total*: Real-time burn-rate projection calculating date of potential budget exhaustion.
     - *Smart Saving Tips*: Personalized micro-recommendations based on discretionary spending patterns.
5. **Multimodal Receipt & Invoice Vision OCR Scanner**:
   - Camera/photo upload for receipts and bills.
   - Client-side HTML5 canvas downscaler compresses 5MB-10MB camera photos in ~50ms to ~120KB, eliminating upload timeouts.
   - Gemini 3.6 Flash Vision parses merchant, total amount, transaction date, category, and payment method, auto-filling all fields in seconds.

---

## Phase 5 — Paradox V2 Supercharged Financial Suite (Completed & Live)

### Objective

Deliver advanced financial power tools: multi-currency, recurring subscriptions, bank statement CSV processing, and exportable financial health reports.

### Implemented Capabilities

1. **Dynamic Multi-Currency System (`₹ INR`, `$ USD`, `€ EUR`, `£ GBP`)**:
   - Persistent user preference stored in `users.currency` table and updated via `PATCH /api/v1/auth/me`.
   - Global reactive `CurrencyContext` with Topbar switcher.
   - Re-formats all monetary values across Dashboard, Expense lists, charts, and Copilot.
2. **Intelligent Bank Statement CSV Import & Export**:
   - Flexible CSV statement parser discovering Date, Debit Amount, and Narration columns.
   - AI auto-categorizer classifies imported transactions in bulk without manual tagging.
   - One-click full CSV transaction export.
3. **Recurring Expenses & Fixed Bills Tracker**:
   - Tags expenses as recurring with frequency options (`monthly`, `weekly`, `yearly`).
   - Dashboard widget displaying active subscription count and normalized monthly commitments.
4. **Monthly Financial Health Report**:
   - Clean printable report modal summarizing period totals, category allocations, budget adherence %, and recurring commitments with 1-click "Print / Save PDF" support.

---

## Phase 6 — Long-Term Product Evolution

- Automated bank sync (account aggregator APIs)
- Advanced predictive forecasting & year-over-year analytics
- Multi-factor authentication (TOTP / Authenticator App)
- Mobile native apps (React Native / Flutter)

---

# 15. Core User Journey

The primary user journey for an authenticated Paradox user:

1. **Access**: User visits Paradox, registers or signs in (Email/Password or Google).
2. **Onboard**: The user's private dashboard loads with default starter categories, currency preference, and empty state guidance.
3. **Record**: User logs an expense manually, via Natural Language AI Quick Add, or by scanning a receipt photo.
4. **Organize**: User creates custom categories, payment methods, or tags recurring subscriptions.
5. **Plan**: User configures Monthly, Weekly, or Daily budgets.
6. **Understand**: User views real-time totals, category breakdowns, budget meters, and AI Copilot burn velocity.
7. **Audit & Export**: User exports CSV statements, imports bank records, or generates a Monthly Financial Health Report.
8. **Secure**: User can manage sessions, change passwords, or log out from all devices.

---

# 16. Acceptance Criteria

Paradox is functionally complete when:

- [x] A user can register via 6-digit numeric OTP code sent to their email or via 1-click magic link.
- [x] A desktop browser polling a pending registration automatically detects completion when verified on a secondary mobile device.
- [x] A user can switch between multiple saved accounts from the profile menu without a full logout cycle.
- [x] A user can log in with password or sign in via Google.
- [x] Sessions refresh automatically in the background without interrupting the user.
- [x] Logging out or logging out from all devices invalidates active sessions.
- [x] A user can record, edit, and delete expenses in under 30 seconds (under 5 seconds via AI Quick Add or Scan).
- [x] Expenses can be reviewed across date ranges, searched by keyword, filtered by category, and sorted.
- [x] Users can define and track Monthly, Weekly, and Daily budgets.
- [x] All data is strictly isolated: User A cannot see or mutate User B's data.
- [x] Attempting to access another user's records returns `404 Not Found`.
- [x] Responsive layout works seamlessly across mobile, tablet, and desktop.
- [x] Intelligent financial categorization recommendations suggest relevant categories based on notes/descriptions.
- [x] Natural language expense parser ("AI Quick Add") extracts structured fields from freeform sentences in under 3 seconds.
- [x] Multimodal Vision OCR receipt scanner reads bill photos and auto-fills the expense form in ~1-2 seconds.
- [x] Multi-currency selector allows switching between INR ₹, USD $, EUR €, and GBP £ with persistent formatting.
- [x] Recurring subscriptions can be tracked with monthly financial commitment calculations.
- [x] Transactions can be exported to CSV, and bank statement CSVs can be imported with AI categorization.
- [x] Monthly Financial Health Reports can be viewed and printed or saved to PDF.
- [x] Multi-provider AI engine dynamically detects Gemini, OpenAI, Claude, or falls back to offline semantic heuristics.

---

# 17. Success Metrics

## 17.1 Primary Success Indicators

- User onboarding and login completion rate > 95%.
- Average time to log an expense reduced from under 30 seconds to **under 5 seconds** via AI Quick Add or Vision OCR Scan.
- 0 incidents of cross-tenant data leakage.
- Active weekly habit retention.

---

# 18. Product Principles

1. **Simplicity over feature count**: A clean, fast product that people actually use.
2. **Absolute privacy & data isolation**: Financial data is strictly private to each account.
3. **Zero trust in client identity**: Security is strictly enforced on the server.
4. **Financial accuracy**: Exact decimal precision across all calculations.
5. **No unnecessary complexity**: No complex RBAC or bloated enterprise overhead where a simple isolated model is best.
6. **Smart, Non-Intrusive Intelligence**: AI acts as an assistant to reduce friction, never blocking or enforcing unwanted changes.

---

# 19. Product Roadmap Summary

| Phase | Primary Goal | Main Outcome | Status |
|---|---|---|---|
| **Phase 1** | Validate core expense loop | Single-user MVP | **Completed** |
| **Phase 2** | Multi-granularity budgeting & PWA | Refined Budget Planner & Mobile UX | **Completed** |
| **Phase 3** | Multi-user foundation & Auth | JWT + Google OAuth + Strict Data Isolation | **Completed** |
| **Phase 4** | AI Financial Intelligence & Smart Automation | Multi-Provider AI (Gemini 3.6 Flash / OpenAI / Claude), AI Quick Add, Smart Categorization, Copilot | **Completed & Live** |
| **Phase 5** | Paradox V2 Supercharged Financial Suite | Multi-Currency (`₹`, `$`, `€`, `£`), Recurring Subscriptions, CSV Statement Import/Export, Receipt Vision OCR, Health Reports | **Completed & Live** |
| **Phase 6** | Broader Financial Expansion | Open banking aggregators, predictive forecasting, native mobile | Future |

---

## Document Status

**Version 2.3 — Final Product Requirements Document (PRD) for Paradox.**

This document is the product-level source of truth for Paradox. Technical design and architecture decisions are documented in [`PARADOX_SRS.md`](file:///e:/Projects/Paradox/docs/PARADOX_SRS.md) and trace directly back to the requirements and goals defined here.
