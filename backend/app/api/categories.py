import uuid
from typing import List

from fastapi import APIRouter, Depends, status

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.schemas.category import CategoryCreate, CategoryRead, CategoryUpdate
from app.schemas.common import DataEnvelope
from app.services.category_service import CategoryService

router = APIRouter()


@router.post("", response_model=DataEnvelope[CategoryRead], status_code=status.HTTP_201_CREATED)
async def create_category(
    data: CategoryCreate,
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """Create a new custom category."""
    service = CategoryService(db)
    category = await service.create_category(data.name, current_user.id)
    return {"data": CategoryRead.model_validate(category)}


@router.get("", response_model=DataEnvelope[List[CategoryRead]])
async def list_categories(
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """List all visible categories (system starters and user's custom categories)."""
    service = CategoryService(db)
    categories = await service.list_categories(current_user.id)
    return {"data": [CategoryRead.model_validate(c) for c in categories]}


@router.get("/{category_id}", response_model=DataEnvelope[CategoryRead])
async def get_category(
    category_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """Get a category by ID."""
    service = CategoryService(db)
    category = await service.get_category(category_id, current_user.id)
    return {"data": CategoryRead.model_validate(category)}


@router.patch("/{category_id}", response_model=DataEnvelope[CategoryRead])
async def rename_category(
    category_id: uuid.UUID,
    data: CategoryUpdate,
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """Rename an existing category (starter or user-owned custom)."""
    service = CategoryService(db)
    category = await service.rename_category(category_id, data.name, current_user.id)
    return {"data": CategoryRead.model_validate(category)}


@router.delete("/{category_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_category(
    category_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> None:
    """Delete a custom category. Referencing expenses are reassigned."""
    service = CategoryService(db)
    await service.delete_category(category_id, current_user.id)
