from datetime import date, timedelta
from decimal import Decimal
import pytest

from app.schemas.ai import (
    AIChatRequest,
    ChatMessage,
    SavingsPlanRequest,
    AnalyzeSentimentRequest,
)
from app.services.ai_service import ai_service


class MockExpense:
    def __init__(self, id_val, amount, category_name, description, date_val):
        self.id = id_val
        self.amount = Decimal(str(amount))
        self.category_name = category_name
        self.description = description
        self.date = date_val


@pytest.mark.asyncio
async def test_chat_assistant_heuristic_affordability():
    context = {
        "current_month_spent": "15000.00",
        "budget_limit": "25000.00",
        "safe_daily_spend": "666.67",
        "burn_rate": "500.00",
        "days_elapsed": 15,
        "days_remaining": 15,
        "top_categories": [{"name": "Dining", "amount": "5000.00", "percentage": 33.3}],
        "recent_transactions": [],
    }

    # Safe purchase query
    res = await ai_service.chat_with_financial_assistant(
        message="Can I afford 300 for lunch?",
        history=[],
        context=context,
    )
    assert res.reply is not None
    assert "Safe to Spend" in res.reply
    assert len(res.suggested_followups) > 0

    # Over-budget purchase query
    res_over = await ai_service.chat_with_financial_assistant(
        message="Can I afford 15000 for a vacation?",
        history=[],
        context=context,
    )
    assert "Not Recommended" in res_over.reply


@pytest.mark.asyncio
async def test_chat_assistant_spending_inquiry():
    context = {
        "current_month_spent": "12500.00",
        "budget_limit": "20000.00",
        "safe_daily_spend": "500.00",
        "burn_rate": "450.00",
        "days_elapsed": 10,
        "days_remaining": 20,
        "top_categories": [
            {"name": "Food & Dining", "amount": "4500.00", "percentage": 36.0},
            {"name": "Shopping", "amount": "3000.00", "percentage": 24.0},
        ],
        "recent_transactions": [],
    }

    res = await ai_service.chat_with_financial_assistant(
        message="How much have I spent so far?",
        history=[],
        context=context,
    )
    assert "12500.00" in res.reply

    res_cat = await ai_service.chat_with_financial_assistant(
        message="What is my biggest expense category?",
        history=[],
        context=context,
    )
    assert "Food & Dining" in res_cat.reply


def test_detect_spending_anomalies():
    today = date.today()
    expenses = [
        # Regular Dining items around 300-400
        MockExpense("1", 300, "Food & Dining", "Lunch", today - timedelta(days=2)),
        MockExpense("2", 350, "Food & Dining", "Dinner", today - timedelta(days=5)),
        MockExpense("3", 400, "Food & Dining", "Cafe", today - timedelta(days=8)),
        MockExpense("4", 320, "Food & Dining", "Breakfast", today - timedelta(days=12)),
        # Anomaly Dining item: 4500 (10x higher!)
        MockExpense("5", 4500, "Food & Dining", "Luxury Buffet Feast", today - timedelta(days=1)),
    ]

    res = ai_service.detect_spending_anomalies(expenses, budget_limit=Decimal("15000.00"))
    assert res.total_anomalies >= 1
    top_anomaly = res.anomalies[0]
    assert top_anomaly.category_name == "Food & Dining"
    assert top_anomaly.amount == Decimal("4500.00")
    assert top_anomaly.severity in ["high", "critical"]


def test_spending_forecast():
    today = date.today()
    current_expenses = [
        MockExpense("1", 2000, "Food & Dining", "Groceries", today - timedelta(days=2)),
        MockExpense("2", 1500, "Transportation", "Fuel", today - timedelta(days=3)),
    ]
    past_expenses = [
        MockExpense("3", 6000, "Food & Dining", "Past Food", today - timedelta(days=40)),
        MockExpense("4", 4500, "Transportation", "Past Travel", today - timedelta(days=50)),
    ]

    res = ai_service.generate_spending_forecast(
        current_expenses=current_expenses,
        past_expenses=past_expenses,
        days_elapsed=10,
        total_days=30,
    )
    assert res.total_projected_next_month > Decimal("0.00")
    assert len(res.category_forecasts) > 0
    assert len(res.forecast_insights) > 0


def test_savings_plan():
    today = date.today()
    past_expenses = [
        MockExpense("1", 9000, "Food & Dining", "Dining", today - timedelta(days=10)),
        MockExpense("2", 6000, "Shopping", "Clothes", today - timedelta(days=20)),
        MockExpense("3", 3000, "Entertainment", "Movies", today - timedelta(days=30)),
    ]

    res = ai_service.generate_savings_plan(
        target_amount=Decimal("15000.00"),
        target_months=3,
        goal_name="Emergency Buffer",
        past_expenses=past_expenses,
    )
    assert res.goal_name == "Emergency Buffer"
    assert res.required_monthly_savings == Decimal("5000.00")
    assert res.feasibility in ["highly_achievable", "achievable", "challenging"]
    assert len(res.category_cuts) > 0
    assert len(res.action_steps) > 0


def test_sentiment_analysis():
    # Remorse test
    res_remorse = ai_service.analyze_expense_sentiment(
        text="Bought expensive shoes, total regret and waste of money",
        amount=Decimal("8000.00"),
    )
    assert res_remorse.sentiment == "remorse"
    assert "Buyer's Remorse" in res_remorse.spending_tag

    # Stress test
    res_stress = ai_service.analyze_expense_sentiment(
        text="Terrible rough day at work, ordered comfort food",
        amount=Decimal("650.00"),
    )
    assert res_stress.sentiment == "stress"
    assert "Stress Spending" in res_stress.spending_tag

    # Celebration test
    res_joy = ai_service.analyze_expense_sentiment(
        text="Birthday celebration dinner with team",
        amount=Decimal("3500.00"),
    )
    assert res_joy.sentiment == "positive"
    assert "Celebration" in res_joy.spending_tag


def test_monthly_wrapped():
    today = date.today()
    expenses = [
        MockExpense("1", 450, "Food & Dining", "Zomato", today - timedelta(days=2)),
        MockExpense("2", 1200, "Food & Dining", "Zomato", today - timedelta(days=5)),
        MockExpense("3", 7500, "Shopping", "Sony Headphones", today - timedelta(days=8)),
    ]

    res = ai_service.generate_monthly_wrapped(
        expenses=expenses,
        month_str="August 2026",
        budget_limit=Decimal("20000.00"),
        active_streak_days=10,
    )
    assert res.month == "August 2026"
    assert res.total_spent == Decimal("9150.00")
    assert res.total_transactions == 3
    assert res.biggest_splurge is not None
    assert res.biggest_splurge.amount == Decimal("7500.00")
    assert res.most_frequent_merchant == "Zomato"
    assert res.archetype_title is not None
    assert len(res.personalized_recap) > 0


def test_vibe_check():
    # Chill vibe
    res_chill = ai_service.generate_vibe_check(
        total_spent=Decimal("5000.00"),
        budget_limit=Decimal("25000.00"),
        days_elapsed=15,
        total_days=30,
        is_roast_mode=True,
    )
    assert res_chill.burn_rate_status == "chill"
    assert res_chill.vibe_emoji == "🧘"

    # Critical vibe
    res_crit = ai_service.generate_vibe_check(
        total_spent=Decimal("26000.00"),
        budget_limit=Decimal("25000.00"),
        days_elapsed=15,
        total_days=30,
        is_roast_mode=True,
    )
    assert res_crit.burn_rate_status == "critical"
    assert res_crit.vibe_emoji == "💀"
    assert "ICU" in res_crit.roast_commentary or "breach" in res_crit.roast_commentary


@pytest.mark.asyncio
async def test_chat_router_context_mapping():
    from unittest.mock import AsyncMock, MagicMock
    from app.api.ai import chat_assistant
    from app.schemas.ai import SafeToSpendResponse
    from app.schemas.dashboard import DashboardRead, DashboardBudget

    mock_db = MagicMock()
    mock_user = MagicMock()
    mock_user.id = "11111111-1111-1111-1111-111111111111"

    # Mock DashboardService
    mock_dash = MagicMock()
    mock_dash.get_dashboard_data = AsyncMock(return_value=DashboardRead(
        period="current_month",
        total_spent=Decimal("5000.00"),
        budget=DashboardBudget(amount=Decimal("20000.00"), spent=Decimal("5000.00"), remaining=Decimal("15000.00"), status="under_budget"),
        category_breakdown=[],
        top_categories=[],
        trend=[],
        recent_expenses=[],
    ))

    # Mock ai_service safe_to_spend
    mock_safe_spend = SafeToSpendResponse(
        safe_daily_allowance=Decimal("750.00"),
        current_daily_burn_rate=Decimal("250.00"),
        remaining_budget=Decimal("15000.00"),
        days_remaining=20,
        depletion_date=None,
        status="optimal",
        burn_status_message="Optimal burn",
    )

    with pytest.MonkeyPatch.context() as mp:
        mp.setattr("app.api.ai.DashboardService", lambda db: mock_dash)
        mp.setattr("app.api.ai.ai_service.calculate_safe_to_spend", AsyncMock(return_value=mock_safe_spend))

        res = await chat_assistant(
            payload=AIChatRequest(message="Can I afford 1000?"),
            current_user=mock_user,
            db=mock_db,
        )
        assert res["data"].reply is not None
        assert len(res["data"].suggested_followups) > 0

