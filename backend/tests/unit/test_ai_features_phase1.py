from datetime import date, timedelta
from decimal import Decimal
import pytest

from app.services.ai_service import AIService


class MockCategory:
    def __init__(self, name: str):
        self.name = name


class MockExpense:
    def __init__(
        self,
        amount: Decimal,
        description: str,
        category_name: str,
        dt: date,
        is_recurring: bool = False,
        recurring_frequency: str = "monthly",
    ):
        self.amount = amount
        self.description = description
        self.category = MockCategory(category_name)
        self.date = dt
        self.is_recurring = is_recurring
        self.recurring_frequency = recurring_frequency


@pytest.mark.asyncio
async def test_simulate_purchase_safe():
    service = AIService()
    res = await service.simulate_purchase(
        amount=Decimal("500.00"),
        category_name="Food & Dining",
        description="Dinner at Italian bistro",
        total_spent=Decimal("5000.00"),
        budget_limit=Decimal("20000.00"),
        days_elapsed=10,
        total_days=30,
        category_spent=Decimal("1200.00"),
    )
    assert res.verdict == "safe"
    assert res.can_proceed is True
    assert res.current_remaining_budget == Decimal("15000.00")
    assert res.projected_remaining_budget == Decimal("14500.00")
    assert res.safe_to_spend_daily_before > Decimal("0.00")
    assert res.safe_to_spend_daily_after > Decimal("0.00")
    assert "Dinner" in res.advice or "Italian" in res.advice or "500" in res.advice


@pytest.mark.asyncio
async def test_simulate_purchase_caution():
    service = AIService()
    # High impact purchase (8,000 out of 10,000 remaining)
    res = await service.simulate_purchase(
        amount=Decimal("8000.00"),
        category_name="Shopping",
        description="Smart TV",
        total_spent=Decimal("15000.00"),
        budget_limit=Decimal("25000.00"),
        days_elapsed=15,
        total_days=30,
    )
    assert res.verdict == "caution"
    assert res.can_proceed is True
    assert res.projected_remaining_budget == Decimal("2000.00")
    assert "Caution" in res.headline or "buffer" in res.headline


@pytest.mark.asyncio
async def test_simulate_purchase_over_budget():
    service = AIService()
    res = await service.simulate_purchase(
        amount=Decimal("6000.00"),
        category_name="Travel",
        description="Weekend flight",
        total_spent=Decimal("18000.00"),
        budget_limit=Decimal("20000.00"),
        days_elapsed=20,
        total_days=30,
    )
    assert res.verdict == "over_budget"
    assert res.can_proceed is False
    assert res.projected_remaining_budget < Decimal("0.00")
    assert "Over-Budget" in res.headline


@pytest.mark.asyncio
async def test_simulate_purchase_no_budget():
    service = AIService()
    res = await service.simulate_purchase(
        amount=Decimal("300.00"),
        category_name="Groceries",
        description="Vegetables",
        total_spent=Decimal("4000.00"),
        budget_limit=None,
        days_elapsed=10,
        total_days=30,
    )
    assert res.verdict == "safe"
    assert res.can_proceed is True


@pytest.mark.asyncio
async def test_safe_to_spend_calculations():
    service = AIService()
    # Case 1: Optimal pacing
    res = await service.calculate_safe_to_spend(
        total_spent=Decimal("3000.00"),
        budget_limit=Decimal("15000.00"),
        days_elapsed=10,
        total_days=30,
    )
    assert res.status == "optimal"
    assert res.safe_daily_allowance == Decimal("600.00")  # 12000 / 20 days
    assert res.current_daily_burn_rate == Decimal("300.00")  # 3000 / 10 days
    assert res.days_remaining == 20

    # Case 2: Budget exhausted
    res_danger = await service.calculate_safe_to_spend(
        total_spent=Decimal("16000.00"),
        budget_limit=Decimal("15000.00"),
        days_elapsed=15,
        total_days=30,
    )
    assert res_danger.status == "danger"
    assert res_danger.remaining_budget == Decimal("-1000.00")
    assert res_danger.safe_daily_allowance == Decimal("0.00")


@pytest.mark.asyncio
async def test_financial_health_score_bounds():
    service = AIService()
    categories = [
        {"category_name": "Food & Dining", "total": "4000.00", "percentage": 40.0},
        {"category_name": "Transportation", "total": "2000.00", "percentage": 20.0},
        {"category_name": "Utilities", "total": "1500.00", "percentage": 15.0},
    ]
    res = await service.calculate_health_score(
        total_spent=Decimal("7500.00"),
        budget_limit=Decimal("20000.00"),
        days_elapsed=15,
        total_days=30,
        category_breakdown=categories,
    )
    assert 0 <= res.score <= 100
    assert res.status in ["excellent", "good", "needs_attention"]
    assert len(res.pillars) == 3
    pillar_names = [p.name for p in res.pillars]
    assert "Budget Adherence" in pillar_names
    assert "Savings Velocity" in pillar_names
    assert "Category Discipline" in pillar_names
    assert len(res.recommendations) >= 1


@pytest.mark.asyncio
async def test_analyze_spending_leaks():
    service = AIService()
    today = date.today()
    expenses = [
        MockExpense(Decimal("120.00"), "Chai Point", "Food & Dining", today - timedelta(days=2)),
        MockExpense(Decimal("120.00"), "Chai Point", "Food & Dining", today - timedelta(days=10)),
        MockExpense(Decimal("120.00"), "Chai Point", "Food & Dining", today - timedelta(days=20)),
        MockExpense(Decimal("50.00"), "Quick Delivery Fee", "Groceries", today - timedelta(days=5)),
        MockExpense(Decimal("50.00"), "Quick Delivery Fee", "Groceries", today - timedelta(days=15)),
        MockExpense(Decimal("2500.00"), "Large Shopping", "Shopping", today - timedelta(days=7)),
    ]
    res = await service.analyze_spending_leaks(expenses, threshold=Decimal("150.00"))
    assert res.total_monthly_leak > Decimal("0.00")
    assert res.total_annual_leak > Decimal("0.00")
    assert len(res.leaks) >= 1
    patterns = [leak.merchant_or_pattern for leak in res.leaks]
    assert any("Chai" in p for p in patterns)


@pytest.mark.asyncio
async def test_audit_subscriptions():
    service = AIService()
    today = date.today()
    recurring = [
        MockExpense(Decimal("649.00"), "Netflix 4K", "Entertainment", today - timedelta(days=15), is_recurring=True, recurring_frequency="monthly"),
        MockExpense(Decimal("149.00"), "Spotify Premium", "Entertainment", today - timedelta(days=12), is_recurring=True, recurring_frequency="monthly"),
        MockExpense(Decimal("999.00"), "Gym Membership", "Health", today - timedelta(days=5), is_recurring=True, recurring_frequency="monthly"),
    ]
    res = await service.audit_subscriptions([], recurring)
    assert res.total_monthly_commitment == Decimal("1797.00")
    assert res.total_annual_commitment == Decimal("1797.00") * Decimal("12")
    assert len(res.active_subscriptions) == 3
    # Check duplicate category warning for Entertainment
    assert any("Entertainment" in w for w in res.duplicate_warnings)
