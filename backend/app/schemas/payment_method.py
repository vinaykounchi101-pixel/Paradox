import uuid
from datetime import datetime
from pydantic import BaseModel, ConfigDict, Field, field_validator


class PaymentMethodBase(BaseModel):
    name: str = Field(..., max_length=60)

    @field_validator("name")
    @classmethod
    def validate_name(cls, v: str) -> str:
        stripped = v.strip()
        if not stripped:
            raise ValueError("name must not be empty or whitespace only")
        return stripped


class PaymentMethodCreate(PaymentMethodBase):
    pass


class PaymentMethodUpdate(PaymentMethodBase):
    pass


class PaymentMethodRead(PaymentMethodBase):
    id: uuid.UUID
    is_default: bool
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
