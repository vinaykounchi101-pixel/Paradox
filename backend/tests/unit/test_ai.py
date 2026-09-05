import json
from datetime import date, timedelta
from decimal import Decimal
from unittest.mock import AsyncMock, patch

import pytest
import httpx

from app.schemas.ai import CategorizeResponse, ParseExpenseResponse
from app.services.ai_service import AIService


@pytest.mark.asyncio
async def test_provider_resolution(monkeypatch):
    # Case 1: No keys -> heuristic
    monkeypatch.setattr("app.core.config.settings.AI_PROVIDER", "auto")
    monkeypatch.setattr("app.core.config.settings.GEMINI_API_KEY", None)
    monkeypatch.setattr("app.core.config.settings.OPENAI_API_KEY", None)
    monkeypatch.setattr("app.core.config.settings.ANTHROPIC_API_KEY", None)
    service = AIService()
    assert service.resolve_provider() == "heuristic"

    # Case 2: Gemini key provided in auto mode
    monkeypatch.setattr("app.core.config.settings.GEMINI_API_KEY", "test-gemini-key")
    service = AIService()
    assert service.resolve_provider() == "gemini"

    # Case 3: OpenAI explicitly requested
    monkeypatch.setattr("app.core.config.settings.AI_PROVIDER", "openai")
    monkeypatch.setattr("app.core.config.settings.OPENAI_API_KEY", "test-openai-key")
    service = AIService()
    assert service.resolve_provider() == "openai"

    # Case 4: Anthropic explicitly requested
    monkeypatch.setattr("app.core.config.settings.AI_PROVIDER", "anthropic")
    monkeypatch.setattr("app.core.config.settings.ANTHROPIC_API_KEY", "test-anthropic-key")
    service = AIService()
    assert service.resolve_provider() == "anthropic"


@pytest.mark.asyncio
async def test_heuristic_categorization():
    service = AIService()
    service.gemini_key = None

    # Test Food & Dining
    res = await service.categorize_expense("Starbucks cold brew with muffin")
    assert res.category_name == "Food & Dining"
    assert res.provider_used == "heuristic"
    assert res.confidence > 0.5

    # Test Transportation
    res = await service.categorize_expense("Uber cab to airport terminal")
    assert res.category_name == "Transportation"

    # Test Utilities / Bills & Utilities
    res = await service.categorize_expense("Electricity bill payment for August")
    assert res.category_name == "Bills & Utilities"

    # Test Entertainment / Subscriptions
    res = await service.categorize_expense("Netflix monthly subscription")
    assert res.category_name in ("Entertainment", "Subscriptions")

    # Test Healthcare
    res = await service.categorize_expense("Apollo pharmacy medicine tablet")
    assert res.category_name in ["Health", "Healthcare"]


@pytest.mark.asyncio
async def test_heuristic_parse_expense():
    service = AIService()
    service.gemini_key = None

    # Test sentence with amount, category, payment method, date relative word
    text = "Paid 450 for auto rickshaw via UPI yesterday"
    parsed = await service.parse_expense_text(text)
    assert parsed.amount == Decimal("450")
    assert parsed.category_name == "Transportation"
    assert parsed.payment_method_name == "UPI"
    assert parsed.provider_used == "heuristic"
    assert parsed.date is not None

    # Test rupee symbol with credit card and groceries
    text_2 = "Bought groceries ₹1200 on Credit Card"
    parsed_2 = await service.parse_expense_text(text_2)
    assert parsed_2.amount == Decimal("1200")
    assert parsed_2.category_name == "Groceries"
    assert parsed_2.payment_method_name == "Credit Card"
    assert parsed_2.description == "Groceries"

    # Test user's exact query: description must only be 'Zomato pizza'
    text_3 = "Paid 450 for Zomato pizza via UPI yesterday"
    parsed_3 = await service.parse_expense_text(text_3)
    assert parsed_3.amount == Decimal("450")
    assert parsed_3.category_name == "Food & Dining"
    assert parsed_3.payment_method_name == "UPI"
    assert parsed_3.description == "Zomato pizza"

    # Test when user only has default starter methods (Digital Wallet instead of UPI)
    starter_pms = ["Cash", "Debit Card", "Credit Card", "Bank Transfer", "Digital Wallet", "Other"]
    parsed_4 = await service.parse_expense_text(text_3, available_payment_methods=starter_pms)
    assert parsed_4.payment_method_name == "Digital Wallet"

    # Test user's exact query: 'paid 1000 for a new gaming mouse via upi 2 days ago'
    text_gaming = "paid 1000 for a new gaming mouse via upi 2 days ago"
    parsed_gaming = await service.parse_expense_text(
        text_gaming,
        available_payment_methods=starter_pms,
    )
    assert parsed_gaming.amount == Decimal("1000")
    assert parsed_gaming.category_name in ("Shopping", "Gaming")
    assert parsed_gaming.payment_method_name == "Digital Wallet"
    assert parsed_gaming.date == (date.today() - timedelta(days=2)).isoformat()
    assert parsed_gaming.description == "A new gaming mouse"


@pytest.mark.asyncio
async def test_gemini_api_mock(monkeypatch):
    monkeypatch.setattr("app.core.config.settings.AI_PROVIDER", "gemini")
    monkeypatch.setattr("app.core.config.settings.GEMINI_API_KEY", "mock-gemini-key")
    service = AIService()

    mock_response = {
        "candidates": [
            {
                "content": {
                    "parts": [
                        {
                            "text": json.dumps({
                                "category_name": "Food & Dining",
                                "confidence": 0.95,
                                "reasoning": "Starbucks is a famous coffee shop chain"
                            })
                        }
                    ]
                }
            }
        ]
    }

    mock_client = AsyncMock()
    mock_client.post.return_value = httpx.Response(200, json=mock_response, request=httpx.Request("POST", "https://api"))

    with patch("httpx.AsyncClient", return_value=mock_client):
        mock_client.__aenter__.return_value = mock_client
        mock_client.__aexit__.return_value = None

        res = await service.categorize_expense("Starbucks frappuccino")
        assert res.category_name == "Food & Dining"
        assert res.provider_used == "gemini"
        assert res.confidence == 0.95


@pytest.mark.asyncio
async def test_api_failure_fallback_to_heuristic(monkeypatch):
    # When Gemini fails (e.g. 500 error or exception), fallback to heuristic seamlessly
    monkeypatch.setattr("app.core.config.settings.AI_PROVIDER", "gemini")
    monkeypatch.setattr("app.core.config.settings.GEMINI_API_KEY", "mock-gemini-key")
    service = AIService()

    mock_client = AsyncMock()
    mock_client.post.side_effect = httpx.ConnectError("Network timeout connecting to Google Gemini API")

    with patch("httpx.AsyncClient", return_value=mock_client):
        mock_client.__aenter__.return_value = mock_client
        mock_client.__aexit__.return_value = None

        res = await service.categorize_expense("Petrol pump fuel refilling")
        assert res.category_name == "Transportation"
        assert res.provider_used == "heuristic"


@pytest.mark.asyncio
async def test_ai_insights_heuristic():
    service = AIService()

    # Case 1: Healthy pacing
    res_healthy = await service.generate_insights(
        period="current_month",
        total_spent=Decimal("5000.00"),
        budget_limit=Decimal("20000.00"),
        category_breakdown=[{"category_name": "Food & Dining", "total": "2000.00", "percentage": 40.0}],
        days_elapsed=10,
        total_days=30,
    )
    assert res_healthy.health_status == "healthy"
    assert res_healthy.daily_burn_rate == Decimal("500.00")
    assert res_healthy.projected_spend == Decimal("15000.00")
    assert len(res_healthy.alerts) >= 1

    # Case 2: Critical pacing (projected spend exceeds budget by >15%)
    res_critical = await service.generate_insights(
        period="current_month",
        total_spent=Decimal("18000.00"),
        budget_limit=Decimal("20000.00"),
        category_breakdown=[{"category_name": "Shopping", "total": "12000.00", "percentage": 66.7}],
        days_elapsed=15,
        total_days=30,
    )
    assert res_critical.health_status == "critical"
    assert res_critical.projected_spend == Decimal("36000.00")
    assert any("exceed" in a.lower() or "pace" in a.lower() for a in res_critical.alerts)


@pytest.mark.asyncio
async def test_ai_suggest_budget():
    service = AIService()

    # Case 1: Empty history defaults
    res_empty = await service.suggest_budget(period_type="month", past_expenses=[])
    assert res_empty.suggested_amount > Decimal("0")
    assert res_empty.period_type == "month"
    assert len(res_empty.category_allocations) > 0

    # Case 2: With simulated expense history
    class DummyExpense:
        def __init__(self, amount, cat_name):
            self.amount = Decimal(amount)
            self.category_name = cat_name
            self.date = date(2026, 8, 1)

    mock_history = [
        DummyExpense("3000.00", "Food & Dining"),
        DummyExpense("2000.00", "Transportation"),
        DummyExpense("5000.00", "Housing"),
    ]
    res_history = await service.suggest_budget(period_type="month", past_expenses=mock_history)
    assert res_history.suggested_amount > Decimal("0")
    assert any(a.category_name == "Housing" for a in res_history.category_allocations)


@pytest.mark.asyncio
async def test_ai_parse_receipt():
    service = AIService()

    receipt_text = """
    McDonald's Restaurant
    1 McSpicy Chicken 249.00
    1 French Fries Large 120.00
    1 Coke 60.00
    Total 429.00
    """
    res = await service.parse_receipt_text(receipt_text)
    assert "McDonald" in (res.merchant_name or "")
    assert res.total_amount == Decimal("429.00")
    assert len(res.items) >= 2


@pytest.mark.asyncio
async def test_categorize_suggests_new_category_when_not_in_user_categories():
    service = AIService()
    service.gemini_key = None

    # User only has Food & Dining and Transportation
    user_cats = ["Food & Dining", "Transportation"]

    # Expense is for a pet
    res_pet = await service.categorize_expense("Bought pedigree dog food from pet shop", available_categories=user_cats)
    assert res_pet.category_name == "Pets"
    assert res_pet.is_new_category is True

    # Expense is for Alcohol (e.g. brandy)
    res_alcohol = await service.categorize_expense("brandy", available_categories=user_cats)
    assert res_alcohol.category_name == "Alcohol"
    assert res_alcohol.is_new_category is True

    # Expense is for Gaming (e.g. ps5)
    res_gaming = await service.categorize_expense("ps5", available_categories=user_cats)
    assert res_gaming.category_name == "Gaming"
    assert res_gaming.is_new_category is True

    # Expense is for existing Food & Dining
    res_food = await service.categorize_expense("Starbucks coffee and croissant", available_categories=user_cats)
    assert res_food.category_name == "Food & Dining"
    assert res_food.is_new_category is False


@pytest.mark.asyncio
async def test_scan_receipt_image_mock(monkeypatch):
    monkeypatch.setattr("app.core.config.settings.GEMINI_API_KEY", "mock-test-key")
    service = AIService()

    mock_response = {
        "candidates": [
            {
                "content": {
                    "parts": [
                        {
                            "text": json.dumps({
                                "amount": 850.50,
                                "date": "2026-09-02",
                                "description": "Starbucks Reserve",
                                "category_name": "Food & Dining",
                                "payment_method_name": "Credit Card"
                            })
                        }
                    ]
                }
            }
        ]
    }

    mock_client = AsyncMock()
    mock_client.post.return_value = httpx.Response(200, json=mock_response, request=httpx.Request("POST", "https://api"))

    with patch("httpx.AsyncClient", return_value=mock_client):
        mock_client.__aenter__.return_value = mock_client
        mock_client.__aexit__.return_value = None

        res = await service.scan_receipt_image(
            image_bytes=b"fake-image-bytes",
            mime_type="image/jpeg",
            categories=["Food & Dining", "Shopping"],
            payment_methods=["Credit Card", "UPI"]
        )

        assert res.amount == Decimal("850.50")
        assert res.date == "2026-09-02"
        assert res.description == "Starbucks Reserve"
        assert res.category_name == "Food & Dining"
        assert res.payment_method_name == "Credit Card"
        assert res.provider_used == "gemini-vision"

