from decimal import Decimal
from unittest.mock import MagicMock

from app.services.dashboard_service import DashboardService


class DummyBudget:
    def __init__(self, amount: Decimal):
        self.amount = amount


def test_calculate_budget_status():
    # Instantiate service with a mock db session
    service = DashboardService(db=MagicMock())

    # Case 1: No budget set
    status_data = service._calculate_budget_status(None, Decimal("100.00"))
    assert status_data.amount is None
    assert status_data.spent == Decimal("100.00")
    assert status_data.remaining is None
    assert status_data.status is None

    # Case 2: Under budget (< 90%)
    budget = DummyBudget(Decimal("1000.00"))
    status_data = service._calculate_budget_status(budget, Decimal("899.99"))
    assert status_data.status == "under_budget"
    assert status_data.remaining == Decimal("100.01")

    # Case 3: Near limit (exactly 90%)
    status_data = service._calculate_budget_status(budget, Decimal("900.00"))
    assert status_data.status == "near_limit"
    assert status_data.remaining == Decimal("100.00")

    # Case 4: Near limit (exactly 100%)
    status_data = service._calculate_budget_status(budget, Decimal("1000.00"))
    assert status_data.status == "near_limit"
    assert status_data.remaining == Decimal("0.00")

    # Case 5: Over budget (> 100%)
    status_data = service._calculate_budget_status(budget, Decimal("1000.01"))
    assert status_data.status == "over_budget"
    assert status_data.remaining == Decimal("-0.01")
