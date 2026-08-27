from decimal import Decimal
from datetime import date
from typing import List, Optional
import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.budget import Budget
from app.utils.datetime import get_current_date


class BudgetRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    def _resolve_month(self, month: Optional[str] = None) -> str:
        if month:
            return month
        today = get_current_date()
        return today.strftime("%Y-%m")

    async def get_budget(self, month: Optional[str] = None) -> Optional[Budget]:
        resolved_month = self._resolve_month(month)
        stmt = select(Budget).where(Budget.month == resolved_month).limit(1)
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def list_budgets(self) -> List[Budget]:
        stmt = select(Budget).order_by(Budget.month.desc())
        result = await self.db.execute(stmt)
        return list(result.scalars().all())

    async def upsert_budget(self, amount: Decimal, month: Optional[str] = None) -> Budget:
        resolved_month = self._resolve_month(month)
        budget = await self.get_budget(resolved_month)
        if budget:
            budget.amount = amount
        else:
            budget = Budget(
                id=uuid.uuid4(),
                month=resolved_month,
                amount=amount,
            )
            self.db.add(budget)
        await self.db.flush()
        return budget

    async def delete_budget(self, month: Optional[str] = None) -> bool:
        resolved_month = self._resolve_month(month)
        budget = await self.get_budget(resolved_month)
        if budget:
            await self.db.delete(budget)
            await self.db.flush()
            return True
        return False
