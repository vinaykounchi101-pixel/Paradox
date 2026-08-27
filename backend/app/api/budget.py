from typing import List, Optional
from datetime import datetime

from fastapi import APIRouter, Depends, Query, status

from app.api.deps import get_db
from app.schemas.budget import BudgetCreate, BudgetRead
from app.schemas.common import DataEnvelope
from app.services.budget_service import BudgetService

router = APIRouter()


@router.get("", response_model=DataEnvelope[BudgetRead])
async def get_budget(
    month: Optional[str] = Query(None, pattern=r"^\d{4}-\d{2}$", description="Target month in YYYY-MM format"),
    db=Depends(get_db),
) -> dict:
    """Retrieve the monthly budget configuration for the specified month (default: current month)."""
    service = BudgetService(db)
    target_month = month or datetime.utcnow().strftime("%Y-%m")
    budget = await service.get_budget(target_month)
    if not budget:
        return {"data": BudgetRead(month=target_month, amount=None, updated_at=None)}
    return {"data": BudgetRead.model_validate(budget)}


@router.get("/all", response_model=DataEnvelope[List[BudgetRead]])
async def list_budgets(
    db=Depends(get_db),
) -> dict:
    """List all configured monthly budgets."""
    service = BudgetService(db)
    budgets = await service.list_budgets()
    return {"data": [BudgetRead.model_validate(b) for b in budgets]}


@router.put("", response_model=DataEnvelope[BudgetRead])
async def upsert_budget(
    data: BudgetCreate,
    month: Optional[str] = Query(None, pattern=r"^\d{4}-\d{2}$", description="Target month in YYYY-MM format"),
    db=Depends(get_db),
) -> dict:
    """Create or update the monthly budget configuration for a month."""
    service = BudgetService(db)
    target_month = data.month or month or datetime.utcnow().strftime("%Y-%m")
    budget = await service.upsert_budget(data.amount, target_month)
    return {"data": BudgetRead.model_validate(budget)}


@router.delete("", status_code=status.HTTP_204_NO_CONTENT)
async def delete_budget(
    month: Optional[str] = Query(None, pattern=r"^\d{4}-\d{2}$", description="Target month in YYYY-MM format"),
    db=Depends(get_db),
) -> None:
    """Delete the monthly budget configuration for a month."""
    service = BudgetService(db)
    target_month = month or datetime.utcnow().strftime("%Y-%m")
    await service.delete_budget(target_month)


@router.delete("/{month}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_budget_by_path(
    month: str,
    db=Depends(get_db),
) -> None:
    """Delete the monthly budget configuration by path parameter."""
    service = BudgetService(db)
    await service.delete_budget(month)
