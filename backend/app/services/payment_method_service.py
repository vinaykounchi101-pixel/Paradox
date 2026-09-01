import uuid
from typing import List

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, NotFoundError
from app.db.models.payment_method import PaymentMethod
from app.repositories.payment_method_repository import PaymentMethodRepository


class PaymentMethodService:
    def __init__(self, db: AsyncSession):
        self.repo = PaymentMethodRepository(db)

    async def get_payment_method(self, id: uuid.UUID, user_id: uuid.UUID) -> PaymentMethod:
        pm = await self.repo.get_by_id(id, user_id=user_id)
        if not pm:
            raise NotFoundError("Payment method not found")
        return pm

    async def list_payment_methods(self, user_id: uuid.UUID) -> List[PaymentMethod]:
        return await self.repo.list_all(user_id=user_id)

    async def create_payment_method(self, name: str, user_id: uuid.UUID) -> PaymentMethod:
        existing = await self.repo.get_by_name(name, user_id=user_id)
        if existing:
            raise ConflictError(f"Payment method with name '{name}' already exists")

        return await self.repo.create(name, user_id=user_id)

    async def rename_payment_method(self, id: uuid.UUID, name: str, user_id: uuid.UUID) -> PaymentMethod:
        pm = await self.get_payment_method(id, user_id=user_id)
        if not pm.is_default and pm.user_id != user_id:
            raise NotFoundError("Payment method not found")

        existing = await self.repo.get_by_name(name, user_id=user_id)
        if existing and existing.id != id:
            raise ConflictError(f"Payment method with name '{name}' already exists")

        return await self.repo.update(pm, name)

    async def delete_payment_method(self, id: uuid.UUID, user_id: uuid.UUID) -> None:
        pm = await self.get_payment_method(id, user_id=user_id)
        if pm.is_default:
            raise ConflictError("Default starter payment methods cannot be deleted.")
        if pm.user_id != user_id:
            raise NotFoundError("Payment method not found")

        await self.repo.delete_payment_method(id, user_id=user_id)
