from decimal import Decimal
from typing import Optional

from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.budget import Budget
from app.repositories.budget_repository import BudgetRepository


class BudgetService:
    def __init__(self, db: AsyncSession):
        self.repo = BudgetRepository(db)

    async def get_budget(self) -> Optional[Budget]:
        return await self.repo.get_budget()

    async def upsert_budget(self, amount: Decimal) -> Budget:
        return await self.repo.upsert_budget(amount)

    async def delete_budget(self) -> None:
        await self.repo.delete_budget()
