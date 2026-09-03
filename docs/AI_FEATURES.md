# 🤖 Paradox — AI Features & Architecture Documentation

Paradox includes a dual-engine AI subsystem (Cloud LLM / Vision + Deterministic Local Heuristics) designed to automate personal expense tracking, eliminate manual data entry, and provide actionable financial intelligence.

---

## 🏛️ AI Architecture & Hybrid Dual-Engine Design

The AI architecture follows a **graceful degradation** model:

```
                  ┌─────────────────────────────────────┐
                  │          User Input / Request       │
                  │   (Text / Image / CSV / Statement)  │
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │        Provider Resolution          │
                  │     (Gemini 3.6 Flash / OpenAI)     │
                  └──────┬───────────────────────┬──────┘
                         │                       │
                API Key Present &               API Offline /
                Network Available              Rate-Limited / Mock
                         │                       │
                         ▼                       ▼
            ┌────────────────────────┐  ┌────────────────────────┐
            │   Cloud LLM / Vision   │  │ Deterministic Fallback │
            │    Gemini 3.6 Flash    │  │   Semantic Heuristic   │
            │   Zero-Shot Structured │  │  RegEx & Scoring Engine │
            └────────────────────────┘  └────────────────────────┘
```

1. **Primary Cloud Engine**: Powered by Google's latest **Gemini 3.6 Flash** (and `gemini-flash-latest`), providing ultra-low latency structured JSON generation and multimodal Vision OCR.
2. **Local Heuristic Engine**: Zero-dependency, offline-capable fallback with regex tokenizers and an extensive keyword scoring matrix covering Indian fintech (UPI, Paytm, PhonePe, GPay, NEFT, IMPS), e-commerce (Amazon, Flipkart, Blinkit, Zepto, Swiggy, Zomato), utilities, and hardware.

---

## 🚀 Implemented AI Features

### 1. Natural Language Quick Add & Expense Parser
- **Endpoint**: `POST /api/v1/ai/parse-expense`
- **Location**: [`backend/app/services/ai_service.py`](backend/app/services/ai_service.py) -> `parse_expense_text`
- **UI Location**: Top of Expense Dialog ("AI Quick Add")
- **Capabilities**:
  - Understands conversational and natural language sentences:
    - *"Paid 1000 for a new gaming mouse via upi 2 days ago"*
    - *"Bought groceries ₹1200 on Credit Card yesterday"*
    - *"Starbucks coffee 250 cash"*
    - *"Netflix subscription 649 net banking 3 weeks ago"*
  - **Relative Date Engine**: Resolves `"N days ago"`, `"N weeks ago"`, `"N months ago"`, `"yesterday"`, and `"day before yesterday"` into exact ISO dates (`YYYY-MM-DD`).
  - **Description Cleansing**: Strips out payment methods, amounts, relative dates, and prepositions (`paid for`, `via`, `on`), leaving pure merchant/item names (e.g. `"A new gaming mouse"`).
  - **Category & Payment Method Matcher**: Accurately maps to existing user categories and payment modes.

---

### 2. Multimodal Receipt & Invoice OCR Scanner
- **Endpoint**: `POST /api/v1/ai/scan-receipt`
- **Location**: [`backend/app/services/ai_service.py`](backend/app/services/ai_service.py) -> `scan_receipt_image`
- **UI Location**: Expense Form Dialog -> **"📷 Scan Bill"** button
- **Capabilities**:
  - **High-Speed Client-Side Preprocessor**: Built with an HTML5 `<canvas>` downscaler ([`ExpenseFormDialog.tsx`](frontend/src/features/expenses/components/ExpenseFormDialog.tsx)) that resizes 5MB–10MB high-resolution camera photos to a clear 1280px JPEG (~120KB) in ~50ms.
  - **Multimodal Vision OCR**: Powered by Gemini 3.6 Flash Vision.
  - **Extracted Fields**:
    - Merchant / Store name
    - Total final amount paid
    - Transaction date
    - Matching category
    - Payment method
  - **Instant Auto-Fill**: All form fields, amounts, and category pickers are populated in 1-2 seconds with zero network timeouts.

---

### 3. Real-Time Description Category Suggestion
- **Endpoint**: `POST /api/v1/ai/categorize`
- **Location**: [`backend/app/services/ai_service.py`](backend/app/services/ai_service.py) -> `categorize_expense`
- **UI Location**: Real-time suggestion banner below Description input in Expense Dialog
- **Capabilities**:
  - As the user types any transaction description (e.g. *"Uber to office"*, *"Apollo pharmacy tablets"*, *"DMart grocery shopping"*), the AI categorizes the transaction on-the-fly.
  - Displays a glassmorphic badge: `AI Suggests: [Category Name] (Reason)` with a 1-click **"Apply"** button.
  - Handles canonical categories and synonym aliases (e.g. maps "Food" -> "Food & Dining", "Grocery" -> "Groceries").

---

### 4. Financial Copilot & AI Insights
- **Endpoint**: `GET /api/v1/ai/insights?period=current_month|last_30_days|current_week`
- **Location**: [`backend/app/services/ai_service.py`](backend/app/services/ai_service.py) -> `generate_insights`
- **UI Location**: [`FinancialCopilotCard.tsx`](frontend/src/features/dashboard/components/FinancialCopilotCard.tsx) on Dashboard
- **Capabilities**:
  - **Health Assessment**: Evaluates spending velocity against budget limits (`Healthy`, `Warning`, `Critical`).
  - **Daily Burn Velocity**: Calculates real-time daily burn rate (`₹XXX.XX / day`).
  - **Projected Period Total**: Projects total anticipated spend at current velocity.
  - **Observations & Alerts**: Detects spikes, unusual discretionary spending, and category over-utilization.
  - **Tailored Savings Tips**: Generates concrete, practical suggestions to restore budget balance.

---

### 5. Intelligent Bank Statement CSV Import
- **Endpoint**: `POST /api/v1/expenses/import`
- **Location**: [`backend/app/services/expense_service.py`](backend/app/services/expense_service.py) -> `import_expenses_csv`
- **UI Location**: `/expenses` page -> **"📤 Import Statement"** button
- **Capabilities**:
  - Accepts standard `.csv` statements from major banks (HDFC, SBI, ICICI, Axis, Kotak, etc.).
  - Header normalization automatically discovers Date, Amount (debit/withdrawal), and Narration columns.
  - Runs AI transaction classification across all statement rows in bulk, assigning appropriate categories without requiring manual tagging.

---

### 6. Dynamic AI Budget Advisor
- **Endpoint**: `GET /api/v1/ai/suggest-budget?period_type=month|week|day`
- **Location**: [`backend/app/services/ai_service.py`](backend/app/services/ai_service.py) -> `suggest_budget`
- **Capabilities**:
  - Analyzes historical transactions, spending trend velocity, and category allocations.
  - Recommends an optimal budget target with a breakdown per category and reasoned advice.

---

### 7. Multi-Line Receipt & SMS Transaction Parser
- **Endpoint**: `POST /api/v1/ai/parse-receipt`
- **Location**: [`backend/app/services/ai_service.py`](backend/app/services/ai_service.py) -> `parse_receipt_text`
- **Capabilities**:
  - Parses bank debit SMS notifications:
    - *"HDFC Bank: Rs 850.00 debited from a/c **1234 on 02-Sep-26 to ZOMATO. UPI Ref 324156"*
  - Parses multi-item text receipts into itemized rows, merchant name, and total.

---

## 🛠️ Configuration & Customization

All AI features are environment-variable driven:

| Environment Variable | Description | Default |
| :--- | :--- | :--- |
| `GEMINI_API_KEY` | Google Gemini API key for Vision & LLM tasks | *(System configured)* |
| `OPENAI_API_KEY` | Optional OpenAI API key (`gpt-4o-mini`) | `None` |
| `ANTHROPIC_API_KEY` | Optional Anthropic API key (`claude-3-haiku`) | `None` |
| `AI_PROVIDER` | Explicit provider override (`gemini`, `openai`, `anthropic`, `heuristic`) | Auto-resolved |
| `AI_MODEL` | Custom model name override | `gemini-3.6-flash` |

---

## 🧪 Testing & Verification

All AI functions include complete automated test suites:
- Run all AI unit tests:
  ```powershell
  .venv\Scripts\pytest tests/unit/test_ai.py -v
  ```
- Test Coverage:
  - Heuristic categorization scoring
  - Heuristic parse with relative dates & currency symbols
  - Gemini Vision OCR JSON schema adherence
  - Fallback mechanisms during API downtime
