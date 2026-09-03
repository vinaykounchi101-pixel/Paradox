import json
import logging
import re
from datetime import date, datetime, timedelta
from decimal import Decimal
from typing import Any, Dict, List, Optional, Tuple

import httpx

from app.core.config import settings
from app.schemas.ai import (
    AIInsightsResponse,
    AuditSubscriptionItem,
    CategorizeResponse,
    CategoryAllocation,
    FinancialHealthScoreResponse,
    HealthScorePillar,
    LeakAnalysisResponse,
    ParsedReceiptItem,
    ParseExpenseResponse,
    ParseReceiptResponse,
    SafeToSpendResponse,
    SimulatePurchaseResponse,
    SpendingLeakItem,
    SubscriptionAuditResponse,
    SuggestBudgetResponse,
)

logger = logging.getLogger(__name__)

DEFAULT_SYSTEM_CATEGORIES = [
    "Food & Dining",
    "Transportation",
    "Shopping",
    "Entertainment",
    "Bills & Utilities",
    "Utilities",
    "Health",
    "Healthcare",
    "Education",
    "Groceries",
    "Housing",
    "Personal Care",
    "Travel",
    "Investments",
    "Uncategorized",
    "Other",
]

DEFAULT_PAYMENT_METHODS = [
    "Cash",
    "Debit Card",
    "Credit Card",
    "UPI",
    "Digital Wallet",
    "Bank Transfer",
    "Net Banking",
    "Wallet",
    "Other",
]

HEURISTIC_KEYWORD_MAP = {
    "Food & Dining": [
        "food", "dining", "restaurant", "cafe", "coffee", "tea", "chai", "lunch", "dinner",
        "breakfast", "snack", "swiggy", "zomato", "mcdonald", "burger", "pizza", "starbucks",
        "biryani", "bakery", "bread", "meal", "eats"
    ],
    "Groceries": [
        "grocery", "groceries", "supermarket", "blinkit", "zepto", "instamart",
        "fruits", "vegetables", "milk", "bread", "eggs", "veggies", "provisions", "mart"
    ],
    "Transportation": [
        "transport", "taxi", "cab", "uber", "ola", "auto", "rickshaw", "metro", "bus",
        "train", "flight", "fuel", "petrol", "diesel", "cng", "parking", "toll", "fastag"
    ],
    "Bills & Utilities": [
        "utility", "utilities", "bill", "electricity", "water", "gas", "wifi", "internet", "broadband", "mobile",
        "recharge", "phone bill", "cylinder", "dth", "power", "pipeline"
    ],
    "Utilities": [
        "utility", "electricity", "water", "gas", "wifi", "internet", "broadband", "mobile",
        "recharge", "phone bill", "cylinder", "dth"
    ],
    "Health": [
        "health", "doctor", "hospital", "clinic", "medicine", "pharmacy", "medical", "test", "dentist",
        "apollo", "pharmeasy", "tablets", "syrup", "healthcare"
    ],
    "Healthcare": [
        "doctor", "hospital", "clinic", "medicine", "pharmacy", "medical", "test", "dentist",
        "apollo", "pharmeasy", "health"
    ],
    "Housing": [
        "rent", "maintenance", "mortgage", "repair", "plumber", "electrician", "furniture",
        "maid", "cook"
    ],
    "Entertainment": [
        "movie", "cinema", "netflix", "prime", "hotstar", "spotify", "concert", "game", "games",
        "gaming", "steam", "youtube", "theatre", "party", "club", "playstation", "xbox", "console"
    ],
    "Shopping": [
        "clothes", "shoes", "amazon", "flipkart", "myntra", "shopping", "mall", "electronics",
        "gadget", "appliances", "book", "gift", "dress", "mouse", "keyboard", "gaming mouse",
        "monitor", "laptop", "pc", "computer", "hardware", "accessories", "headphones", "headset",
        "gaming", "game", "controller", "tech", "device", "ipad", "watch"
    ],
    "Personal Care": [
        "salon", "spa", "haircut", "gym", "fitness", "cosmetics", "skincare", "massage"
    ],
    "Education": [
        "tuition", "course", "udemy", "coursera", "school", "college", "fees", "books", "training"
    ],
    "Travel": [
        "hotel", "airbnb", "resort", "vacation", "trip", "tour", "visa", "luggage"
    ],
    "Investments": [
        "stocks", "mutual fund", "sip", "crypto", "shares", "gold", "fixed deposit"
    ],
    "Uncategorized": [
        "other", "misc", "miscellaneous", "general", "extra"
    ],
    "Other": [
        "other", "misc", "general"
    ],
}

HEURISTIC_PM_MAP = {
    "Digital Wallet": ["digital wallet", "wallet", "upi", "gpay", "google pay", "phonepe", "paytm", "bhim", "scan", "amazon pay", "apple pay"],
    "UPI": ["upi", "gpay", "google pay", "phonepe", "paytm", "bhim", "scan"],
    "Wallet": ["wallet", "digital wallet", "amazon pay", "paytm wallet", "apple pay"],
    "Credit Card": ["credit card", "credit", "cc"],
    "Debit Card": ["debit card", "debit", "dc", "atm card"],
    "Bank Transfer": ["bank transfer", "net banking", "netbanking", "neft", "rtgs", "imps", "wire", "transfer"],
    "Net Banking": ["net banking", "netbanking", "neft", "rtgs", "imps", "bank transfer"],
    "Cash": ["cash", "notes", "currency"],
    "Other": ["other"],
}


class AIService:
    def __init__(self):
        self.provider_setting = (settings.AI_PROVIDER or "auto").lower().strip()
        self.gemini_key = settings.GEMINI_API_KEY
        self.openai_key = settings.OPENAI_API_KEY
        self.anthropic_key = settings.ANTHROPIC_API_KEY
        self.custom_model = settings.AI_MODEL

    def resolve_provider(self) -> str:
        """
        Determines the active AI provider based on configuration and available API keys.
        Returns: 'gemini', 'openai', 'anthropic', or 'heuristic'
        """
        if self.provider_setting == "gemini" and self.gemini_key:
            return "gemini"
        if self.provider_setting == "openai" and self.openai_key:
            return "openai"
        if self.provider_setting == "anthropic" and self.anthropic_key:
            return "anthropic"

        if self.provider_setting == "auto":
            if self.gemini_key:
                return "gemini"
            if self.openai_key:
                return "openai"
            if self.anthropic_key:
                return "anthropic"

        return "heuristic"

    # =========================================================================
    # 1. Categorization Recommendation
    # =========================================================================

    async def categorize_expense(
        self, description: str, available_categories: Optional[List[str]] = None
    ) -> CategorizeResponse:
        cats = available_categories if available_categories else DEFAULT_SYSTEM_CATEGORIES
        provider = self.resolve_provider()

        if provider == "gemini":
            try:
                res = await self._call_gemini_categorize(description, cats)
                if res:
                    return res
            except Exception as exc:
                logger.warning("Gemini categorization call failed (%s), falling back to heuristic", exc)

        elif provider == "openai":
            try:
                res = await self._call_openai_categorize(description, cats)
                if res:
                    return res
            except Exception as exc:
                logger.warning("OpenAI categorization call failed (%s), falling back to heuristic", exc)

        elif provider == "anthropic":
            try:
                res = await self._call_anthropic_categorize(description, cats)
                if res:
                    return res
            except Exception as exc:
                logger.warning("Anthropic categorization call failed (%s), falling back to heuristic", exc)

        return self._heuristic_categorize(description, cats)

    # =========================================================================
    # 2. Natural Language Expense Parser
    # =========================================================================

    async def parse_expense_text(
        self,
        text: str,
        available_categories: Optional[List[str]] = None,
        available_payment_methods: Optional[List[str]] = None,
    ) -> ParseExpenseResponse:
        cats = available_categories if available_categories else DEFAULT_SYSTEM_CATEGORIES
        pms = available_payment_methods if available_payment_methods else DEFAULT_PAYMENT_METHODS
        provider = self.resolve_provider()

        if provider == "gemini":
            try:
                res = await self._call_gemini_parse(text, cats, pms)
                if res:
                    return res
            except Exception as exc:
                logger.warning("Gemini parse call failed (%s), falling back to heuristic", exc)

        elif provider == "openai":
            try:
                res = await self._call_openai_parse(text, cats, pms)
                if res:
                    return res
            except Exception as exc:
                logger.warning("OpenAI parse call failed (%s), falling back to heuristic", exc)

        elif provider == "anthropic":
            try:
                res = await self._call_anthropic_parse(text, cats, pms)
                if res:
                    return res
            except Exception as exc:
                logger.warning("Anthropic parse call failed (%s), falling back to heuristic", exc)

        return self._heuristic_parse(text, cats, pms)

    # =========================================================================
    # Google Gemini Implementation
    # =========================================================================

    async def _call_gemini_categorize(
        self, description: str, categories: List[str]
    ) -> Optional[CategorizeResponse]:
        model = self.custom_model or "gemini-3.6-flash"
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={self.gemini_key}"

        prompt = (
            f"You are an expert personal finance categorizer for the Paradox expense tracker.\n"
            f"Allowed categories: {json.dumps(categories)}\n"
            f"Expense description: \"{description}\"\n"
            f"Respond strictly in valid JSON format with keys: \"category_name\" (one of the allowed categories), "
            f"\"confidence\" (float 0.0 to 1.0), \"reasoning\" (one brief sentence)."
        )

        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
            }
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload)
            if resp.status_code == 200:
                data = resp.json()
                content = data["candidates"][0]["content"]["parts"][0]["text"]
                parsed = json.loads(content)
                cat = self._match_closest(parsed.get("category_name"), categories)
                return CategorizeResponse(
                    category_name=cat,
                    confidence=float(parsed.get("confidence", 0.85)),
                    reasoning=parsed.get("reasoning"),
                    provider_used="gemini",
                )
        return None

    async def _call_gemini_parse(
        self, text: str, categories: List[str], payment_methods: List[str]
    ) -> Optional[ParseExpenseResponse]:
        model = self.custom_model or "gemini-3.6-flash"
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={self.gemini_key}"
        today_str = date.today().isoformat()

        prompt = (
            f"You are an expert expense parsing assistant for the Paradox personal finance app. Today is {today_str}.\n"
            f"Allowed categories: {json.dumps(categories)}\n"
            f"Allowed payment methods: {json.dumps(payment_methods)}\n"
            f"User input text: \"{text}\"\n"
            f"Extract expense information strictly into JSON with fields:\n"
            f"- \"amount\": numeric float or null\n"
            f"- \"category_name\": exact matching string from allowed categories or null\n"
            f"- \"payment_method_name\": exact matching string from allowed payment methods or null\n"
            f"- \"date\": ISO date YYYY-MM-DD or null\n"
            f"- \"description\": concise merchant or purchased item ONLY (e.g. for \"Paid 450 for Zomato pizza via UPI yesterday\", description must be \"Zomato pizza\" or \"Zomato\". NEVER include amount, payment method, date keywords like 'yesterday', or prepositions like 'paid for', 'via').\n"
            f"- \"confidence\": float 0.0 to 1.0\n"
            f"- \"reasoning\": one brief sentence\n\n"
            f"Examples:\n"
            f"- \"Paid 450 for Zomato pizza via UPI yesterday\" -> amount: 450, category_name: \"Food & Dining\", payment_method_name: \"UPI\", description: \"Zomato pizza\"\n"
            f"- \"Petrol 500 cash\" -> amount: 500, category_name: \"Transportation\", payment_method_name: \"Cash\", description: \"Petrol\"\n"
            f"- \"Starbucks coffee 250 credit card\" -> amount: 250, category_name: \"Food & Dining\", payment_method_name: \"Credit Card\", description: \"Starbucks coffee\""
        )

        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
            }
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload)
            if resp.status_code == 200:
                data = resp.json()
                content = data["candidates"][0]["content"]["parts"][0]["text"]
                parsed = json.loads(content)
                amount = Decimal(str(parsed["amount"])) if parsed.get("amount") is not None else None
                cat = self._match_closest(parsed.get("category_name"), categories) if parsed.get("category_name") else None
                pm = self._match_closest(parsed.get("payment_method_name"), payment_methods) if parsed.get("payment_method_name") else None

                return ParseExpenseResponse(
                    amount=amount,
                    category_name=cat,
                    payment_method_name=pm,
                    date=parsed.get("date") or today_str,
                    description=parsed.get("description"),
                    confidence=float(parsed.get("confidence", 0.9)),
                    reasoning=parsed.get("reasoning"),
                    provider_used="gemini",
                )
        return None

    # =========================================================================
    # OpenAI Implementation
    # =========================================================================

    async def _call_openai_categorize(
        self, description: str, categories: List[str]
    ) -> Optional[CategorizeResponse]:
        model = self.custom_model or "gpt-4o-mini"
        url = "https://api.openai.com/v1/chat/completions"

        system_msg = (
            f"You are a finance categorization assistant for Paradox.\n"
            f"Allowed categories: {json.dumps(categories)}\n"
            f"Output JSON with keys: category_name, confidence, reasoning."
        )

        payload = {
            "model": model,
            "messages": [
                {"role": "system", "content": system_msg},
                {"role": "user", "content": f"Expense description: {description}"}
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.1,
        }

        headers = {
            "Authorization": f"Bearer {self.openai_key}",
            "Content-Type": "application/json"
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload, headers=headers)
            if resp.status_code == 200:
                data = resp.json()
                content = data["choices"][0]["message"]["content"]
                parsed = json.loads(content)
                cat = self._match_closest(parsed.get("category_name"), categories)
                return CategorizeResponse(
                    category_name=cat,
                    confidence=float(parsed.get("confidence", 0.85)),
                    reasoning=parsed.get("reasoning"),
                    provider_used="openai",
                )
        return None

    async def _call_openai_parse(
        self, text: str, categories: List[str], payment_methods: List[str]
    ) -> Optional[ParseExpenseResponse]:
        model = self.custom_model or "gpt-4o-mini"
        url = "https://api.openai.com/v1/chat/completions"
        today_str = date.today().isoformat()

        system_msg = (
            f"You are an expense parsing assistant for the Paradox personal finance tracker. Today is {today_str}.\n"
            f"Allowed categories: {json.dumps(categories)}\n"
            f"Allowed payment methods: {json.dumps(payment_methods)}\n"
            f"Rules for \"description\": Must be ONLY the concise merchant or purchased item name (e.g. for 'Paid 450 for Zomato pizza via UPI yesterday', description must be 'Zomato pizza' or 'Zomato'). Do NOT repeat amounts, payment methods, date keywords like 'yesterday', or prepositions like 'paid for', 'via'.\n"
            f"Output JSON with keys: amount, category_name, payment_method_name, date, description, confidence, reasoning."
        )

        payload = {
            "model": model,
            "messages": [
                {"role": "system", "content": system_msg},
                {"role": "user", "content": text}
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.1,
        }

        headers = {
            "Authorization": f"Bearer {self.openai_key}",
            "Content-Type": "application/json"
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload, headers=headers)
            if resp.status_code == 200:
                data = resp.json()
                content = data["choices"][0]["message"]["content"]
                parsed = json.loads(content)
                amount = Decimal(str(parsed["amount"])) if parsed.get("amount") is not None else None
                cat = self._match_closest(parsed.get("category_name"), categories) if parsed.get("category_name") else None
                pm = self._match_closest(parsed.get("payment_method_name"), payment_methods) if parsed.get("payment_method_name") else None

                return ParseExpenseResponse(
                    amount=amount,
                    category_name=cat,
                    payment_method_name=pm,
                    date=parsed.get("date") or today_str,
                    description=parsed.get("description"),
                    confidence=float(parsed.get("confidence", 0.9)),
                    reasoning=parsed.get("reasoning"),
                    provider_used="openai",
                )
        return None

    # =========================================================================
    # Anthropic Claude Implementation
    # =========================================================================

    async def _call_anthropic_categorize(
        self, description: str, categories: List[str]
    ) -> Optional[CategorizeResponse]:
        model = self.custom_model or "claude-3-5-haiku-20241022"
        url = "https://api.anthropic.com/v1/messages"

        prompt = (
            f"You are a finance categorization assistant for Paradox.\n"
            f"Allowed categories: {json.dumps(categories)}\n"
            f"Expense description: \"{description}\"\n"
            f"Respond ONLY with valid JSON having keys: \"category_name\", \"confidence\", \"reasoning\"."
        )

        payload = {
            "model": model,
            "max_tokens": 256,
            "messages": [{"role": "user", "content": prompt}]
        }

        headers = {
            "x-api-key": self.anthropic_key,
            "anthropic-version": "2023-06-01",
            "Content-Type": "application/json"
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload, headers=headers)
            if resp.status_code == 200:
                data = resp.json()
                text = data["content"][0]["text"]
                # Extract JSON block
                json_match = re.search(r"\{.*\}", text, re.DOTALL)
                if json_match:
                    parsed = json.loads(json_match.group())
                    cat = self._match_closest(parsed.get("category_name"), categories)
                    return CategorizeResponse(
                        category_name=cat,
                        confidence=float(parsed.get("confidence", 0.85)),
                        reasoning=parsed.get("reasoning"),
                        provider_used="anthropic",
                    )
        return None

    async def _call_anthropic_parse(
        self, text: str, categories: List[str], payment_methods: List[str]
    ) -> Optional[ParseExpenseResponse]:
        model = self.custom_model or "claude-3-5-haiku-20241022"
        url = "https://api.anthropic.com/v1/messages"
        today_str = date.today().isoformat()

        prompt = (
            f"You are an expense parsing assistant for the Paradox personal finance tracker. Today is {today_str}.\n"
            f"Allowed categories: {json.dumps(categories)}\n"
            f"Allowed payment methods: {json.dumps(payment_methods)}\n"
            f"User input text: \"{text}\"\n"
            f"Extract expense information strictly into JSON with keys: \"amount\", \"category_name\", \"payment_method_name\", \"date\", \"description\", \"confidence\", \"reasoning\".\n"
            f"Important rule: \"description\" must be ONLY the concise merchant or purchased item (e.g. for 'Paid 450 for Zomato pizza via UPI yesterday', description must be 'Zomato pizza' or 'Zomato'). Strip out amount, payment methods, date words like 'yesterday', and filler prepositions like 'paid for', 'via'."
        )

        payload = {
            "model": model,
            "max_tokens": 512,
            "messages": [{"role": "user", "content": prompt}]
        }

        headers = {
            "x-api-key": self.anthropic_key,
            "anthropic-version": "2023-06-01",
            "Content-Type": "application/json"
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload, headers=headers)
            if resp.status_code == 200:
                data = resp.json()
                raw_text = data["content"][0]["text"]
                json_match = re.search(r"\{.*\}", raw_text, re.DOTALL)
                if json_match:
                    parsed = json.loads(json_match.group())
                    amount = Decimal(str(parsed["amount"])) if parsed.get("amount") is not None else None
                    cat = self._match_closest(parsed.get("category_name"), categories) if parsed.get("category_name") else None
                    pm = self._match_closest(parsed.get("payment_method_name"), payment_methods) if parsed.get("payment_method_name") else None

                    return ParseExpenseResponse(
                        amount=amount,
                        category_name=cat,
                        payment_method_name=pm,
                        date=parsed.get("date") or today_str,
                        description=parsed.get("description"),
                        confidence=float(parsed.get("confidence", 0.9)),
                        reasoning=parsed.get("reasoning"),
                        provider_used="anthropic",
                    )
        return None

    # =========================================================================
    # Fallback Semantic Heuristic Matcher
    # =========================================================================

    def _heuristic_categorize(self, description: str, categories: List[str]) -> CategorizeResponse:
        desc_lower = description.lower()

        # Score available categories
        best_cat = categories[0] if categories else "Other"
        best_score = 0
        matched_reason = "Matched default category"

        for cat in categories:
            keywords = HEURISTIC_KEYWORD_MAP.get(cat, [])
            cat_score = 0
            matched_kws = []
            for kw in keywords:
                if re.search(rf"\b{re.escape(kw)}\b", desc_lower):
                    cat_score += 2
                    matched_kws.append(kw)
                elif len(kw) >= 4 and re.search(rf"\b{re.escape(kw)}", desc_lower):
                    cat_score += 1
                    matched_kws.append(kw)

            if cat_score > best_score:
                best_score = cat_score
                best_cat = cat
                matched_reason = f"Matched {', '.join(matched_kws[:3])} for {cat}"

        confidence = 0.85 if best_score >= 2 else (0.65 if best_score == 1 else 0.40)
        return CategorizeResponse(
            category_name=best_cat,
            confidence=confidence,
            reasoning=matched_reason,
            provider_used="heuristic",
        )

    def _heuristic_parse(
        self, text: str, categories: List[str], payment_methods: List[str]
    ) -> ParseExpenseResponse:
        text_lower = text.lower()
        today = date.today()

        # 1. Extract Amount
        amount: Optional[Decimal] = None
        amount_matches = re.findall(r"(?:(?:rs\.?|inr|₹|\$)\s*(\d+(?:\.\d{1,2})?)|(\d+(?:\.\d{1,2})?)\s*(?:rs\.?|inr|₹|\$|bucks|rupees?|\b))", text_lower)
        # Also plain numbers
        if not amount_matches:
            plain_nums = re.findall(r"\b\d+(?:\.\d{1,2})?\b", text)
            if plain_nums:
                amount = Decimal(plain_nums[0])
        else:
            for m in amount_matches:
                val = m[0] or m[1]
                if val:
                    amount = Decimal(val)
                    break

        # 2. Extract Date
        date_str = today.isoformat()
        days_ago_match = re.search(r"\b(\d+)\s+days?\s+ago\b", text_lower)
        weeks_ago_match = re.search(r"\b(\d+)\s+weeks?\s+ago\b", text_lower)
        months_ago_match = re.search(r"\b(\d+)\s+months?\s+ago\b", text_lower)
        if days_ago_match:
            days_ago = int(days_ago_match.group(1))
            date_str = (today - timedelta(days=days_ago)).isoformat()
        elif weeks_ago_match:
            weeks_ago = int(weeks_ago_match.group(1))
            date_str = (today - timedelta(weeks=weeks_ago)).isoformat()
        elif months_ago_match:
            months_ago = int(months_ago_match.group(1))
            date_str = (today - timedelta(days=months_ago * 30)).isoformat()
        elif "day before yesterday" in text_lower:
            date_str = (today - timedelta(days=2)).isoformat()
        elif "yesterday" in text_lower:
            date_str = (today - timedelta(days=1)).isoformat()
        else:
            # Check for explicit date like 2026-08-15 or 15-08-2026
            date_match = re.search(r"\b(\d{4}[-/]\d{1,2}[-/]\d{1,2})\b", text)
            if date_match:
                date_str = date_match.group(1).replace("/", "-")
            else:
                date_match_dm = re.search(r"\b(\d{1,2})[-/](\d{1,2})[-/](\d{4})\b", text)
                if date_match_dm:
                    d, m, y = date_match_dm.groups()
                    date_str = f"{y}-{int(m):02d}-{int(d):02d}"

        # 3. Extract Category
        cat_response = self._heuristic_categorize(text, categories)
        matched_cat = cat_response.category_name

        # 4. Extract Payment Method
        matched_pm = payment_methods[0] if payment_methods else "Cash"
        found_exact = False
        for pm in payment_methods:
            if re.search(rf"\b{re.escape(pm.lower())}\b", text_lower):
                matched_pm = pm
                found_exact = True
                break

        if not found_exact:
            for pm in payment_methods:
                keywords = HEURISTIC_PM_MAP.get(pm, [pm.lower()])
                matched_this_pm = False
                for kw in keywords:
                    if re.search(rf"\b{re.escape(kw)}\b", text_lower):
                        matched_pm = pm
                        matched_this_pm = True
                        break
                if matched_this_pm:
                    break

        # 5. Clean Description (extract pure merchant/item name)
        cleaned_desc = text
        # Strip relative date words
        cleaned_desc = re.sub(r"\b\d+\s+days?\s+ago\b", "", cleaned_desc, flags=re.IGNORECASE)
        cleaned_desc = re.sub(r"\b\d+\s+weeks?\s+ago\b", "", cleaned_desc, flags=re.IGNORECASE)
        cleaned_desc = re.sub(r"\b\d+\s+months?\s+ago\b", "", cleaned_desc, flags=re.IGNORECASE)
        cleaned_desc = re.sub(r"\b(day before yesterday|yesterday|today|tomorrow)\b", "", cleaned_desc, flags=re.IGNORECASE)
        cleaned_desc = re.sub(r"\b(days?\s+ago|weeks?\s+ago|months?\s+ago)\b", "", cleaned_desc, flags=re.IGNORECASE)
        # Strip explicit ISO or slash dates
        cleaned_desc = re.sub(r"\b\d{4}-\d{2}-\d{2}\b|\b\d{1,2}[-/]\d{1,2}[-/]\d{2,4}\b", "", cleaned_desc)
        # Strip amounts with currency prefix/suffix
        cleaned_desc = re.sub(r"(?:(?:rs\.?|inr|₹|\$)\s*\d+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?\s*(?:rs\.?|inr|₹|\$|bucks|rupees?|\b))", "", cleaned_desc, flags=re.IGNORECASE)
        if amount:
            cleaned_desc = re.sub(rf"\b{re.escape(str(amount))}\b", "", cleaned_desc)
        # Strip payment method words (e.g. UPI, cash, credit card, etc.)
        for pm_words in HEURISTIC_PM_MAP.values():
            for w in pm_words:
                cleaned_desc = re.sub(rf"\b{re.escape(w)}\b", "", cleaned_desc, flags=re.IGNORECASE)
        # Strip action verbs and filler prepositions
        cleaned_desc = re.sub(r"\b(paid|spent|bought|for|via|by|on|at|in|using|with|through|to|from)\b", "", cleaned_desc, flags=re.IGNORECASE)
        # Clean excess whitespace
        cleaned_desc = re.sub(r"\s+", " ", cleaned_desc).strip()
        if not cleaned_desc:
            cleaned_desc = text.strip()

        return ParseExpenseResponse(
            amount=amount,
            category_name=matched_cat,
            payment_method_name=matched_pm,
            date=date_str,
            description=cleaned_desc.capitalize() if cleaned_desc else "Expense",
            confidence=cat_response.confidence,
            reasoning=cat_response.reasoning,
            provider_used="heuristic",
        )

    def _match_closest(self, target: Optional[str], choices: List[str]) -> str:
        if not target:
            return choices[0] if choices else "Other"
        target_clean = target.strip().lower()
        # 1. Exact match
        for c in choices:
            if c.lower() == target_clean:
                return c
        # 2. Known alias / synonym mapping
        if target_clean in ["upi", "gpay", "google pay", "phonepe", "paytm", "bhim", "scan", "wallet"]:
            for c in choices:
                if c.lower() in ["digital wallet", "wallet", "upi"]:
                    return c
        if target_clean in ["net banking", "netbanking", "neft", "rtgs", "imps", "wire", "transfer", "bank"]:
            for c in choices:
                if c.lower() in ["bank transfer", "net banking"]:
                    return c
        if target_clean in ["utilities", "utility", "bills", "bill", "electricity", "water", "gas"]:
            for c in choices:
                if "utilit" in c.lower() or "bill" in c.lower():
                    return c
        if target_clean in ["healthcare", "health", "medicine", "doctor", "medical"]:
            for c in choices:
                if "health" in c.lower() or "medic" in c.lower():
                    return c
        if target_clean in ["groceries", "grocery", "supermarket", "veggies"]:
            for c in choices:
                if "grocer" in c.lower():
                    return c
            for c in choices:
                if "food" in c.lower():
                    return c
        if target_clean in ["food", "dining", "restaurant", "cafe", "snack"]:
            for c in choices:
                if "food" in c.lower() or "dining" in c.lower():
                    return c
        if target_clean in ["transport", "taxi", "cab", "travel"]:
            for c in choices:
                if "transport" in c.lower():
                    return c
        if target_clean in ["other", "uncategorized", "misc"]:
            for c in choices:
                if "uncategorized" in c.lower() or "other" in c.lower():
                    return c

        # 3. Substring match
        for c in choices:
            if target_clean in c.lower() or c.lower() in target_clean:
                return c

        return choices[0] if choices else "Uncategorized"
    # =========================================================================
    # Financial Copilot & Smart Insights
    # =========================================================================

    async def generate_insights(
        self,
        period: str,
        total_spent: Decimal,
        budget_limit: Optional[Decimal],
        category_breakdown: List[Dict[str, Any]],
        days_elapsed: int,
        total_days: int,
    ) -> AIInsightsResponse:
        provider = self.resolve_provider()
        days_eff = max(days_elapsed, 1)
        daily_burn_rate = round(total_spent / Decimal(str(days_eff)), 2)
        projected_spend = round(daily_burn_rate * Decimal(str(total_days)), 2)

        if provider == "gemini":
            try:
                res = await self._gemini_insights(
                    period, total_spent, budget_limit, category_breakdown, daily_burn_rate, projected_spend, days_elapsed, total_days
                )
                if res:
                    return res
            except Exception as exc:
                logger.warning("Gemini insights call failed (%s), falling back to heuristic", exc)
        elif provider == "openai":
            try:
                res = await self._openai_insights(
                    period, total_spent, budget_limit, category_breakdown, daily_burn_rate, projected_spend, days_elapsed, total_days
                )
                if res:
                    return res
            except Exception as exc:
                logger.warning("OpenAI insights call failed (%s), falling back to heuristic", exc)
        elif provider == "anthropic":
            try:
                res = await self._anthropic_insights(
                    period, total_spent, budget_limit, category_breakdown, daily_burn_rate, projected_spend, days_elapsed, total_days
                )
                if res:
                    return res
            except Exception as exc:
                logger.warning("Anthropic insights call failed (%s), falling back to heuristic", exc)

        return self._heuristic_generate_insights(
            period=period,
            total_spent=total_spent,
            budget_limit=budget_limit,
            category_breakdown=category_breakdown,
            daily_burn_rate=daily_burn_rate,
            projected_spend=projected_spend,
            days_elapsed=days_elapsed,
            total_days=total_days,
        )

    async def _gemini_insights(
        self,
        period: str,
        total_spent: Decimal,
        budget_limit: Optional[Decimal],
        category_breakdown: List[Dict[str, Any]],
        daily_burn_rate: Decimal,
        projected_spend: Decimal,
        days_elapsed: int,
        total_days: int,
    ) -> Optional[AIInsightsResponse]:
        model = self.custom_model or "gemini-1.5-flash"
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={self.gemini_key}"

        prompt = (
            f"You are the Paradox AI Financial Copilot. Generate actionable, encouraging financial insights for this user.\n"
            f"Period: {period} ({days_elapsed} of {total_days} days elapsed)\n"
            f"Total Spent: ₹{total_spent}\n"
            f"Budget Limit: ₹{budget_limit if budget_limit else 'None'}\n"
            f"Daily Burn Rate: ₹{daily_burn_rate}\n"
            f"Projected Month-end Spend: ₹{projected_spend}\n"
            f"Category Breakdown: {json.dumps(category_breakdown[:5])}\n"
            f"Respond STRICTLY in JSON with keys:\n"
            f"- \"health_status\": \"healthy\" | \"cautious\" | \"critical\"\n"
            f"- \"headline\": punchy, motivating 1-sentence assessment\n"
            f"- \"alerts\": array of 1-2 important bullet points regarding budget threshold or burn rate\n"
            f"- \"saving_tips\": array of 2 practical recommendations to optimize spending\n"
            f"- \"confidence\": float 0.0 to 1.0"
        )

        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {"temperature": 0.2, "responseMimeType": "application/json"},
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload)
            if resp.status_code == 200:
                data = resp.json()
                raw_text = data["candidates"][0]["content"]["parts"][0]["text"]
                parsed = json.loads(raw_text)
                return AIInsightsResponse(
                    health_status=parsed.get("health_status", "healthy"),
                    headline=parsed.get("headline", "Financial tracking active"),
                    alerts=parsed.get("alerts", []),
                    saving_tips=parsed.get("saving_tips", []),
                    projected_spend=projected_spend,
                    daily_burn_rate=daily_burn_rate,
                    confidence=float(parsed.get("confidence", 0.9)),
                    provider_used="gemini",
                )
        return None

    async def _openai_insights(
        self,
        period: str,
        total_spent: Decimal,
        budget_limit: Optional[Decimal],
        category_breakdown: List[Dict[str, Any]],
        daily_burn_rate: Decimal,
        projected_spend: Decimal,
        days_elapsed: int,
        total_days: int,
    ) -> Optional[AIInsightsResponse]:
        model = self.custom_model or "gpt-4o-mini"
        url = "https://api.openai.com/v1/chat/completions"

        prompt = (
            f"You are the Paradox AI Financial Copilot. Generate actionable financial insights in strict JSON.\n"
            f"Period: {period} ({days_elapsed}/{total_days} days). Spent: ₹{total_spent}. Budget: ₹{budget_limit}. Burn rate: ₹{daily_burn_rate}/day. Projected: ₹{projected_spend}.\n"
            f"Top categories: {json.dumps(category_breakdown[:5])}\n"
            f"Keys required: health_status (healthy/cautious/critical), headline, alerts (array), saving_tips (array), confidence (float)."
        )

        payload = {
            "model": model,
            "messages": [{"role": "user", "content": prompt}],
            "response_format": {"type": "json_object"},
            "temperature": 0.2,
        }

        headers = {"Authorization": f"Bearer {self.openai_key}", "Content-Type": "application/json"}

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload, headers=headers)
            if resp.status_code == 200:
                data = resp.json()
                parsed = json.loads(data["choices"][0]["message"]["content"])
                return AIInsightsResponse(
                    health_status=parsed.get("health_status", "healthy"),
                    headline=parsed.get("headline", "Financial tracking active"),
                    alerts=parsed.get("alerts", []),
                    saving_tips=parsed.get("saving_tips", []),
                    projected_spend=projected_spend,
                    daily_burn_rate=daily_burn_rate,
                    confidence=float(parsed.get("confidence", 0.9)),
                    provider_used="openai",
                )
        return None

    async def _anthropic_insights(
        self,
        period: str,
        total_spent: Decimal,
        budget_limit: Optional[Decimal],
        category_breakdown: List[Dict[str, Any]],
        daily_burn_rate: Decimal,
        projected_spend: Decimal,
        days_elapsed: int,
        total_days: int,
    ) -> Optional[AIInsightsResponse]:
        model = self.custom_model or "claude-3-5-haiku-20241022"
        url = "https://api.anthropic.com/v1/messages"

        prompt = (
            f"You are the Paradox AI Financial Copilot. Generate financial insights strictly in JSON format.\n"
            f"Period: {period} ({days_elapsed}/{total_days} days). Spent: ₹{total_spent}. Budget: ₹{budget_limit}. Burn rate: ₹{daily_burn_rate}/day. Projected: ₹{projected_spend}.\n"
            f"Top categories: {json.dumps(category_breakdown[:5])}\n"
            f"JSON format: {{\"health_status\": \"healthy\"|\"cautious\"|\"critical\", \"headline\": \"...\", \"alerts\": [...], \"saving_tips\": [...], \"confidence\": 0.9}}"
        )

        payload = {"model": model, "max_tokens": 512, "messages": [{"role": "user", "content": prompt}]}
        headers = {
            "x-api-key": self.anthropic_key,
            "anthropic-version": "2023-06-01",
            "Content-Type": "application/json",
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, json=payload, headers=headers)
            if resp.status_code == 200:
                data = resp.json()
                raw_text = data["content"][0]["text"]
                match = re.search(r"\{.*\}", raw_text, re.DOTALL)
                if match:
                    parsed = json.loads(match.group())
                    return AIInsightsResponse(
                        health_status=parsed.get("health_status", "healthy"),
                        headline=parsed.get("headline", "Financial tracking active"),
                        alerts=parsed.get("alerts", []),
                        saving_tips=parsed.get("saving_tips", []),
                        projected_spend=projected_spend,
                        daily_burn_rate=daily_burn_rate,
                        confidence=float(parsed.get("confidence", 0.9)),
                        provider_used="anthropic",
                    )
        return None

    def _heuristic_generate_insights(
        self,
        period: str,
        total_spent: Decimal,
        budget_limit: Optional[Decimal],
        category_breakdown: List[Dict[str, Any]],
        daily_burn_rate: Decimal,
        projected_spend: Decimal,
        days_elapsed: int,
        total_days: int,
    ) -> AIInsightsResponse:
        alerts: List[str] = []
        saving_tips: List[str] = []
        days_left = max(total_days - days_elapsed, 0)

        # Health status determination
        if budget_limit and budget_limit > Decimal("0.00"):
            pct_spent = float((total_spent / budget_limit) * Decimal("100"))
            proj_pct = float((projected_spend / budget_limit) * Decimal("100"))

            if proj_pct > 115.0 or pct_spent >= 100.0:
                health_status = "critical"
                headline = f"Budget pace critical: projected to exceed by {int(proj_pct - 100)}%"
                alerts.append(f"At your current pace of ₹{daily_burn_rate}/day, total spend will reach ₹{projected_spend} vs budget ₹{budget_limit}.")
            elif proj_pct >= 90.0 or pct_spent >= 80.0:
                health_status = "cautious"
                headline = "Spending on high pace: approaching budget limit"
                alerts.append(f"You have spent {int(pct_spent)}% of your budget with {days_left} days remaining.")
            else:
                health_status = "healthy"
                headline = "Great pacing: safely within your target budget"
                alerts.append(f"Projected to finish comfortably under budget with ~₹{budget_limit - projected_spend} in savings.")
        else:
            health_status = "healthy"
            headline = f"Tracking active: ₹{total_spent} spent across {days_elapsed} days"
            if total_spent > Decimal("0.00"):
                alerts.append(f"Current daily burn rate is ₹{daily_burn_rate}/day.")

        # Top category analysis & tips
        if category_breakdown:
            top_cat = category_breakdown[0]
            cat_name = top_cat.get("category_name") or top_cat.get("name") or "Top Category"
            cat_amount = Decimal(str(top_cat.get("total", "0")))
            if total_spent > Decimal("0.00"):
                cat_pct = float((cat_amount / total_spent) * Decimal("100"))
                if cat_pct >= 35.0:
                    alerts.append(f"{cat_name} accounts for {int(cat_pct)}% of all spending (₹{cat_amount}).")
                    saving_tips.append(f"Consider trimming discretionary expenses in {cat_name} to optimize savings.")

        if not saving_tips:
            saving_tips.append("Review recurring subscriptions and automated payments to free up monthly cashflow.")
            saving_tips.append("Plan weekend outings with a dedicated budget cap to avoid sudden spend spikes.")

        return AIInsightsResponse(
            health_status=health_status,
            headline=headline,
            alerts=alerts,
            saving_tips=saving_tips,
            projected_spend=projected_spend,
            daily_burn_rate=daily_burn_rate,
            confidence=0.85,
            provider_used="heuristic",
        )

    # =========================================================================
    # Predictive Budget Recommendation
    # =========================================================================

    async def suggest_budget(
        self,
        period_type: str,
        past_expenses: List[Any],
    ) -> SuggestBudgetResponse:
        if not past_expenses:
            defaults = {
                "month": Decimal("20000.00"),
                "week": Decimal("5000.00"),
                "day": Decimal("800.00"),
            }
            amount = defaults.get(period_type, Decimal("10000.00"))
            return SuggestBudgetResponse(
                period_type=period_type,
                suggested_amount=amount,
                reasoning=f"Standard personal finance benchmark for a {period_type}ly budget with balanced allocations.",
                category_allocations=[
                    CategoryAllocation(category_name="Food & Dining", percentage=35.0, suggested_amount=round(amount * Decimal("0.35"), 2)),
                    CategoryAllocation(category_name="Utilities", percentage=20.0, suggested_amount=round(amount * Decimal("0.20"), 2)),
                    CategoryAllocation(category_name="Transportation", percentage=15.0, suggested_amount=round(amount * Decimal("0.15"), 2)),
                    CategoryAllocation(category_name="Shopping", percentage=15.0, suggested_amount=round(amount * Decimal("0.15"), 2)),
                    CategoryAllocation(category_name="Other", percentage=15.0, suggested_amount=round(amount * Decimal("0.15"), 2)),
                ],
                confidence=0.75,
                provider_used="heuristic",
            )

        total_historical = sum((Decimal(str(e.amount)) for e in past_expenses), Decimal("0.00"))
        dates = [e.date for e in past_expenses if e.date]
        if dates:
            min_date = min(dates)
            max_date = max(dates)
            span_days = max((max_date - min_date).days + 1, 1)
        else:
            span_days = 30

        avg_daily = total_historical / Decimal(str(span_days))

        if period_type == "day":
            target_days = 1
            buffer = Decimal("1.10")
        elif period_type == "week":
            target_days = 7
            buffer = Decimal("1.10")
        else:
            target_days = 30
            buffer = Decimal("1.12")

        suggested = round(avg_daily * Decimal(str(target_days)) * buffer, 2)
        # Round up to clean multiple
        suggested = Decimal(str(int(suggested / Decimal("50") + Decimal("0.99")) * 50))

        cat_totals: Dict[str, Decimal] = {}
        for e in past_expenses:
            cat_name = getattr(e, "category_name", None) or (e.category.name if hasattr(e, "category") and e.category else "Other")
            cat_totals[cat_name] = cat_totals.get(cat_name, Decimal("0.00")) + Decimal(str(e.amount))

        allocations: List[CategoryAllocation] = []
        for cat, cat_tot in sorted(cat_totals.items(), key=lambda x: x[1], reverse=True)[:5]:
            if total_historical > Decimal("0.00"):
                pct = round(float((cat_tot / total_historical) * Decimal("100")), 1)
                alloc_amt = round(suggested * (Decimal(str(pct)) / Decimal("100")), 2)
                allocations.append(CategoryAllocation(category_name=cat, percentage=pct, suggested_amount=alloc_amt))

        reasoning = (
            f"Calculated from your past {span_days} days of spending (avg ₹{round(avg_daily, 2)}/day), "
            f"scaled to a {period_type}ly cycle with a {int((buffer - 1) * 100)}% safety buffer for unexpected costs."
        )

        return SuggestBudgetResponse(
            period_type=period_type,
            suggested_amount=suggested,
            reasoning=reasoning,
            category_allocations=allocations,
            confidence=0.90,
            provider_used="heuristic",
        )

    # =========================================================================
    # Receipt & Invoice Text Parser
    # =========================================================================

    async def parse_receipt_text(
        self,
        text: str,
        categories: Optional[List[str]] = None,
        payment_methods: Optional[List[str]] = None,
    ) -> ParseReceiptResponse:
        cats = categories or DEFAULT_SYSTEM_CATEGORIES
        pms = payment_methods or DEFAULT_PAYMENT_METHODS

        lines = [line.strip() for line in text.strip().split("\n") if line.strip()]
        merchant = lines[0] if lines else "Merchant"
        merchant = re.sub(r"(tax invoice|receipt|bill|store|welcome to|invoice)\b", "", merchant, flags=re.IGNORECASE).strip() or "Merchant"

        items: List[ParsedReceiptItem] = []
        total_amount: Optional[Decimal] = None

        for line in lines[1:]:
            price_match = re.search(r"(?:₹|\$|rs\.?|inr)?\s*(\d+(?:\.\d{1,2})?)\s*$", line, re.IGNORECASE)
            if price_match:
                price = Decimal(price_match.group(1))
                item_name = line[:price_match.start()].strip()
                item_name = re.sub(r"^[-*•\d.]+\s*", "", item_name).strip()
                if item_name and "total" not in item_name.lower():
                    cat = self._heuristic_categorize(item_name, cats).category_name
                    items.append(ParsedReceiptItem(item_name=item_name, amount=price, category_suggestion=cat))
                elif "total" in item_name.lower():
                    total_amount = price

        if not total_amount and items:
            total_amount = sum((it.amount for it in items), Decimal("0.00"))
        elif not total_amount:
            parsed_gen = self._heuristic_parse(text, cats, pms)
            total_amount = parsed_gen.amount

        parsed_expense = self._heuristic_parse(text, cats, pms)

        return ParseReceiptResponse(
            merchant_name=merchant,
            total_amount=total_amount,
            date=parsed_expense.date,
            category_name=parsed_expense.category_name,
            payment_method_name=parsed_expense.payment_method_name,
            items=items,
            provider_used="heuristic",
        )

    async def scan_receipt_image(
        self,
        image_bytes: bytes,
        mime_type: str,
        categories: List[str],
        payment_methods: List[str],
    ) -> ParseExpenseResponse:
        import base64
        import json
        import re

        # If Gemini key is available, call Gemini 3.6 Flash Vision
        if settings.GEMINI_API_KEY and settings.GEMINI_API_KEY != "mock-gemini-key":
            try:
                base64_img = base64.b64encode(image_bytes).decode("utf-8")
                prompt = f"""You are an expert financial receipt and invoice OCR reader for Paradox.
Extract the transaction details from this receipt/bill image:
- "amount": total final amount paid as numeric decimal/float (e.g. 540.00). Only numbers and dot.
- "date": transaction date in YYYY-MM-DD format (if missing or cannot read, use today: {date.today().isoformat()}).
- "description": concise merchant/store/service name or main purchase (max 40 chars, e.g. "Dmart", "Starbucks", "Shell Fuel").
- "category_name": best matching category from this list: {json.dumps(categories)}.
- "payment_method_name": best matching payment method from this list: {json.dumps(payment_methods)}.

Return ONLY a valid raw JSON object with keys: "amount", "date", "description", "category_name", "payment_method_name". No markdown, no commentary."""

                for model in ["gemini-3.6-flash", "gemini-flash-latest"]:
                    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={settings.GEMINI_API_KEY}"
                    payload = {
                        "contents": [{
                            "parts": [
                                {"text": prompt},
                                {
                                    "inline_data": {
                                        "mime_type": mime_type or "image/jpeg",
                                        "data": base64_img
                                    }
                                }
                            ]
                        }],
                        "generationConfig": {"temperature": 0.1}
                    }
                    try:
                        async with httpx.AsyncClient(timeout=30.0) as client:
                            resp = await client.post(url, json=payload)
                            if resp.status_code == 200:
                                data = resp.json()
                                text_res = data["candidates"][0]["content"]["parts"][0]["text"].strip()
                                m = re.search(r"\{.*\}", text_res, re.DOTALL)
                                if m:
                                    parsed = json.loads(m.group(0))
                                    raw_amount = parsed.get("amount")
                                    amt_str = str(raw_amount).replace(",", "").replace("$", "").replace("₹", "").strip() if raw_amount is not None else "0"
                                    try:
                                        amount_dec = Decimal(amt_str)
                                    except Exception:
                                        amount_dec = Decimal("0.00")

                                    cat_str = self._match_closest(parsed.get("category_name"), categories) if categories else "Shopping"
                                    pm_str = self._match_closest(parsed.get("payment_method_name"), payment_methods) if payment_methods else "Cash"

                                    return ParseExpenseResponse(
                                        amount=amount_dec,
                                        date=parsed.get("date") or date.today().isoformat(),
                                        description=parsed.get("description") or "Scanned Receipt",
                                        category_name=cat_str,
                                        payment_method_name=pm_str,
                                        confidence=0.96,
                                        reasoning=f"Scanned with Gemini Vision OCR ({model})",
                                        provider_used="gemini-vision",
                                    )
                    except Exception as model_err:
                        logger.warning(f"Vision model {model} attempt error: {model_err}")
                        continue
            except Exception as e:
                logger.warning(f"Gemini Vision scan failed: {e}")

        # Fallback if Vision not available or offline
        return ParseExpenseResponse(
            amount=Decimal("0.00"),
            date=date.today().isoformat(),
            description="Scanned Receipt Image",
            category_name=categories[0] if categories else "Shopping",
            payment_method_name=payment_methods[0] if payment_methods else "Cash",
            confidence=0.5,
            reasoning="Receipt image received (please enter verified amount)",
            provider_used="heuristic-image",
        )

    # =========================================================================
    # PHASE 1: CORE FINANCIAL INTELLIGENCE METHODS
    # =========================================================================

    async def simulate_purchase(
        self,
        amount: Decimal,
        category_name: Optional[str],
        description: Optional[str],
        total_spent: Decimal,
        budget_limit: Optional[Decimal],
        days_elapsed: int,
        total_days: int,
        category_spent: Decimal = Decimal("0.00"),
        category_budget: Optional[Decimal] = None,
    ) -> SimulatePurchaseResponse:
        """
        Evaluate a purchase decision deterministically against budget limits,
        daily burn velocity, safe-to-spend allowance, and remaining month pacing.
        """
        days_remaining = max(total_days - days_elapsed, 1)

        # Baseline budget calculations
        if budget_limit and budget_limit > 0:
            current_rem = budget_limit - total_spent
            projected_rem = current_rem - amount
            safe_daily_before = max(current_rem / Decimal(days_remaining), Decimal("0.00")).quantize(Decimal("0.01"))
            safe_daily_after = max(projected_rem / Decimal(days_remaining), Decimal("0.00")).quantize(Decimal("0.01"))

            # Determine verdict
            if projected_rem < Decimal("0.00"):
                verdict = "over_budget"
                deficit = abs(projected_rem)
                headline = f"Over-Budget Alert (Deficit: {deficit:.2f})"
                advice = (
                    f"Spending {amount:.2f} on {description or 'this item'} will breach your period budget by "
                    f"{deficit:.2f}. Consider postponing this purchase or offsetting with other categories."
                )
                can_proceed = False
                savings_impact = f"Destroys month-end surplus and puts balance in a {deficit:.2f} deficit."
            elif amount > (current_rem * Decimal("0.40")) or safe_daily_after < (safe_daily_before * Decimal("0.60")):
                verdict = "caution"
                percent_burn = (amount / current_rem * Decimal("100")).quantize(Decimal("0.1"))
                headline = f"Proceed with Caution (Consumes {percent_burn}% of remaining buffer)"
                advice = (
                    f"This purchase will reduce your daily safe allowance from {safe_daily_before:.2f}/day to "
                    f"{safe_daily_after:.2f}/day for the remaining {days_remaining} days. Feasible if you tighten other expenses."
                )
                can_proceed = True
                savings_impact = f"Reduces projected period surplus by {amount:.2f}."
            else:
                verdict = "safe"
                headline = "Affordable Purchase (Safe to Proceed)"
                advice = (
                    f"Your finances accommodate {amount:.2f} smoothly. You will still have {projected_rem:.2f} "
                    f"budget buffer ({safe_daily_after:.2f}/day for {days_remaining} days)."
                )
                can_proceed = True
                savings_impact = f"Healthy trajectory retained with {projected_rem:.2f} projected buffer."

            cat_impact = None
            if category_name:
                new_cat_spent = category_spent + amount
                if category_budget and category_budget > 0:
                    cat_pct = (new_cat_spent / category_budget * Decimal("100")).quantize(Decimal("0.1"))
                    cat_impact = f"{category_name} spending will reach {new_cat_spent:.2f} ({cat_pct}% of category budget)."
                else:
                    cat_impact = f"{category_name} total this month will rise to {new_cat_spent:.2f}."

            return SimulatePurchaseResponse(
                verdict=verdict,
                headline=headline,
                advice=advice,
                current_remaining_budget=current_rem.quantize(Decimal("0.01")),
                projected_remaining_budget=projected_rem.quantize(Decimal("0.01")),
                safe_to_spend_daily_before=safe_daily_before,
                safe_to_spend_daily_after=safe_daily_after,
                category_impact=cat_impact,
                savings_impact=savings_impact,
                can_proceed=can_proceed,
            )
        else:
            # Unbudgeted user fallback
            current_burn = (total_spent / Decimal(max(days_elapsed, 1))).quantize(Decimal("0.01"))
            if amount > (current_burn * Decimal("5")):
                verdict = "caution"
                headline = "Substantial Outlay (No Budget Set)"
                advice = (
                    f"Spending {amount:.2f} is more than 5x your average daily spend ({current_burn:.2f}/day). "
                    f"Setting a monthly budget will unlock precise safe-to-spend limits."
                )
                can_proceed = True
            else:
                verdict = "safe"
                headline = "Manageable Expense"
                advice = f"Spending {amount:.2f} fits within normal single-day variations. Consider setting a monthly budget."
                can_proceed = True

            return SimulatePurchaseResponse(
                verdict=verdict,
                headline=headline,
                advice=advice,
                current_remaining_budget=Decimal("0.00"),
                projected_remaining_budget=Decimal("0.00"),
                safe_to_spend_daily_before=Decimal("0.00"),
                safe_to_spend_daily_after=Decimal("0.00"),
                category_impact=f"Adds {amount:.2f} to {category_name or 'Uncategorized'}.",
                savings_impact="No target budget defined to measure savings variance.",
                can_proceed=can_proceed,
            )

    async def calculate_safe_to_spend(
        self,
        total_spent: Decimal,
        budget_limit: Optional[Decimal],
        days_elapsed: int,
        total_days: int,
    ) -> SafeToSpendResponse:
        """
        Calculate deterministic daily spending allowance, velocity comparison,
        and projected date of budget exhaustion.
        """
        days_remaining = max(total_days - days_elapsed, 0)
        current_burn = (total_spent / Decimal(max(days_elapsed, 1))).quantize(Decimal("0.01"))

        if not budget_limit or budget_limit <= 0:
            return SafeToSpendResponse(
                safe_daily_allowance=Decimal("0.00"),
                current_daily_burn_rate=current_burn,
                remaining_budget=Decimal("0.00"),
                days_remaining=days_remaining,
                depletion_date=None,
                status="optimal",
                burn_status_message="Configure a budget target to activate the daily safe-to-spend speedometer.",
            )

        remaining = (budget_limit - total_spent).quantize(Decimal("0.01"))
        effective_days = max(days_remaining, 1)
        safe_daily = max(remaining / Decimal(effective_days), Decimal("0.00")).quantize(Decimal("0.01"))

        # Compute depletion date
        if remaining <= 0:
            depletion_date = date.today().isoformat()
            status = "danger"
            burn_msg = f"Budget fully depleted! You are exceeding your target by {abs(remaining):.2f}."
        elif current_burn > 0:
            days_until_empty = int(remaining / current_burn)
            depletion_dt = date.today() + timedelta(days=days_until_empty)
            depletion_date = depletion_dt.isoformat()

            if current_burn > (safe_daily * Decimal("1.25")):
                status = "danger"
                burn_msg = (
                    f"Warning: Current spend ({current_burn:.2f}/day) is 25%+ above safe allowance ({safe_daily:.2f}/day). "
                    f"Budget projected to run out around {depletion_date}."
                )
            elif current_burn > safe_daily:
                status = "warning"
                burn_msg = (
                    f"Pacing Warning: Spending {current_burn:.2f}/day slightly outpaces safe target ({safe_daily:.2f}/day). "
                    f"Estimated depletion on {depletion_date}."
                )
            else:
                status = "optimal"
                burn_msg = (
                    f"Optimal Pacing: Daily burn ({current_burn:.2f}/day) is comfortably below safe allowance ({safe_daily:.2f}/day). "
                    f"On track to maintain surplus."
                )
        else:
            depletion_date = None
            status = "optimal"
            burn_msg = f"Safe allowance is {safe_daily:.2f}/day for the remaining {days_remaining} days."

        return SafeToSpendResponse(
            safe_daily_allowance=safe_daily,
            current_daily_burn_rate=current_burn,
            remaining_budget=remaining,
            days_remaining=days_remaining,
            depletion_date=depletion_date,
            status=status,
            burn_status_message=burn_msg,
        )

    async def calculate_health_score(
        self,
        total_spent: Decimal,
        budget_limit: Optional[Decimal],
        days_elapsed: int,
        total_days: int,
        category_breakdown: List[Dict[str, Any]],
    ) -> FinancialHealthScoreResponse:
        """
        Compute deterministic 0-100 Financial Health Score across 3 core pillars:
        - Budget Adherence (40 points)
        - Savings Velocity (35 points)
        - Category Discipline (25 points)
        """
        # Pillar 1: Budget Adherence (40 pts)
        if budget_limit and budget_limit > 0:
            expected_fraction = Decimal(days_elapsed) / Decimal(total_days)
            actual_fraction = total_spent / budget_limit
            if actual_fraction <= expected_fraction:
                score_adherence = 40
                adherence_fb = "Perfect pacing. Cumulative spending is strictly on or below target."
            elif actual_fraction <= Decimal("1.0"):
                over_fraction = actual_fraction - expected_fraction
                penalty = int(over_fraction * 40 * Decimal("1.8"))
                score_adherence = max(12, 40 - penalty)
                adherence_fb = "Pacing is slightly faster than calendar timeline, but still within overall budget."
            else:
                over_budget = actual_fraction - Decimal("1.0")
                score_adherence = max(0, int(15 - over_budget * 25))
                adherence_fb = "Current expenditure has crossed the total period budget."
        else:
            score_adherence = 26
            adherence_fb = "Default score assigned. Set a monthly budget to maximize points."

        # Pillar 2: Savings Velocity (35 pts)
        if budget_limit and budget_limit > 0:
            daily_burn = total_spent / Decimal(max(days_elapsed, 1))
            projected_total = daily_burn * Decimal(total_days)
            if projected_total < budget_limit:
                savings_margin = (budget_limit - projected_total) / budget_limit
                score_savings = min(35, 20 + int(savings_margin * 30))
                savings_fb = f"Projected surplus of {(budget_limit - projected_total):.2f} at period close."
            else:
                deficit_margin = (projected_total - budget_limit) / budget_limit
                score_savings = max(5, int(18 - deficit_margin * 20))
                savings_fb = "Run rate indicates potential budget overrun by end of period."
        else:
            score_savings = 22
            savings_fb = "Establish target savings to measure velocity accurately."

        # Pillar 3: Category Discipline (25 pts)
        if not category_breakdown:
            score_discipline = 20
            discipline_fb = "Healthy baseline. Log regular expenses to track category concentration."
        else:
            max_pct = max((c.get("percentage", 0.0) for c in category_breakdown), default=0.0)
            if max_pct > 65.0:
                score_discipline = 10
                discipline_fb = f"Over-concentration alert: One category takes {max_pct:.0f}% of total budget."
            elif max_pct > 45.0:
                score_discipline = 18
                discipline_fb = f"Primary category accounts for {max_pct:.0f}% of your spending."
            else:
                score_discipline = 25
                discipline_fb = "Excellent spending diversification across categories."

        total_score = max(0, min(100, score_adherence + score_savings + score_discipline))

        if total_score >= 80:
            status = "excellent"
            headline = "Outstanding Financial Health"
            recs = [
                "Maintain current daily burn rate to lock in maximum monthly savings.",
                "Consider transferring projected surplus into liquid investments or emergency fund.",
            ]
        elif total_score >= 60:
            status = "good"
            headline = "Good Financial Control"
            recs = [
                "Monitor top-spending categories to prevent end-of-month budget strain.",
                "Aim to trim discretionary purchases by 5-10% to push into the Excellent tier.",
            ]
        else:
            status = "needs_attention"
            headline = "Budget Strain Detected"
            recs = [
                "Pause non-essential and entertainment spending for the next 7 days.",
                "Review upcoming recurring bills to avoid unexpected overdraft or deficits.",
            ]

        pillars = [
            HealthScorePillar(name="Budget Adherence", score=score_adherence, max_score=40, feedback=adherence_fb),
            HealthScorePillar(name="Savings Velocity", score=score_savings, max_score=35, feedback=savings_fb),
            HealthScorePillar(name="Category Discipline", score=score_discipline, max_score=25, feedback=discipline_fb),
        ]

        return FinancialHealthScoreResponse(
            score=total_score,
            status=status,
            headline=headline,
            pillars=pillars,
            recommendations=recs,
        )

    async def analyze_spending_leaks(
        self,
        past_expenses: List[Any],
        threshold: Decimal = Decimal("150.00"),
    ) -> LeakAnalysisResponse:
        """
        Identify micro-spending drains (transactions <= threshold, default ₹150/$2)
        occurring frequently that create substantial annualized leakage.
        """
        # Filter micro-transactions
        micro_txs = [
            e for e in past_expenses
            if hasattr(e, "amount") and Decimal(str(e.amount)) <= threshold and Decimal(str(e.amount)) > 0
        ]

        if not micro_txs:
            return LeakAnalysisResponse(
                total_monthly_leak=Decimal("0.00"),
                total_annual_leak=Decimal("0.00"),
                leaks=[],
                summary="No significant micro-spending leaks detected under your threshold.",
            )

        # Group by normalized merchant/description or category
        grouped: Dict[str, List[Decimal]] = {}
        cat_map: Dict[str, str] = {}

        for e in micro_txs:
            desc = (e.description or "").strip()
            # Clean description
            cleaned = re.sub(r"[^\w\s]", "", desc).strip().title()
            if not cleaned or len(cleaned) < 2:
                cleaned = e.category.name if hasattr(e, "category") and e.category else "Miscellaneous"

            grouped.setdefault(cleaned, []).append(Decimal(str(e.amount)))
            if hasattr(e, "category") and e.category:
                cat_map[cleaned] = e.category.name

        # Calculate time span in months (minimum 1)
        dates = [e.date for e in past_expenses if hasattr(e, "date") and e.date]
        if dates:
            span_days = max((max(dates) - min(dates)).days, 30)
            months_span = Decimal(str(span_days)) / Decimal("30")
        else:
            months_span = Decimal("1.0")

        leaks: List[SpendingLeakItem] = []
        total_monthly = Decimal("0.00")

        for key, amounts in grouped.items():
            count = len(amounts)
            if count >= 2:  # Occurs at least twice
                avg_amt = (sum(amounts) / Decimal(count)).quantize(Decimal("0.01"))
                freq_mo = max(1, int(Decimal(count) / months_span))
                monthly_cost = (avg_amt * Decimal(freq_mo)).quantize(Decimal("0.01"))
                annual_cost = (monthly_cost * Decimal("12")).quantize(Decimal("0.01"))
                total_monthly += monthly_cost

                # Actionable savings tip
                tip = (
                    f"Consolidating or capping '{key}' by 30% could save ~{(annual_cost * Decimal('0.30')):.2f} every year."
                )

                leaks.append(
                    SpendingLeakItem(
                        merchant_or_pattern=key,
                        frequency_per_month=freq_mo,
                        avg_amount=avg_amt,
                        monthly_drain=monthly_cost,
                        annualized_drain=annual_cost,
                        category_name=cat_map.get(key, "General"),
                        savings_tip=tip,
                    )
                )

        leaks.sort(key=lambda x: x.annualized_drain, reverse=True)
        total_annual = (total_monthly * Decimal("12")).quantize(Decimal("0.01"))

        summary = (
            f"Detected {len(leaks)} recurring micro-spending patterns draining {total_monthly:.2f}/month "
            f"({total_annual:.2f}/year). Modest behavioral adjustments here deliver high savings leverage."
            if leaks else "Micro-spending is well-controlled with minimal repetitive leakage."
        )

        return LeakAnalysisResponse(
            total_monthly_leak=total_monthly.quantize(Decimal("0.01")),
            total_annual_leak=total_annual,
            leaks=leaks,
            summary=summary,
        )

    async def audit_subscriptions(
        self,
        past_expenses: List[Any],
        recurring_expenses: List[Any],
    ) -> SubscriptionAuditResponse:
        """
        Audit recurring commitments, detect overlapping subscriptions in the same category,
        compute annual drain, and flag bundle/annual discount opportunities.
        """
        audited_items: List[AuditSubscriptionItem] = []
        total_monthly = Decimal("0.00")
        category_counts: Dict[str, List[str]] = {}

        # 1. Process explicit recurring expenses
        for rec in recurring_expenses:
            amt = Decimal(str(rec.amount))
            freq = rec.recurring_frequency or "monthly"
            cat_name = rec.category.name if hasattr(rec, "category") and rec.category else "Subscriptions"
            merchant = rec.description or "Subscription"

            if freq == "weekly":
                mo_cost = amt * Decimal("4.33")
            elif freq == "yearly":
                mo_cost = amt / Decimal("12")
            else:
                mo_cost = amt

            ann_cost = mo_cost * Decimal("12")
            total_monthly += mo_cost
            category_counts.setdefault(cat_name.lower(), []).append(merchant)

            audited_items.append(
                AuditSubscriptionItem(
                    merchant=merchant,
                    category_name=cat_name,
                    estimated_amount=amt.quantize(Decimal("0.01")),
                    frequency=freq,
                    annual_cost=ann_cost.quantize(Decimal("0.01")),
                    flag=None,
                    optimization_tip=f"Consider an annual plan for {merchant} to save up to 15-20% on recurring fees.",
                )
            )

        # 2. Flag duplicate/overlapping categories
        warnings: List[str] = []
        pot_savings = Decimal("0.00")

        for cat, merchants in category_counts.items():
            if len(merchants) >= 2:
                names = ", ".join(merchants)
                warnings.append(f"Multiple overlapping subscriptions in '{cat.title()}': {names}.")
                pot_savings += Decimal("300.00")  # Modest estimate

        insights = [
            f"Active subscriptions generate a fixed commitment of {total_monthly:.2f} per month ({total_monthly * 12:.2f}/year).",
            "Auditing subscriptions every quarter prevents zombie renewals for services you no longer utilize.",
        ]
        if warnings:
            insights.append("Consolidating redundant entertainment or streaming platforms can immediately free up cash flow.")

        return SubscriptionAuditResponse(
            total_monthly_commitment=total_monthly.quantize(Decimal("0.01")),
            total_annual_commitment=(total_monthly * Decimal("12")).quantize(Decimal("0.01")),
            active_subscriptions=audited_items,
            duplicate_warnings=warnings,
            potential_annual_savings=pot_savings.quantize(Decimal("0.01")),
            insights=insights,
        )


ai_service = AIService()


