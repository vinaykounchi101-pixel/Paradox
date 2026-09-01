import uuid
from decimal import Decimal
from datetime import date
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.core.exceptions import NotFoundError
from app.db.models.category import Category
from app.db.models.expense import Expense
from app.db.models.payment_method import PaymentMethod
from app.schemas.expense import ExpenseCreate
from app.services.category_service import CategoryService
from app.services.dashboard_service import DashboardService
from app.services.expense_service import ExpenseService


@pytest.mark.asyncio
async def test_expense_service_user_isolation_get_other_user_expense():
    mock_db = MagicMock()
    service = ExpenseService(mock_db)

    user_a_id = uuid.uuid4()
    user_b_id = uuid.uuid4()
    expense_id = uuid.uuid4()

    # Repository returns None when queried with user_b_id because user_b does not own the expense
    service.repo.get_by_id = AsyncMock(return_value=None)

    with pytest.raises(NotFoundError):
        await service.get_expense(id=expense_id, user_id=user_b_id)


@pytest.mark.asyncio
async def test_expense_service_create_sets_correct_user_id():
    mock_db = MagicMock()
    service = ExpenseService(mock_db)

    user_id = uuid.uuid4()
    cat_id = uuid.uuid4()
    pm_id = uuid.uuid4()

    service.category_repo.get_by_id = AsyncMock(return_value=Category(id=cat_id, name="Food", is_default=True))
    service.pm_repo.get_by_id = AsyncMock(return_value=PaymentMethod(id=pm_id, name="Cash", is_default=True))
    service.repo.create = AsyncMock(side_effect=lambda exp: exp)

    create_data = ExpenseCreate(
        amount=Decimal("45.50"),
        category_id=cat_id,
        payment_method_id=pm_id,
        date=date(2026, 8, 20),
        description="Lunch",
    )

    created_expense = await service.create_expense(user_id=user_id, data=create_data)
    assert created_expense.user_id == user_id
    assert created_expense.amount == Decimal("45.50")


@pytest.mark.asyncio
async def test_dashboard_service_user_isolation():
    mock_db = MagicMock()
    service = DashboardService(mock_db)

    user_a_id = uuid.uuid4()
    user_b_id = uuid.uuid4()

    # Mock expense repo to return expenses only for user_a
    service.expense_repo.get_expenses_for_period = AsyncMock(return_value=[])
    service.expense_repo.list_expenses = AsyncMock(return_value=([], 0))
    service.budget_repo.get_budget = AsyncMock(return_value=None)

    dashboard_b = await service.get_dashboard_data(user_id=user_b_id, period="current_month")

    # Verify query strictly uses user_b_id
    service.expense_repo.get_expenses_for_period.assert_called_with(user_b_id, date(2026, 8, 1), date.today())
    assert dashboard_b.total_spent == Decimal("0.00")
    assert len(dashboard_b.recent_expenses) == 0
