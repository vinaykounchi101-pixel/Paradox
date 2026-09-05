# 🤖 Paradox — Complete AI Features & Architecture Documentation

Paradox includes a production-grade, dual-engine AI subsystem combining **Cloud LLMs / Multimodal Vision** with a **Deterministic Offline Heuristic Engine**. This subsystem automates expense entry, eliminates manual receipt typing, guards against financial mistakes, and delivers actionable financial intelligence.

---

## 🏛️ 1. Architecture: Graceful Degradation & Dual-Engine Design

The architecture is built on a **Zero-Failure** principle: if an external API key is missing, network is offline, or cloud rate limits are reached, the system gracefully falls back to deterministic local scoring without crashing or blocking the user.

```
                      ┌────────────────────────────────────────┐
                      │          User Input / Request          │
                      │ (Text, Voice, Camera, SMS, CSV, Image) │
                      └───────────────────┬────────────────────┘
                                          │
                                          ▼
                      ┌────────────────────────────────────────┐
                      │          Provider Resolution           │
                      │  (Gemini v1beta / OpenAI / Anthropic)  │
                      └───────────┬────────────────┬───────────┘
                                  │                │
                        API Key Present &         API Offline /
                        Network Connected        Rate-Limited / None
                                  │                │
                                  ▼                ▼
                     ┌────────────────────────┐  ┌────────────────────────┐
                     │   Cloud LLM / Vision   │  │ Deterministic Fallback │
                     │   gemini-flash-latest  │  │   Semantic Heuristic   │
                     │    gemini-2.5-flash    │  │  RegEx & Scoring Matrix│
                     │  Zero-Shot Structured  │  │  Zero-Latency Offline  │
                     └────────────────────────┘  └────────────────────────┘
```

### Privacy & Zero-PII Compliance
- No user passwords, emails, account numbers, or authentication tokens are ever sent to external LLM providers.
- Financial arithmetic (totals, percentages, burn rates, days remaining) is calculated deterministically in Python rather than relying on LLM hallucinations.

---

## 📋 2. Comprehensive Inventory of Implemented AI Features

| # | Feature Name | Primary Endpoint | UI Component / Location | Engine / Provider |
|---|:---|:---|:---|:---|
| 1 | **Natural Language Quick Add** | `POST /api/v1/ai/parse-expense` | `ExpenseFormDialog.tsx` | Gemini / Heuristic |
| 2 | **Multimodal Receipt OCR** | `POST /api/v1/ai/scan-receipt` | `ExpenseFormDialog.tsx` (Scan Bill) | Gemini Vision (`inlineData`) |
| 3 | **Smart Category Suggestion** | `POST /api/v1/ai/categorize` | Description field badge | Gemini / Heuristic |
| 4 | **1-Click Dynamic Category Add** | `POST /api/v1/categories/` | Inline prompt banner | Real-time Category Engine |
| 5 | **Indian Banking & UPI SMS Parser** | `POST /api/v1/ai/parse-sms` | `ExpenseFormDialog.tsx` (Paste SMS) | Regex Pattern Matcher |
| 6 | **Voice Expense Input** | *Client-Side Speech API* | Microphone button in Quick Add | Web Speech API (`en-IN`) |
| 7 | **Duplicate Transaction Guard** | `POST /api/v1/ai/check-duplicate` | Form amber alert card | Expense Repository / Heuristic |
| 8 | **Financial Copilot Insights** | `GET /api/v1/ai/insights` | `FinancialCopilotCard.tsx` | Gemini / Statistical Engine |
| 9 | **Purchase Decision Simulator** | `POST /api/v1/ai/simulate-purchase` | `CanIAffordThisModal.tsx` | Safe-Spend Algorithm |
| 10 | **Safe-to-Spend Speedometer** | `GET /api/v1/ai/safe-to-spend` | `SafeToSpendSpeedometer.tsx` | Burn Rate Math Model |
| 11 | **Financial Health Score (0-100)** | `GET /api/v1/ai/health-score` | `FinancialHealthScoreCard.tsx` | 3-Pillar Scoring Model |
| 12 | **Micro-Spending Leak Hunter** | `GET /api/v1/ai/leak-analysis` | `LeakHunterCard.tsx` | 90-day Frequency Clustering |
| 13 | **Subscription & Recurring Audit**| `GET /api/v1/ai/subscription-audit`| `SubscriptionAuditModal.tsx` | Overlap & Cadence Detector |
| 14 | **50/30/20 Budget Optimization** | `GET /api/v1/ai/fifty-thirty-twenty`| `FiftyThirtyTwentyCard.tsx` | Macro Framework Classifier |
| 15 | **Gamified Streaks & Badges** | `GET /api/v1/ai/achievements` | `AchievementsCard.tsx` | Milestone & Streak Engine |
| 16 | **Dynamic AI Budget Advisor** | `GET /api/v1/ai/suggest-budget` | Budget Settings / Advisor | Historical Regression Model |
| 17 | **Bank Statement CSV Categorizer**| `POST /api/v1/expenses/import` | `/expenses` Import Modal | Heuristic Matrix Matcher |
| 18 | **Multi-Line Bill Parser** | `POST /api/v1/ai/parse-receipt` | Receipt Itemizer API | Heuristic Tokenizer |
| 19 | **Financial Assistant (RAG Chat)** | `POST /api/v1/ai/chat` | `FinancialAssistantChat.tsx` | Gemini / Live User Context RAG |
| 20 | **Spending Spikes & Anomalies** | `GET /api/v1/ai/anomalies` | `AnomalyForecastCard.tsx` | Std-Deviation Outlier Detector |
| 21 | **30-Day Predictive Forecast** | `GET /api/v1/ai/forecast` | `AnomalyForecastCard.tsx` | Rolling Trajectory Engine |
| 22 | **Goal Savings Plan Optimizer** | `POST /api/v1/ai/savings-plan` | `SavingsPlannerDialog.tsx` | Category Discretionary Cuts |
| 23 | **Sentiment / Remorse Guard** | `POST /api/v1/ai/analyze-sentiment` | `ExpenseFormDialog.tsx` | Behavioral Sentiment Engine |
| 24 | **Paradox Monthly Wrapped** | `GET /api/v1/ai/monthly-wrapped` | `MonthlyWrappedModal.tsx` | Spotify-Story Archetypes |
| 25 | **Finny Vibe Check & Roast Mode**| `GET /api/v1/ai/vibe-check` | `FinnyMascot.tsx` | Hinglish / Reality Check |

---

## 🔍 3. Detailed Feature Breakdown

### 1. Natural Language Quick Add & Expense Parser
- **Endpoint:** `POST /api/v1/ai/parse-expense`
- **Frontend:** Top of Record Expense modal (`ExpenseFormDialog.tsx`).
- **What it does:**
  - Converts conversational sentences into clean transaction drafts:
    - *"Paid 450 for Zomato pizza via UPI yesterday"*
    - *"Metro 40 cash"*
    - *"Starbucks coffee 250 credit card 2 days ago"*
  - **Relative Date Engine:** Accurately converts `yesterday`, `day before yesterday`, `N days ago`, `N weeks ago`, `N months ago` into ISO dates (`YYYY-MM-DD`).
  - **Merchant Cleaning:** Strips noise words (`paid for`, `via`, `on`, `for`) to preserve pure item/vendor names (`Zomato pizza`, `Metro`).

### 2. Multimodal Receipt & Invoice OCR Scanner (Gemini Vision)
- **Endpoint:** `POST /api/v1/ai/scan-receipt`
- **Frontend:** `[ 📷 Scan Bill ]` button in Record Expense modal.
- **What it does:**
  - **High-Speed Preprocessor:** HTML5 canvas resizes large camera photos (5–12MB) to 1280px JPEG (~120KB) in ~50ms before upload.
  - **Google Gemini REST API Integration:** Utilizes Google's v1beta `inlineData` schema with active models (`gemini-flash-latest`, `gemini-2.5-flash`).
  - **Auto-Fill Extraction:** Automatically extracts total amount, transaction date, merchant name, category, and payment method (Cash, Card, UPI), filling the entire form in 1-2 seconds.

### 3. Real-Time Category Recommendation & 1-Click Expansion
- **Endpoint:** `POST /api/v1/ai/categorize`
- **Frontend:** Reactive banner directly below the Description input.
- **What it does:**
  - Debounced background evaluation as the user types (e.g., *"Uber to airport"* -> `Transportation`).
  - **Existing Category Match:** Displays `AI Suggests: [Category] [ Apply ]`.
  - **Dynamic Missing Category Add:** If the suggested category doesn't exist in the user's category list (e.g. user only has Food, but types *"Pet vaccination"*), it prompts:
    `✨ Category "Pets" doesn't exist yet. [ + Add & Select ] [ ✕ ]`
    Clicking creates it in the database and selects it immediately without navigating away.

### 4. Indian Banking & UPI SMS Transaction Parser
- **Endpoint:** `POST /api/v1/ai/parse-sms`
- **Frontend:** `[ 💬 Paste SMS ]` toggle drawer in Record Expense modal.
- **What it does:**
  - Recognizes standard Indian bank debit SMS formats:
    - `HDFC Bank: Rs 850.00 debited from a/c **1234 on 02-Sep-26 to ZOMATO. UPI Ref 324156`
    - `SBI: INR 1,200.00 debited by UPI transfer to DMART on 01/09/2026`
    - `Axis Bank: Card ending 9876 spent Rs 2,499 at CROMA`
  - Extracts exact amount, merchant, date, and payment mode (UPI, Debit Card, Credit Card) into the form.

### 5. Hands-Free Voice Quick Add
- **Frontend:** Embedded microphone button inside the AI Quick Add input field.
- **What it does:**
  - Built on Web Speech API configured with `en-IN` (Indian English).
  - Listens to user voice, shows real-time pulse indicator, transcripts text into the box, and automatically triggers the AI parser.

### 6. Duplicate Transaction Guard
- **Endpoint:** `POST /api/v1/ai/check-duplicate`
- **Frontend:** Amber warning card before submission.
- **What it does:**
  - Checks if an identical amount and similar description was already recorded within a ±2 day window.
  - Alerts user with exact date and category of previous transaction to prevent accidental duplicate entries.

### 7. Financial Copilot & AI Insights
- **Endpoint:** `GET /api/v1/ai/insights?period=current_month|last_30_days|current_week`
- **Frontend:** `FinancialCopilotCard.tsx` on Dashboard.
- **What it does:**
  - Calculates daily burn velocity (`₹XXX / day`) and projected end-of-period expenditure.
  - Classifies health status (`Healthy`, `Caution`, `Critical`) with custom alerts and savings tips.

### 8. Purchase Decision Simulator ("Can I Afford This?")
- **Endpoint:** `POST /api/v1/ai/simulate-purchase`
- **Frontend:** `CanIAffordThisModal.tsx`.
- **What it does:**
  - Simulates the impact of an impending purchase before spending money.
  - Outputs verdict: `Safe` (green), `Caution` (yellow), or `Over Budget` (red).
  - Shows remaining daily safe-to-spend allowance before vs. after the purchase.

### 9. Safe-to-Spend Daily Speedometer
- **Endpoint:** `GET /api/v1/ai/safe-to-spend`
- **Frontend:** `SafeToSpendSpeedometer.tsx`.
- **What it does:**
  - Calculates remaining uncommitted budget divided by remaining days in the month.
  - Calculates exact budget depletion date if current burn rate continues.

### 10. Financial Health Score (0–100)
- **Endpoint:** `GET /api/v1/ai/health-score`
- **Frontend:** `FinancialHealthScoreCard.tsx`.
- **What it does:**
  - Computes a holistic score across 3 pillars:
    - **Budget Adherence (40 pts):** Pacing against overall spending limit.
    - **Savings Velocity (30 pts):** Surplus unspent ratio.
    - **Category Discipline (30 pts):** Even distribution without overspending single categories.

### 11. Micro-Spending Leak Hunter
- **Endpoint:** `GET /api/v1/ai/leak-analysis?threshold=150`
- **Frontend:** `LeakHunterCard.tsx`.
- **What it does:**
  - Surfaces high-frequency small transactions (e.g. <= ₹150) across chai, snacks, quick deliveries.
  - Computes monthly total and annualized compound drain (e.g. ₹150/day = ₹54,750/year).

### 12. Subscription & Recurring Expense Audit
- **Endpoint:** `GET /api/v1/ai/subscription-audit`
- **Frontend:** `SubscriptionAuditModal.tsx`.
- **What it does:**
  - Auto-identifies repeating monthly/quarterly charges (Netflix, Spotify, Gym, AWS, etc.).
  - Flags overlapping/redundant services within the same category.

### 13. 50/30/20 Budget Framework Optimizer
- **Endpoint:** `GET /api/v1/ai/fifty-thirty-twenty`
- **Frontend:** `FiftyThirtyTwentyCard.tsx`.
- **What it does:**
  - Classifies spending into **Needs (50%)**, **Wants (30%)**, and **Savings (20%)**.
  - Provides adherence scores and rebalancing recommendations.

### 14. Gamified Discipline Streaks & Achievements
- **Endpoint:** `GET /api/v1/ai/achievements`
- **Frontend:** `AchievementsCard.tsx`.
- **What it does:**
  - Tracks consecutive daily logging streaks.
  - Unlocks bronze, silver, gold, and diamond achievement badges with financial motivation quotes.

### 15. Intelligent Bank Statement CSV Auto-Categorizer
- **Endpoint:** `POST /api/v1/expenses/import`
- **Frontend:** `/expenses` -> Import Statement button.
- **What it does:**
  - Ingests CSV statement files from any major Indian bank.
  - Automatically identifies Date, Debit Amount, and Narration columns.
  - Classifies all rows using the AI categorization matrix in a single batch.

---

## ⚙️ 4. Configuration & Environment Variables

All AI capabilities are environment-variable driven:

| Environment Variable | Description | Recommended Value |
| :--- | :--- | :--- |
| `GEMINI_API_KEY` | Google AI Studio API Key | Set in backend `.env` |
| `AI_PROVIDER` | Provider selection override (`auto`, `gemini`, `openai`, `anthropic`, `heuristic`) | `auto` |
| `AI_MODEL` | Custom model override | `gemini-flash-latest` |
| `OPENAI_API_KEY` | Optional OpenAI fallback key | Optional |
| `ANTHROPIC_API_KEY` | Optional Anthropic fallback key | Optional |

### Active Google Gemini Models Supported
- `gemini-flash-latest` (Primary default)
- `gemini-2.5-flash`
- `gemini-flash-lite-latest`
- `gemini-2.5-flash-lite`
- `gemini-2.0-flash`

---

## 🧪 5. Automated Test Coverage

All AI modules have automated unit test suites in `backend/tests/`:

- **Core AI Unit Tests:** `backend/tests/unit/test_ai.py`
  - Heuristic categorization scoring
  - Heuristic expense parser & date calculations
  - Gemini API mocking and fallback behavior
  - Missing category suggestion (`is_new_category`)
  - Multimodal Vision OCR receipt scanner
- **Phase 1 Financial Intelligence Tests:** `backend/tests/unit/test_ai_features_phase1.py`
  - Purchase decision simulator (`safe`, `caution`, `over_budget`)
  - Safe-to-spend allowance & depletion dates
  - Financial health score boundary tests
  - Micro-spending leak analysis
  - Subscription audit & cadence detection
- **Phase 3 Final Intelligence Tests:** `backend/tests/unit/test_ai_features_final.py`
  - Financial assistant RAG and spending inquiry
  - Spending anomaly outlier detection
  - 30-day predictive spending forecast
  - Goal-based savings planner category cuts
  - Behavioral sentiment & buyer's remorse reflection
  - Monthly wrapped storytelling and persona archetypes
  - Finny burn-rate vibe check & Hinglish roast mode

To run all AI test suites:
```bash
cd backend
.venv/Scripts/pytest tests/unit/test_ai* -v
```
Output:
```
============================= 31 passed in 1.45s ==============================
```
Total project test suite: **57 / 57 passed** cleanly with 0 failures.
