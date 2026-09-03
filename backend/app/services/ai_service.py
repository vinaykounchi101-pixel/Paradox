import json
import logging
import re
from datetime import date, datetime, timedelta
from decimal import Decimal
from typing import Any, Dict, List, Optional, Tuple

import httpx

from app.core.config import settings
from app.schemas.ai import CategorizeResponse, ParseExpenseResponse

logger = logging.getLogger(__name__)

DEFAULT_SYSTEM_CATEGORIES = [
    "Food & Dining",
    "Transportation",
    "Utilities",
    "Housing",
    "Entertainment",
    "Healthcare",
    "Shopping",
    "Education",
    "Personal Care",
    "Travel",
    "Investments",
    "Income",
    "Other",
]

DEFAULT_PAYMENT_METHODS = [
    "Cash",
    "Credit Card",
    "Debit Card",
    "UPI",
    "Net Banking",
    "Wallet",
    "Other",
]

HEURISTIC_KEYWORD_MAP = {
    "Food & Dining": [
        "food", "dining", "restaurant", "cafe", "coffee", "tea", "chai", "lunch", "dinner",
        "breakfast", "snack", "swiggy", "zomato", "mcdonald", "burger", "pizza", "starbucks",
        "biryani", "grocery", "groceries", "supermarket", "blinkit", "zepto", "instamart",
        "fruits", "vegetables", "milk", "bakery", "bread"
    ],
    "Transportation": [
        "transport", "taxi", "cab", "uber", "ola", "auto", "rickshaw", "metro", "bus",
        "train", "flight", "fuel", "petrol", "diesel", "cng", "parking", "toll", "fastag"
    ],
    "Utilities": [
        "utility", "electricity", "water", "gas", "wifi", "internet", "broadband", "mobile",
        "recharge", "phone bill", "cylinder", "dth"
    ],
    "Housing": [
        "rent", "maintenance", "mortgage", "repair", "plumber", "electrician", "furniture",
        "maid", "cook"
    ],
    "Entertainment": [
        "movie", "cinema", "netflix", "prime", "hotstar", "spotify", "concert", "game",
        "steam", "youtube", "theatre", "party", "club"
    ],
    "Healthcare": [
        "doctor", "hospital", "clinic", "medicine", "pharmacy", "medical", "test", "dentist",
        "apollo", "pharmeasy", "health"
    ],
    "Shopping": [
        "clothes", "shoes", "amazon", "flipkart", "myntra", "shopping", "mall", "electronics",
        "gadget", "appliances", "book", "gift"
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
    ]
}

HEURISTIC_PM_MAP = {
    "UPI": ["upi", "gpay", "google pay", "phonepe", "paytm", "bhim", "scan"],
    "Credit Card": ["credit card", "credit", "cc"],
    "Debit Card": ["debit card", "debit", "dc", "atm card"],
    "Cash": ["cash", "notes", "currency"],
    "Net Banking": ["net banking", "netbanking", "neft", "rtgs", "imps", "bank transfer"],
    "Wallet": ["wallet", "amazon pay", "paytm wallet"],
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
        model = self.custom_model or "gemini-1.5-flash"
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
        model = self.custom_model or "gemini-1.5-flash"
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
            for kw in keywords:
                if re.search(rf"\b{re.escape(kw)}\b", desc_lower):
                    score = 2 if kw in desc_lower.split() else 1
                    if score > best_score:
                        best_score = score
                        best_cat = cat
                        matched_reason = f"Matched keyword '{kw}' for {cat}"

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
        if "yesterday" in text_lower:
            date_str = (today - timedelta(days=1)).isoformat()
        elif "day before yesterday" in text_lower:
            date_str = (today - timedelta(days=2)).isoformat()
        else:
            # Check for explicit date like 2026-08-15 or 15-08-2026
            date_match = re.search(r"\b(\d{4}-\d{2}-\d{2})\b", text)
            if date_match:
                date_str = date_match.group(1)

        # 3. Extract Category
        cat_response = self._heuristic_categorize(text, categories)
        matched_cat = cat_response.category_name

        # 4. Extract Payment Method
        matched_pm = payment_methods[0] if payment_methods else "Cash"
        for pm in payment_methods:
            keywords = HEURISTIC_PM_MAP.get(pm, [pm.lower()])
            for kw in keywords:
                if re.search(rf"\b{re.escape(kw)}\b", text_lower):
                    matched_pm = pm
                    break

        # 5. Clean Description (extract pure merchant/item name)
        cleaned_desc = text
        # Strip relative date words
        cleaned_desc = re.sub(r"\b(day before yesterday|yesterday|today|tomorrow)\b", "", cleaned_desc, flags=re.IGNORECASE)
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
        for c in choices:
            if c.lower() == target_clean:
                return c
        for c in choices:
            if target_clean in c.lower() or c.lower() in target_clean:
                return c
        return choices[0] if choices else "Other"


ai_service = AIService()
