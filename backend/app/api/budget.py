from typing import List, Optional
from datetime import datetime

from fastapi import APIRouter, Depends, Query, status

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.schemas.budget import BudgetCreate, BudgetRead
from app.schemas.common import DataEnvelope
from app.services.budget_service import BudgetService

router = APIRouter()


@router.get("", response_model=DataEnvelope[BudgetRead])
async def get_budget(
    period_type: str = Query("month", pattern=r"^(month|week|day)$", description="Granularity: month, week, or day"),
    period_key: Optional[str] = Query(None, description="Identifier for period: YYYY-MM, YYYY-Www, or YYYY-MM-DD"),
    month: Optional[str] = Query(None, pattern=r"^\d{4}-\d{2}$", description="Legacy month alias"),
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """Retrieve the budget configuration for a specific period (month, week, or day)."""
    service = BudgetService(db)
    resolved_type = "month" if month else period_type
    resolved_key = month or period_key

    budget = await service.get_budget(current_user.id, resolved_type, resolved_key)
    if not budget:
        return {
            "data": BudgetRead(
                period_type=resolved_type,
                period_key=resolved_key,
                month=resolved_key if resolved_type == "month" else None,
                amount=None,
                updated_at=None,
            )
        }
    return {"data": BudgetRead.model_validate(budget)}


@router.get("/all", response_model=DataEnvelope[List[BudgetRead]])
async def list_budgets(
    period_type: Optional[str] = Query(None, pattern=r"^(month|week|day)$", description="Filter by granularity"),
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """List all configured budgets for the authenticated user."""
    service = BudgetService(db)
    budgets = await service.list_budgets(current_user.id, period_type)
    return {"data": [BudgetRead.model_validate(b) for b in budgets]}


@router.put("", response_model=DataEnvelope[BudgetRead])
async def upsert_budget(
    data: BudgetCreate,
    period_type: Optional[str] = Query(None, pattern=r"^(month|week|day)$"),
    period_key: Optional[str] = Query(None),
    month: Optional[str] = Query(None, pattern=r"^\d{4}-\d{2}$"),
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """Create or update a budget for a specific month, week, or day."""
    service = BudgetService(db)
    resolved_type = data.period_type or period_type or ("month" if (month or data.month) else "month")
    resolved_key = data.period_key or period_key or data.month or month
    budget = await service.upsert_budget(current_user.id, data.amount, resolved_type, resolved_key)
    return {"data": BudgetRead.model_validate(budget)}


@router.delete("", status_code=status.HTTP_204_NO_CONTENT)
async def delete_budget(
    period_type: str = Query("month", pattern=r"^(month|week|day)$"),
    period_key: Optional[str] = Query(None),
    month: Optional[str] = Query(None, pattern=r"^\d{4}-\d{2}$"),
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> None:
    """Delete a budget configuration for a specific month, week, or day."""
    service = BudgetService(db)
    resolved_type = "month" if month else period_type
    resolved_key = month or period_key
    await service.delete_budget(current_user.id, resolved_type, resolved_key)
