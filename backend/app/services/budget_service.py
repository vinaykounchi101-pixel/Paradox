from decimal import Decimal
from typing import List, Optional

from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.budget import Budget
from app.repositories.budget_repository import BudgetRepository


class BudgetService:
    def __init__(self, db: AsyncSession):
        self.repo = BudgetRepository(db)

    async def get_budget(self, period_type: str = "month", period_key: Optional[str] = None) -> Optional[Budget]:
        return await self.repo.get_budget(period_type, period_key)

    async def list_budgets(self, period_type: Optional[str] = None) -> List[Budget]:
        return await self.repo.list_budgets(period_type)

    async def upsert_budget(
        self, amount: Decimal, period_type: str = "month", period_key: Optional[str] = None
    ) -> Budget:
        return await self.repo.upsert_budget(amount, period_type, period_key)

    async def delete_budget(self, period_type: str = "month", period_key: Optional[str] = None) -> bool:
        return await self.repo.delete_budget(period_type, period_key)
