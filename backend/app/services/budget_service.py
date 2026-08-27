from decimal import Decimal
from typing import List, Optional

from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.budget import Budget
from app.repositories.budget_repository import BudgetRepository


class BudgetService:
    def __init__(self, db: AsyncSession):
        self.repo = BudgetRepository(db)

    async def get_budget(self, month: Optional[str] = None) -> Optional[Budget]:
        return await self.repo.get_budget(month)

    async def list_budgets(self) -> List[Budget]:
        return await self.repo.list_budgets()

    async def upsert_budget(self, amount: Decimal, month: Optional[str] = None) -> Budget:
        return await self.repo.upsert_budget(amount, month)

    async def delete_budget(self, month: Optional[str] = None) -> bool:
        return await self.repo.delete_budget(month)
