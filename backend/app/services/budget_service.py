from decimal import Decimal
from typing import List, Optional
import uuid

from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.budget import Budget
from app.repositories.budget_repository import BudgetRepository


class BudgetService:
    def __init__(self, db: AsyncSession):
        self.repo = BudgetRepository(db)

    async def get_budget(
        self, user_id: uuid.UUID, period_type: str = "month", period_key: Optional[str] = None
    ) -> Optional[Budget]:
        return await self.repo.get_budget(user_id, period_type, period_key)

    async def list_budgets(self, user_id: uuid.UUID, period_type: Optional[str] = None) -> List[Budget]:
        return await self.repo.list_budgets(user_id, period_type)

    async def upsert_budget(
        self,
        user_id: uuid.UUID,
        amount: Decimal,
        period_type: str = "month",
        period_key: Optional[str] = None,
    ) -> Budget:
        return await self.repo.upsert_budget(user_id, amount, period_type, period_key)

    async def delete_budget(
        self, user_id: uuid.UUID, period_type: str = "month", period_key: Optional[str] = None
    ) -> bool:
        return await self.repo.delete_budget(user_id, period_type, period_key)
