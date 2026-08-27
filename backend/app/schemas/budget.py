import uuid
from decimal import Decimal
from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_serializer


class BudgetBase(BaseModel):
    amount: Decimal = Field(..., ge=0, max_digits=12, decimal_places=2)
    period_type: str = Field("month", description="Budget granularity: month, week, or day")
    period_key: Optional[str] = Field(None, description="Identifier for period: YYYY-MM, YYYY-Www, or YYYY-MM-DD")
    month: Optional[str] = Field(None, description="Optional legacy month field")


class BudgetCreate(BudgetBase):
    pass


class BudgetRead(BaseModel):
    id: Optional[uuid.UUID] = None
    period_type: str = "month"
    period_key: Optional[str] = None
    month: Optional[str] = None
    amount: Optional[Decimal] = None
    updated_at: Optional[datetime] = None

    model_config = ConfigDict(from_attributes=True)

    @field_serializer("amount")
    def serialize_amount(self, v: Optional[Decimal]) -> Optional[str]:
        if v is not None:
            return f"{v:.2f}"
        return None
