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


class MockBudget:
    def __init__(self, amount: Decimal):
        self.amount = amount


@pytest.mark.asyncio
async def test_parse_sms_hdfc_upi():
    service = AIService()
    sms = "HDFC Bank: Rs 850.00 debited from a/c **1234 on 02-Sep-26 to ZOMATO. UPI Ref 324156"
    res = await service.parse_sms_text(sms)

    assert res.amount == Decimal("850.00")
    assert "ZOMATO" in res.merchant.upper() or "Zomato" in res.merchant
    assert res.payment_method_name == "UPI"
    assert res.date == "2026-09-02"
    assert "324156" in (res.reference_id or "")
    assert res.transaction_type == "debit"


@pytest.mark.asyncio
async def test_parse_sms_axis_credit_card():
    service = AIService()
    sms = "Axis Bank: Rs. 1500.00 spent on your Credit Card XX4321 on 03-Sep-26 at SHELL PETROL. Avl Lmt: 45000"
    res = await service.parse_sms_text(sms)

    assert res.amount == Decimal("1500.00")
    assert "SHELL" in res.merchant.upper()
    assert res.payment_method_name == "Credit Card"
    assert res.date == "2026-09-03"
    assert res.category_name in ["Transportation", "Other"]


@pytest.mark.asyncio
async def test_parse_sms_swiggy_credit():
    service = AIService()
    sms = "Rs 250.00 credited to your A/C from SWIGGY REFUND. Ref: 987654"
    res = await service.parse_sms_text(sms)

    assert res.amount == Decimal("250.00")
    assert res.transaction_type == "credit"
    assert "987654" in (res.reference_id or "")


@pytest.mark.asyncio
async def test_fifty_thirty_twenty_calculation():
    service = AIService()
    today = date(2026, 9, 4)

    # Total 10,000 spent: Needs (6,000), Wants (3,000), Savings (1,000)
    expenses = [
        MockExpense(Decimal("3000.00"), "Apartment rent", "Housing", today),
        MockExpense(Decimal("2000.00"), "Monthly grocery run", "Groceries", today),
        MockExpense(Decimal("1000.00"), "Electricity bill", "Utilities", today),
        MockExpense(Decimal("2000.00"), "Weekend dinner", "Dining Out", today),
        MockExpense(Decimal("1000.00"), "New sneakers", "Shopping", today),
        MockExpense(Decimal("1000.00"), "Index fund SIP", "Investments", today),
    ]

    res = await service.calculate_fifty_thirty_twenty(
        expenses=expenses,
        total_spent=Decimal("10000.00"),
        budget_limit=Decimal("12000.00"),
    )

    assert res.total_spent == Decimal("10000.00")
    assert res.target_budget == Decimal("12000.00")
    assert res.needs.actual_amount == Decimal("6000.00")
    assert res.wants.actual_amount == Decimal("3000.00")
    assert res.savings.actual_amount == Decimal("1000.00")
    assert res.needs.actual_percentage == 60.0
    assert res.wants.actual_percentage == 30.0
    assert res.savings.actual_percentage == 10.0
    assert res.adherence_score > 0
    assert len(res.rebalance_advice) > 0


@pytest.mark.asyncio
async def test_achievements_calculation():
    service = AIService()
    today = date(2026, 9, 4)

    expenses = [
        MockExpense(Decimal("50.00"), "Chai", "Food", today - timedelta(days=1)),
        MockExpense(Decimal("80.00"), "Snacks", "Food", today - timedelta(days=2)),
        MockExpense(Decimal("1200.00"), "Groceries", "Groceries", today - timedelta(days=3)),
    ]

    budget = MockBudget(Decimal("20000.00"))

    res = await service.calculate_achievements(
        expenses=expenses,
        budget=budget,
        past_expenses=expenses,
    )

    assert len(res.badges) >= 4
    # User spent 1,330 out of 20,000 -> Budget Champion should be unlocked
    champ_badge = next((b for b in res.badges if b.id == "budget_champion"), None)
    assert champ_badge is not None
    assert champ_badge.is_unlocked is True
    assert res.active_streak_days >= 1
    assert len(res.motivation_quote) > 10
