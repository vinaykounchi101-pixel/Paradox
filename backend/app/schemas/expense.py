import uuid
from decimal import Decimal
from datetime import date as date_type, datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_serializer, field_validator

from app.schemas.category import CategoryRead
from app.schemas.payment_method import PaymentMethodRead


class ExpenseBase(BaseModel):
    amount: Decimal = Field(..., gt=0, max_digits=12, decimal_places=2)
    category_id: uuid.UUID
    payment_method_id: uuid.UUID
    date: date_type
    description: Optional[str] = Field(None, max_length=255)

    @field_validator("description")
    @classmethod
    def strip_description(cls, v: Optional[str]) -> Optional[str]:
        if v is not None:
            return v.strip() or None
        return v


class ExpenseCreate(ExpenseBase):
    pass


class ExpenseUpdate(BaseModel):
    amount: Optional[Decimal] = Field(None, gt=0, max_digits=12, decimal_places=2)
    category_id: Optional[uuid.UUID] = None
    payment_method_id: Optional[uuid.UUID] = None
    date: Optional[date_type] = None
    description: Optional[str] = Field(None, max_length=255)

    @field_validator("description")
    @classmethod
    def strip_description(cls, v: Optional[str]) -> Optional[str]:
        if v is not None:
            return v.strip() or None
        return v


class ExpenseRead(ExpenseBase):
    id: uuid.UUID
    created_at: datetime
    updated_at: datetime
    category: Optional[CategoryRead] = None
    payment_method: Optional[PaymentMethodRead] = None

    model_config = ConfigDict(from_attributes=True)

    @field_serializer("amount")
    def serialize_amount(self, v: Decimal) -> str:
        return f"{v:.2f}"
