import pytest
from decimal import Decimal
from app.schemas.budget import BudgetCreate, BudgetRead

def test_budget_schemas_all_granularity():
    # Month
    m = BudgetCreate(amount=Decimal("1500.00"), period_type="month", period_key="2026-08")
    assert m.period_type == "month"
    assert m.period_key == "2026-08"

    # Week
    w = BudgetCreate(amount=Decimal("350.00"), period_type="week", period_key="2026-W35")
    assert w.period_type == "week"
    assert w.period_key == "2026-W35"

    # Day
    d = BudgetCreate(amount=Decimal("50.00"), period_type="day", period_key="2026-08-27")
    assert d.period_type == "day"
    assert d.period_key == "2026-08-27"

    # Serialization test
    r = BudgetRead(amount=Decimal("250.50"), period_type="week", period_key="2026-W35")
    dump = r.model_dump()
    assert dump["amount"] == "250.50"
    assert dump["period_type"] == "week"
    assert dump["period_key"] == "2026-W35"
