import uuid
from typing import List, Optional

from sqlalchemy import func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError
from app.db.models.expense import Expense
from app.db.models.payment_method import PaymentMethod


class PaymentMethodRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, id: uuid.UUID, user_id: Optional[uuid.UUID] = None) -> Optional[PaymentMethod]:
        stmt = select(PaymentMethod).where(PaymentMethod.id == id)
        if user_id is not None:
            stmt = stmt.where(or_(PaymentMethod.is_default == True, PaymentMethod.user_id == user_id))  # noqa: E712
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_name(self, name: str, user_id: Optional[uuid.UUID] = None) -> Optional[PaymentMethod]:
        stmt = select(PaymentMethod).where(func.lower(PaymentMethod.name) == name.lower().strip())
        if user_id is not None:
            stmt = stmt.where(or_(PaymentMethod.is_default == True, PaymentMethod.user_id == user_id))  # noqa: E712
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def list_all(self, user_id: Optional[uuid.UUID] = None) -> List[PaymentMethod]:
        stmt = select(PaymentMethod)
        if user_id is not None:
            stmt = stmt.where(or_(PaymentMethod.is_default == True, PaymentMethod.user_id == user_id))  # noqa: E712
        stmt = stmt.order_by(PaymentMethod.is_default.desc(), PaymentMethod.name.asc())
        result = await self.db.execute(stmt)
        return list(result.scalars().all())

    async def create(self, name: str, user_id: Optional[uuid.UUID] = None) -> PaymentMethod:
        pm = PaymentMethod(name=name.strip(), is_default=False, user_id=user_id)
        self.db.add(pm)
        await self.db.flush()
        await self.db.refresh(pm)
        return pm

    async def update(self, pm: PaymentMethod, name: str) -> PaymentMethod:
        pm.name = name.strip()
        self.db.add(pm)
        await self.db.flush()
        await self.db.refresh(pm)
        return pm

    async def delete_payment_method(self, id: uuid.UUID, user_id: uuid.UUID) -> None:
        # Atomic transaction: reassign user's referencing expenses to another existing visible payment method, then delete
        stmt = (
            select(PaymentMethod)
            .where(
                PaymentMethod.id != id,
                or_(PaymentMethod.is_default == True, PaymentMethod.user_id == user_id),  # noqa: E712
            )
            .limit(1)
        )
        res = await self.db.execute(stmt)
        fallback = res.scalar_one_or_none()
        if not fallback:
            raise ConflictError("Cannot delete the only remaining payment method. Please create another payment method first.")

        # 1. Update referencing expenses for this user to fallback payment method
        select_stmt = select(Expense).where(Expense.payment_method_id == id, Expense.user_id == user_id)
        expenses_result = await self.db.execute(select_stmt)
        expenses = expenses_result.scalars().all()
        for expense in expenses:
            expense.payment_method_id = fallback.id
            self.db.add(expense)

        await self.db.flush()

        # 2. Delete the payment method (only if owned by user and not default)
        pm = await self.get_by_id(id, user_id=user_id)
        if pm:
            if pm.is_default:
                raise ConflictError("Default starter payment methods cannot be deleted.")
            if pm.user_id != user_id:
                raise ConflictError("Cannot delete payment method belonging to another user.")
            await self.db.delete(pm)
            await self.db.flush()
