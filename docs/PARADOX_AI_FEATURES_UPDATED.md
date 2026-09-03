# Paradox — AI Features & Architecture Documentation (Updated)

> This document merges the currently implemented AI features from `AI_FEATURES.md` with the remaining useful features identified in `MERGED_AI_FEATURES.md`.
>
> **Purpose:** The features already implemented in Paradox are preserved as implemented/current. The additional features are marked **PLANNED** so this document can be used as the implementation roadmap for the remaining AI work.

---

# 1. AI Architecture

Paradox uses a dual-engine AI subsystem:

1. **Cloud LLM / Vision Engine**
2. **Deterministic Local Heuristic / Fallback Engine**

The architecture follows graceful degradation: when a configured cloud provider is unavailable, rate-limited, or no API key is available, deterministic local logic should provide a useful fallback wherever practical.

## 1.1 Cloud Engine

Current implementation is centered on Google Gemini, with support/configuration for additional providers.

Supported/configurable providers described across the source documents:

- Google Gemini
- OpenAI
- Anthropic
- Local heuristic engine

## 1.2 Privacy

AI processing should follow zero-trust principles:

- Do not transmit names, emails, passwords, tokens, bank credentials, or other unnecessary PII to external AI providers.
- Prefer aggregated financial metrics and required transaction information.
- Financial calculations such as totals, percentages, burn rates, and pacing metrics should be calculated deterministically rather than relying on the LLM for arithmetic.

---

# 2. Feature Status

| Feature | Status |
|---|---|
| Natural Language Quick Add & Expense Parser | ✅ IMPLEMENTED |
| Multimodal Receipt & Invoice OCR | ✅ IMPLEMENTED |
| Real-Time Description Category Suggestion | ✅ IMPLEMENTED |
| Financial Copilot & AI Insights | ✅ IMPLEMENTED |
| Intelligent Bank Statement CSV Import | ✅ IMPLEMENTED |
| Dynamic AI Budget Advisor | ⚙️ PLANNED |
| Multi-Line Receipt & SMS Transaction Parser | ⚙️ PLANNED |
| Purchase Decision Simulator — "Can I Afford This?" | ⚙️ PLANNED |
| Subscription & Recurring Expense Audit | ⚙️ PLANNED |
| Micro-Spending Leak Hunter | ⚙️ PLANNED |
| Safe-to-Spend Speedometer | ⚙️ PLANNED / EXTENSION |
| 50/30/20 Budget Optimization | ⚙️ PLANNED |
| Financial Health Score (0–100) | ⚙️ PLANNED |
| Monthly Executive AI Digest | ⚙️ PLANNED |
| Voice Expense Parser | ⚙️ PLANNED |
| Expense Note Sentiment Analysis | ⚙️ PLANNED |
| Financial Mascot / Finny | ⚙️ PLANNED |
| Hinglish Financial Assistant Support | ⚙️ PLANNED |
| Achievement-Based Financial Insights | ⚙️ PLANNED |
| Contextual AI Chat Actions | ⚙️ PLANNED |
| Duplicate Transaction Guarding | ⚙️ PLANNED |
| Debit/Credit Auto Classification | ⚙️ PLANNED |
| Receipt Line-Item Extraction | ⚙️ PLANNED |
| Multi-Model / Provider Rotation | ⚙️ PLANNED / ARCHITECTURE EXTENSION |

---

# 3. IMPLEMENTED FEATURES

## 3.1 Natural Language Quick Add & Expense Parser

**Endpoint:** `POST /api/v1/ai/parse-expense`

### Capabilities

- Understands conversational expense descriptions.
- Extracts amount, description, date, category, and payment method.
- Resolves relative dates such as:
  - `yesterday`
  - `day before yesterday`
  - `N days ago`
  - `N weeks ago`
  - `N months ago`
- Cleans descriptions by removing payment methods, amounts, relative dates, and unnecessary prepositions.
- Matches categories and payment methods against existing user data.

### Example

> "Paid 1000 for a new gaming mouse via upi 2 days ago"

Should become a structured expense draft with:

- Amount: ₹1000
- Description: gaming mouse
- Payment method: UPI
- Date: resolved exact date
- Category: matched category

---

# 4. IMPLEMENTED — MULTIMODAL RECEIPT & INVOICE OCR

**Endpoint:** `POST /api/v1/ai/scan-receipt`

### Current capabilities

- Upload/capture receipt or invoice images.
- Client-side image preprocessing.
- Vision OCR.
- Extracts:
  - Merchant/store name
  - Total amount
  - Transaction date
  - Category
  - Payment method
- Auto-fills the expense form.

### Existing implementation detail

The current implementation uses an HTML5 canvas preprocessor to resize large camera images before processing.

---

# 5. IMPLEMENTED — REAL-TIME DESCRIPTION CATEGORY SUGGESTION

**Endpoint:** `POST /api/v1/ai/categorize`

### Capabilities

- Categorizes descriptions while the user types.
- Supports category aliases/synonyms.
- Displays an AI category suggestion.
- Allows one-click application of the suggested category.

Example:

> "Uber to office"

→ AI Suggests: **Transport**

---

# 6. IMPLEMENTED — FINANCIAL COPILOT & AI INSIGHTS

**Endpoint:** `GET /api/v1/ai/insights?period=current_month|last_30_days|current_week`

### Current capabilities

- Spending health assessment.
- Daily burn velocity.
- Projected period spending.
- Spending spike detection.
- Unusual discretionary spending detection.
- Category over-utilization detection.
- Tailored savings tips.

The feature is grounded in the user's financial ledger rather than generic financial information.

---

# 7. IMPLEMENTED — INTELLIGENT BANK STATEMENT CSV IMPORT

**Endpoint:** `POST /api/v1/expenses/import`

### Current capabilities

- Imports CSV bank statements.
- Normalizes common statement headers.
- Detects date, amount/debit, and narration fields.
- Performs AI-based transaction classification in bulk.
- Supports common Indian bank statement formats described in the source document.

---

# 8. PLANNED FEATURES

The following features are the remaining useful capabilities consolidated from the additional feature documents.

---

## 8.1 Dynamic AI Budget Advisor

**Suggested Endpoint:** `GET /api/v1/ai/suggest-budget?period_type=month|week|day`

### Purpose

Analyze historical spending, spending velocity, and category allocations to recommend realistic budget targets.

### Requirements

- Analyze historical transaction data.
- Calculate category-level spending patterns.
- Consider recent spending velocity.
- Recommend an overall budget.
- Recommend category-level limits.
- Explain why each recommendation was made.
- Provide deterministic fallback calculations.

### Example

> "Based on your last 3 months, your food spending averages ₹7,200. A recommended monthly limit is ₹6,500."

---

# 9. "CAN I AFFORD THIS?" PURCHASE DECISION SIMULATOR

**Suggested Endpoint:** `POST /api/v1/ai/simulate-purchase`

### Purpose

Allow the user to test a purchase before spending the money.

### Input

- Purchase amount
- Category
- Optional description
- Optional payment method

### Analysis

Compare the purchase against:

- Current monthly cash flow
- Remaining category budget
- Remaining total budget
- Current savings trajectory
- Safe-to-spend amount

### Verdicts

- **Safe**
- **Caution**
- **Over-Budget**

### Output

Show before/after:

- Remaining budget
- Remaining safe-to-spend amount
- Projected month-end position
- Savings impact

### Action

Provide a **1-click "Add as Expense"** action after simulation.

---

# 10. SUBSCRIPTION & RECURRING EXPENSE AUDIT

**Suggested Endpoint:** `GET /api/v1/ai/subscription-audit`

### Purpose

Automatically discover recurring financial commitments.

### Detection

Analyze approximately 90 days of transactions for:

- Repeated merchants
- Similar transaction amounts
- Repeating time intervals
- Digital subscriptions
- Gym memberships
- Wi-Fi/utilities
- Rent
- SIP/investment payments

### Output

For each recurring expense:

- Merchant
- Typical amount
- Frequency
- Estimated monthly cost
- Estimated annual cost

### Recommendations

- Identify unused/idle subscriptions.
- Identify duplicate services.
- Suggest cancellation.
- Suggest annual-plan savings where appropriate.
- Suggest bundle optimization.

---

# 11. MICRO-SPENDING "LEAK HUNTER"

**Suggested Endpoint:** `GET /api/v1/ai/leak-analysis`

### Purpose

Detect small expenses that individually look insignificant but create a meaningful annual drain.

### Detection

Look for repeated small transactions, including transactions around or below **₹150**, while allowing the threshold to be configurable.

### Output

Example:

> ₹120 snacks × 15 times/month ≈ ₹1,800/month → ₹21,600/year

### Recommendations

Group leaks into:

- Food
- Delivery
- Convenience purchases
- Digital purchases
- Other recurring micro-expenses

---

# 12. SAFE-TO-SPEND SPEEDOMETER

**Suggested Endpoint:** `GET /api/v1/ai/safe-to-spend`

### Purpose

Give the user a real-time answer to:

> "How much can I safely spend today?"

### Calculations

- Remaining budget
- Days remaining
- Safe daily spending allowance
- Current daily burn rate
- Spending velocity
- Projected month-end spending
- Estimated depletion date
- Net savings trajectory

### Status

- Optimal
- Warning
- Danger

This extends the existing burn-rate functionality already present in the Financial Copilot.

---

# 13. 50/30/20 BUDGET OPTIMIZATION

### Purpose

Compare actual spending distribution against the 50/30/20 budgeting framework.

### Classification

**Needs — 50%**

Examples:

- Rent
- Groceries
- Utilities
- Healthcare
- Commute

**Wants — 30%**

Examples:

- Dining out
- Food delivery
- Shopping
- Entertainment
- Travel

**Savings — 20%**

Examples:

- Mutual funds
- SIPs
- Emergency savings

### UI

Provide a segmented progress meter:

- Actual distribution
- Target distribution
- Difference

### AI Recommendation

Give exact rebalancing advice.

Example:

> "Wants are 38% of your spending. Reduce discretionary spending by approximately ₹2,400 this month to move toward the target."

---

# 14. FINANCIAL HEALTH SCORE

### Score

**0–100**

### Purpose

Turn multiple financial metrics into one understandable indicator.

### Suggested pillars from the source documents

- Budget Adherence — 40 points
- Savings Velocity — 35 points
- Category Discipline — 25 points

### Output

Possible states:

- Excellent
- Good
- Needs Attention

The score should be calculated deterministically.

---

# 15. MONTHLY EXECUTIVE AI DIGEST

### Purpose

Create a monthly financial recap.

### Content

- Total income
- Total spending
- Savings
- Top spending categories
- Largest purchases
- Budget performance
- Subscription costs
- Major spending changes
- Achievements
- Problems
- Savings opportunities
- Strategic goals for next month

### UX

A visually engaging monthly summary inspired by a "year/month wrapped" experience.

---

# 16. VOICE EXPENSE PARSER

### Purpose

Allow the user to add an expense without typing.

### Example

User says:

> "Spent ₹450 on dinner yesterday."

Convert to:

- Amount: ₹450
- Category: Dining/Food
- Date: yesterday → exact date
- Description: dinner

### Flow

Voice → Speech-to-text → Existing expense parser → Structured draft → User confirmation → Save.

Voice should reuse the existing natural-language parser rather than implementing a second independent parsing system.

---

# 17. EXPENSE NOTE SENTIMENT ANALYSIS

**Suggested Endpoint:** `POST /api/v1/ai/sentiment`

### Purpose

Analyze the emotional context associated with an expense note.

### Sentiments

- Positive
- Neutral
- Negative

### Context Tags

- Celebration / Reward
- Necessary Routine
- Buyer's Remorse
- Stress Spending
- Emergency

### Example

> "Bought this because I was stressed after work."

→ Negative / Stress Spending

### Important

This should be presented as an informational behavioral insight, not a psychological diagnosis.

---

# 18. FINNY — FINANCIAL MOOD MASCOT

### Purpose

Make financial tracking more engaging.

Finny changes its visual state based on financial behavior.

### Example states

- 😊 Happy — spending is under control
- 🎉 Excited — savings milestone achieved
- 😟 Worried — approaching budget limit
- 😢 Sad — budget exceeded
- 💡 Helpful — useful insight available

### Integration

Finny can react to:

- Budget performance
- Savings milestones
- Overspending
- Unusual transactions
- Positive financial achievements
- AI recommendations

This is primarily a UX/engagement feature rather than a core financial calculation feature.

---

# 19. HINGLISH FINANCIAL ASSISTANT

Extend the Financial Copilot so users can ask questions in natural English and Hinglish.

### Examples

> "Is month sabse zyada paisa kaha gaya?"

> "Kya main ₹5000 ka watch afford kar sakta hoon?"

> "Mera shopping budget kitna bacha hai?"

The answer should still be grounded in deterministic financial data.

---

# 20. ACHIEVEMENT-BASED INSIGHTS

The AI should identify positive financial behavior, not only problems.

### Examples

- Stayed below food budget.
- Saved more than previous month.
- Reduced unnecessary spending.
- Reached savings goal.
- Maintained budget for multiple weeks.
- Reduced subscription costs.

These can feed into:

- Dashboard insights
- Financial Health Score
- Monthly Digest
- Finny

---

# 21. CONTEXTUAL ACTIONS IN AI CHAT

The Financial Copilot should be able to return useful actions alongside answers.

### Example

User:

> "Can I afford this ₹3,000 watch?"

AI:

> "Yes, but it would reduce your remaining discretionary budget to ₹4,200."

Actions:

- **Simulate Purchase**
- **Add Expense**
- **View Shopping Budget**

Actions should only execute after explicit user interaction.

---

# 22. RECEIPT LINE-ITEM EXTRACTION

Extend receipt OCR to optionally extract individual purchased items.

### Example

Receipt:

- Coffee — ₹180
- Sandwich — ₹250
- Dessert — ₹150

Output:

- Merchant
- Date
- Total
- Payment mode
- Line items
- Suggested category

This should remain optional because many users only need the transaction total.

---

# 23. BANK / UPI SMS PARSER

**Suggested Endpoint:** `POST /api/v1/ai/parse-receipt`

### Purpose

Convert Indian banking/UPI notification text into a transaction draft.

### Example

> "HDFC Bank: Rs 850.00 debited from a/c ****1234 on 02-Sep-26 to ZOMATO. UPI Ref 324156"

Extract:

- Amount
- Date
- Merchant
- Transaction type
- Payment method
- UPI reference where appropriate

### Privacy

Sensitive account/card information should be scrubbed before sending text to an external AI provider.

---

# 24. DUPLICATE TRANSACTION GUARD

### Purpose

Prevent the same transaction from being imported multiple times through:

- CSV
- UPI SMS
- Receipt OCR
- Manual entry
- AI Quick Add

### Matching signals

Use deterministic matching where possible:

- Amount
- Date/time
- Merchant
- Payment method
- Transaction/reference ID

The system should warn the user before creating a probable duplicate.

---

# 25. DEBIT / CREDIT AUTO CLASSIFICATION

Automatically determine whether an imported transaction represents:

- Debit / Expense
- Credit / Income

This is especially useful for:

- Bank statements
- UPI SMS
- Imported transaction data

The final classification should be deterministic where source data explicitly provides debit/credit information.

---

# 26. MULTI-MODEL / MULTI-PROVIDER RESILIENCE

Extend the current provider architecture to support automatic fallback.

### Example strategy

1. Primary configured provider/model
2. Secondary provider/model
3. Local deterministic heuristic engine

### Failure conditions

Fallback when:

- API quota is exceeded
- Rate limit occurs
- Provider temporarily fails
- Network unavailable
- API key is unavailable

The application should never depend on an LLM for core arithmetic or basic transaction parsing where deterministic logic can perform the task.

---

# 27. RECOMMENDED IMPLEMENTATION ORDER

Do not implement these randomly. Build them in dependency order.

## Phase 1 — Core Financial Intelligence

1. Dynamic AI Budget Advisor
2. Purchase Decision Simulator
3. Subscription & Recurring Expense Audit
4. Micro-Spending Leak Hunter
5. Safe-to-Spend Speedometer
6. Financial Health Score

## Phase 2 — Transaction Automation

7. Bank/UPI SMS Parser
8. Duplicate Transaction Guard
9. Debit/Credit Classification
10. Receipt Line-Item Extraction
11. Voice Expense Parser

## Phase 3 — Financial Intelligence UX

12. 50/30/20 Budget Optimization
13. Achievement-Based Insights
14. Monthly Executive AI Digest
15. Hinglish Financial Assistant
16. Contextual Chat Actions

## Phase 4 — Engagement

17. Expense Note Sentiment Analysis
18. Finny Financial Mascot

## Phase 5 — AI Infrastructure

19. Multi-model/provider rotation
20. Strengthen deterministic fallbacks
21. Add caching/rate limiting where required
22. Add automated tests for every new AI endpoint

---

# 28. API ROADMAP

| Method | Endpoint | Feature | Status |
|---|---|---|---|
| POST | `/api/v1/ai/parse-expense` | Natural language expense parser | ✅ Implemented |
| POST | `/api/v1/ai/scan-receipt` | Receipt/invoice OCR | ✅ Implemented |
| POST | `/api/v1/ai/categorize` | Real-time categorization | ✅ Implemented |
| GET | `/api/v1/ai/insights` | Financial Copilot & insights | ✅ Implemented |
| POST | `/api/v1/expenses/import` | Bank CSV import | ✅ Implemented |
| GET | `/api/v1/ai/suggest-budget` | Dynamic budget advisor | ⚙️ Planned |
| POST | `/api/v1/ai/simulate-purchase` | Purchase simulator | ⚙️ Planned |
| GET | `/api/v1/ai/subscription-audit` | Subscription audit | ⚙️ Planned |
| GET | `/api/v1/ai/leak-analysis` | Micro-spending leak hunter | ⚙️ Planned |
| GET | `/api/v1/ai/safe-to-spend` | Safe-to-spend speedometer | ⚙️ Planned |
| POST | `/api/v1/ai/sentiment` | Expense sentiment | ⚙️ Planned |
| POST | `/api/v1/ai/parse-receipt` | SMS/text receipt parser | ⚙️ Planned |

> Endpoint names for planned features are suggested based on the source documents and should be finalized against Paradox's existing API conventions before implementation.

---

# 29. IMPLEMENTATION RULES

1. Preserve the existing Paradox folder structure.
2. Keep all AI configuration environment-variable driven.
3. Never hardcode API keys, credentials, or secrets.
4. Never store `.env` in source control.
5. Do not transmit unnecessary PII to external AI providers.
6. Keep financial arithmetic deterministic.
7. Provide deterministic fallbacks for AI-dependent functionality where practical.
8. Reuse existing parsers and financial calculation services instead of creating duplicate logic.
9. Require explicit user confirmation before destructive or financial actions.
10. Add unit/integration tests for every new AI endpoint.
11. Keep AI features isolated behind service-layer abstractions.
12. Keep the system usable when external AI providers are unavailable.

---

# 30. FINAL TARGET

After implementing the planned features, Paradox should function as more than an expense tracker.

The target system should be able to:

- Capture expenses automatically from text, voice, receipts, and bank/UPI data.
- Categorize transactions intelligently.
- Detect recurring expenses and financial leaks.
- Predict budget exhaustion.
- Tell users how much they can safely spend.
- Simulate purchases before they happen.
- Recommend realistic budgets.
- Evaluate overall financial health.
- Explain spending behavior conversationally.
- Generate actionable savings recommendations.
- Summarize monthly financial performance.
- Detect positive and negative spending behavior.
- Provide an engaging financial experience without making core financial calculations dependent on an LLM.

The **implemented features are the baseline**. The **planned features form the remaining implementation roadmap**.
