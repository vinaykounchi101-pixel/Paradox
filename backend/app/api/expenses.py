import uuid
import math
from datetime import date
from typing import Optional

from fastapi import APIRouter, Depends, Query, status

from app.api.deps import get_db
from app.schemas.common import DataEnvelope, PaginatedEnvelope
from app.schemas.expense import ExpenseCreate, ExpenseRead, ExpenseUpdate
from app.services.expense_service import ExpenseService

router = APIRouter()


@router.post("", response_model=DataEnvelope[ExpenseRead], status_code=status.HTTP_201_CREATED)
async def create_expense(
    data: ExpenseCreate,
    db=Depends(get_db),
) -> dict:
    """Create a new expense transaction."""
    service = ExpenseService(db)
    expense = await service.create_expense(data)
    return {"data": ExpenseRead.model_validate(expense)}


@router.get("", response_model=PaginatedEnvelope[ExpenseRead])
async def list_expenses(
    search: Optional[str] = Query(None),
    category_id: Optional[uuid.UUID] = Query(None),
    date_from: Optional[date] = Query(None),
    date_to: Optional[date] = Query(None),
    sort_by: str = Query("date", pattern="^(date|amount|category)$"),
    sort_order: str = Query("desc", pattern="^(asc|desc)$"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db=Depends(get_db),
) -> dict:
    """List expenses with free-text search, pagination, and sorting.
    Supports single-dimension filtering by date range OR category (mutually exclusive).
    """
    service = ExpenseService(db)
    expenses, total_items = await service.list_expenses(
        search=search,
        category_id=category_id,
        date_from=date_from,
        date_to=date_to,
        sort_by=sort_by,
        sort_order=sort_order,
        page=page,
        page_size=page_size,
    )
    total_pages = math.ceil(total_items / page_size) if total_items > 0 else 0
    return {
        "data": [ExpenseRead.model_validate(e) for e in expenses],
        "meta": {
            "page": page,
            "page_size": page_size,
            "total_items": total_items,
            "total_pages": total_pages,
        },
    }


@router.get("/{expense_id}", response_model=DataEnvelope[ExpenseRead])
async def get_expense(
    expense_id: uuid.UUID,
    db=Depends(get_db),
) -> dict:
    """Retrieve details of a single expense."""
    service = ExpenseService(db)
    expense = await service.get_expense(expense_id)
    return {"data": ExpenseRead.model_validate(expense)}


@router.patch("/{expense_id}", response_model=DataEnvelope[ExpenseRead])
async def update_expense(
    expense_id: uuid.UUID,
    data: ExpenseUpdate,
    db=Depends(get_db),
) -> dict:
    """Partially update an existing expense transaction."""
    service = ExpenseService(db)
    expense = await service.update_expense(expense_id, data)
    return {"data": ExpenseRead.model_validate(expense)}


@router.delete("/{expense_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_expense(
    expense_id: uuid.UUID,
    db=Depends(get_db),
) -> None:
    """Delete an expense transaction."""
    service = ExpenseService(db)
    await service.delete_expense(expense_id)
