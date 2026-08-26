import uuid
from datetime import datetime
from pydantic import BaseModel, ConfigDict


class UserBase(BaseModel):
    display_name: str


class UserRead(UserBase):
    id: uuid.UUID
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
