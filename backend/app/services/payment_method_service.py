import uuid
from typing import List

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, NotFoundError
from app.db.models.payment_method import PaymentMethod
from app.repositories.payment_method_repository import PaymentMethodRepository


class PaymentMethodService:
    def __init__(self, db: AsyncSession):
        self.repo = PaymentMethodRepository(db)

    async def get_payment_method(self, id: uuid.UUID) -> PaymentMethod:
        pm = await self.repo.get_by_id(id)
        if not pm:
            raise NotFoundError("Payment method not found")
        return pm

    async def list_payment_methods(self) -> List[PaymentMethod]:
        return await self.repo.list_all()

    async def create_payment_method(self, name: str) -> PaymentMethod:
        existing = await self.repo.get_by_name(name)
        if existing:
            raise ConflictError(f"Payment method with name '{name}' already exists")

        return await self.repo.create(name)

    async def rename_payment_method(self, id: uuid.UUID, name: str) -> PaymentMethod:
        pm = await self.get_payment_method(id)

        existing = await self.repo.get_by_name(name)
        if existing and existing.id != id:
            raise ConflictError(f"Payment method with name '{name}' already exists")

        return await self.repo.update(pm, name)

    async def delete_payment_method(self, id: uuid.UUID) -> None:
        await self.get_payment_method(id)
        await self.repo.delete_payment_method(id)
