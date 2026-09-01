import uuid
from typing import List

from fastapi import APIRouter, Depends, status

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.schemas.common import DataEnvelope
from app.schemas.payment_method import PaymentMethodCreate, PaymentMethodRead, PaymentMethodUpdate
from app.services.payment_method_service import PaymentMethodService

router = APIRouter()


@router.post("", response_model=DataEnvelope[PaymentMethodRead], status_code=status.HTTP_201_CREATED)
async def create_payment_method(
    data: PaymentMethodCreate,
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """Create a new custom payment method."""
    service = PaymentMethodService(db)
    pm = await service.create_payment_method(data.name, current_user.id)
    return {"data": PaymentMethodRead.model_validate(pm)}


@router.get("", response_model=DataEnvelope[List[PaymentMethodRead]])
async def list_payment_methods(
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """List all visible payment methods (starter and user's custom)."""
    service = PaymentMethodService(db)
    pms = await service.list_payment_methods(current_user.id)
    return {"data": [PaymentMethodRead.model_validate(pm) for pm in pms]}


@router.patch("/{payment_method_id}", response_model=DataEnvelope[PaymentMethodRead])
async def rename_payment_method(
    payment_method_id: uuid.UUID,
    data: PaymentMethodUpdate,
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """Rename an existing payment method (starter or user-owned custom)."""
    service = PaymentMethodService(db)
    pm = await service.rename_payment_method(payment_method_id, data.name, current_user.id)
    return {"data": PaymentMethodRead.model_validate(pm)}


@router.delete("/{payment_method_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_payment_method(
    payment_method_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> None:
    """Delete a custom payment method. Referencing expenses are reassigned to 'Other'."""
    service = PaymentMethodService(db)
    await service.delete_payment_method(payment_method_id, current_user.id)
