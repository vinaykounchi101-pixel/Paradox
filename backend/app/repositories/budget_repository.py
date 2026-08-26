from decimal import Decimal
from typing import Optional
import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.budget import Budget


class BudgetRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_budget(self) -> Optional[Budget]:
        stmt = select(Budget).limit(1)
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def upsert_budget(self, amount: Decimal) -> Budget:
        budget = await self.get_budget()
        if budget:
            budget.amount = amount
        else:
            budget = Budget(
                id=uuid.uuid4(),
                amount=amount,
            )
        self.db.add(budget)
        await self.db.flush()
        return budget
