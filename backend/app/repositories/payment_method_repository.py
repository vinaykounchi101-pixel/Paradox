import uuid
from typing import List, Optional

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.payment_method import PaymentMethod
from app.db.models.expense import Expense
from app.constants.payment_methods import OTHER_PAYMENT_METHOD_ID


class PaymentMethodRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, id: uuid.UUID) -> Optional[PaymentMethod]:
        stmt = select(PaymentMethod).where(PaymentMethod.id == id)
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_name(self, name: str) -> Optional[PaymentMethod]:
        stmt = select(PaymentMethod).where(func.lower(PaymentMethod.name) == name.lower())
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def list_all(self) -> List[PaymentMethod]:
        stmt = select(PaymentMethod).order_by(PaymentMethod.is_default.desc(), PaymentMethod.name.asc())
        result = await self.db.execute(stmt)
        return list(result.scalars().all())

    async def create(self, name: str) -> PaymentMethod:
        pm = PaymentMethod(name=name, is_default=False)
        self.db.add(pm)
        await self.db.flush()
        return pm

    async def update(self, pm: PaymentMethod, name: str) -> PaymentMethod:
        pm.name = name
        self.db.add(pm)
        await self.db.flush()
        return pm

    async def delete_custom_payment_method(self, id: uuid.UUID) -> None:
        # 1. Update referencing expenses to OTHER_PAYMENT_METHOD_ID
        select_stmt = select(Expense).where(Expense.payment_method_id == id)
        expenses_result = await self.db.execute(select_stmt)
        expenses = expenses_result.scalars().all()
        for expense in expenses:
            expense.payment_method_id = OTHER_PAYMENT_METHOD_ID
            self.db.add(expense)

        await self.db.flush()

        # 2. Delete the payment method
        pm = await self.get_by_id(id)
        if pm:
            await self.db.delete(pm)
            await self.db.flush()
