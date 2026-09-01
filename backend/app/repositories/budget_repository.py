from decimal import Decimal
from typing import List, Optional
import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.budget import Budget
from app.utils.datetime import get_current_date


class BudgetRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    def _resolve_period(self, period_type: str = "month", period_key: Optional[str] = None) -> tuple[str, str]:
        pt = period_type if period_type in ("month", "week", "day") else "month"
        if period_key:
            return pt, period_key

        today = get_current_date()
        if pt == "day":
            return pt, today.strftime("%Y-%m-%d")
        elif pt == "week":
            return pt, f"{today.year}-W{today.isocalendar()[1]:02d}"
        else:
            return pt, today.strftime("%Y-%m")

    async def get_budget(
        self, user_id: uuid.UUID, period_type: str = "month", period_key: Optional[str] = None
    ) -> Optional[Budget]:
        pt, pk = self._resolve_period(period_type, period_key)
        stmt = (
            select(Budget)
            .where(
                Budget.user_id == user_id,
                Budget.period_type == pt,
                Budget.period_key == pk,
            )
            .limit(1)
        )
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def list_budgets(self, user_id: uuid.UUID, period_type: Optional[str] = None) -> List[Budget]:
        stmt = select(Budget).where(Budget.user_id == user_id)
        if period_type:
            stmt = stmt.where(Budget.period_type == period_type)
        stmt = stmt.order_by(Budget.period_key.desc(), Budget.created_at.desc())
        result = await self.db.execute(stmt)
        return list(result.scalars().all())

    async def upsert_budget(
        self,
        user_id: uuid.UUID,
        amount: Decimal,
        period_type: str = "month",
        period_key: Optional[str] = None,
    ) -> Budget:
        pt, pk = self._resolve_period(period_type, period_key)
        budget = await self.get_budget(user_id, pt, pk)
        month_val = pk if pt == "month" else None
        if budget:
            budget.amount = amount
            budget.month = month_val
        else:
            budget = Budget(
                id=uuid.uuid4(),
                user_id=user_id,
                period_type=pt,
                period_key=pk,
                month=month_val,
                amount=amount,
            )
            self.db.add(budget)
        await self.db.flush()
        await self.db.refresh(budget)
        return budget

    async def delete_budget(
        self, user_id: uuid.UUID, period_type: str = "month", period_key: Optional[str] = None
    ) -> bool:
        pt, pk = self._resolve_period(period_type, period_key)
        budget = await self.get_budget(user_id, pt, pk)
        if budget:
            await self.db.delete(budget)
            await self.db.flush()
            return True
        return False
