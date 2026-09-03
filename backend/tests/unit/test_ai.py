import json
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

    # Test Food & Dining
    res = await service.categorize_expense("Starbucks cold brew with muffin")
    assert res.category_name == "Food & Dining"
    assert res.provider_used == "heuristic"
    assert res.confidence > 0.5

    # Test Transportation
    res = await service.categorize_expense("Uber cab to airport terminal")
    assert res.category_name == "Transportation"

    # Test Utilities
    res = await service.categorize_expense("Electricity bill payment for August")
    assert res.category_name == "Utilities"

    # Test Entertainment
    res = await service.categorize_expense("Netflix monthly subscription")
    assert res.category_name == "Entertainment"

    # Test Healthcare
    res = await service.categorize_expense("Apollo pharmacy medicine tablet")
    assert res.category_name == "Healthcare"


@pytest.mark.asyncio
async def test_heuristic_parse_expense():
    service = AIService()

    # Test sentence with amount, category, payment method, date relative word
    text = "Paid 450 for auto rickshaw via UPI yesterday"
    parsed = await service.parse_expense_text(text)
    assert parsed.amount == Decimal("450")
    assert parsed.category_name == "Transportation"
    assert parsed.payment_method_name == "UPI"
    assert parsed.provider_used == "heuristic"
    assert parsed.date is not None

    # Test rupee symbol with credit card
    text_2 = "Bought groceries ₹1200 on Credit Card"
    parsed_2 = await service.parse_expense_text(text_2)
    assert parsed_2.amount == Decimal("1200")
    assert parsed_2.category_name == "Food & Dining"
    assert parsed_2.payment_method_name == "Credit Card"
    assert parsed_2.description == "Groceries"

    # Test user's exact query: description must only be 'Zomato pizza'
    text_3 = "Paid 450 for Zomato pizza via UPI yesterday"
    parsed_3 = await service.parse_expense_text(text_3)
    assert parsed_3.amount == Decimal("450")
    assert parsed_3.category_name == "Food & Dining"
    assert parsed_3.payment_method_name == "UPI"
    assert parsed_3.description == "Zomato pizza"


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
