from fastapi import APIRouter, Depends, status

from app.api.deps import get_db
from app.schemas.budget import BudgetCreate, BudgetRead
from app.schemas.common import DataEnvelope
from app.services.budget_service import BudgetService

router = APIRouter()


@router.get("", response_model=DataEnvelope[BudgetRead])
async def get_budget(
    db=Depends(get_db),
) -> dict:
    """Retrieve the current monthly budget configuration."""
    service = BudgetService(db)
    budget = await service.get_budget()
    if not budget:
        # Documented empty state (return amount: null, not a 404)
        return {"data": BudgetRead(amount=None, updated_at=None)}
    return {"data": BudgetRead.model_validate(budget)}


@router.put("", response_model=DataEnvelope[BudgetRead])
async def upsert_budget(
    data: BudgetCreate,
    db=Depends(get_db),
) -> dict:
    """Create or update the single monthly budget configuration."""
    service = BudgetService(db)
    budget = await service.upsert_budget(data.amount)
    return {"data": BudgetRead.model_validate(budget)}
