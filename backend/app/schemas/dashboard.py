import uuid
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, field_serializer

from app.schemas.expense import ExpenseRead


class DashboardCategoryBreakdown(BaseModel):
    category_id: uuid.UUID
    category_name: str
    total: Decimal
    percentage: float

    @field_serializer("total")
    def serialize_total(self, v: Decimal) -> str:
        return f"{v:.2f}"


class DashboardTopCategory(BaseModel):
    category_id: uuid.UUID
    category_name: str
    total: Decimal

    @field_serializer("total")
    def serialize_total(self, v: Decimal) -> str:
        return f"{v:.2f}"


class DashboardTrendItem(BaseModel):
    label: str
    total: Decimal

    @field_serializer("total")
    def serialize_total(self, v: Decimal) -> str:
        return f"{v:.2f}"


class DashboardBudget(BaseModel):
    amount: Optional[Decimal] = None
    spent: Decimal
    remaining: Optional[Decimal] = None
    status: Optional[str] = None

    @field_serializer("amount", "spent", "remaining")
    def serialize_money(self, v: Optional[Decimal]) -> Optional[str]:
        if v is not None:
            return f"{v:.2f}"
        return None


class DashboardRead(BaseModel):
    period: str
    total_spent: Decimal
    budget: DashboardBudget
    category_breakdown: List[DashboardCategoryBreakdown]
    top_categories: List[DashboardTopCategory]
    trend: List[DashboardTrendItem]
    recent_expenses: List[ExpenseRead]

    @field_serializer("total_spent")
    def serialize_total_spent(self, v: Decimal) -> str:
        return f"{v:.2f}"
