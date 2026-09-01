import uuid
from typing import List, Optional

from sqlalchemy import func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError
from app.db.models.category import Category
from app.db.models.expense import Expense


class CategoryRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, id: uuid.UUID, user_id: Optional[uuid.UUID] = None) -> Optional[Category]:
        stmt = select(Category).where(Category.id == id)
        if user_id is not None:
            stmt = stmt.where(or_(Category.is_default == True, Category.user_id == user_id))  # noqa: E712
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_name(self, name: str, user_id: Optional[uuid.UUID] = None) -> Optional[Category]:
        stmt = select(Category).where(func.lower(Category.name) == name.lower().strip())
        if user_id is not None:
            stmt = stmt.where(or_(Category.is_default == True, Category.user_id == user_id))  # noqa: E712
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def list_all(self, user_id: Optional[uuid.UUID] = None) -> List[Category]:
        stmt = select(Category)
        if user_id is not None:
            stmt = stmt.where(or_(Category.is_default == True, Category.user_id == user_id))  # noqa: E712
        stmt = stmt.order_by(Category.is_default.desc(), Category.name.asc())
        result = await self.db.execute(stmt)
        return list(result.scalars().all())

    async def create(self, name: str, user_id: Optional[uuid.UUID] = None) -> Category:
        category = Category(name=name.strip(), is_default=False, user_id=user_id)
        self.db.add(category)
        await self.db.flush()
        await self.db.refresh(category)
        return category

    async def update(self, category: Category, name: str) -> Category:
        category.name = name.strip()
        self.db.add(category)
        await self.db.flush()
        await self.db.refresh(category)
        return category

    async def delete_category(self, id: uuid.UUID, user_id: uuid.UUID) -> None:
        # Atomic transaction: reassign user's referencing expenses to another existing visible category, then delete
        stmt = (
            select(Category)
            .where(
                Category.id != id,
                or_(Category.is_default == True, Category.user_id == user_id),  # noqa: E712
            )
            .limit(1)
        )
        res = await self.db.execute(stmt)
        fallback = res.scalar_one_or_none()
        if not fallback:
            raise ConflictError("Cannot delete the only remaining category. Please create another category first.")

        # 1. Update referencing expenses for this user to fallback category
        update_stmt = (
            select(Expense)
            .where(Expense.category_id == id, Expense.user_id == user_id)
        )
        expenses_result = await self.db.execute(update_stmt)
        expenses = expenses_result.scalars().all()
        for expense in expenses:
            expense.category_id = fallback.id
            self.db.add(expense)

        await self.db.flush()

        # 2. Retrieve and delete the category (only if owned by user and not default)
        category = await self.get_by_id(id, user_id=user_id)
        if category:
            if category.is_default:
                raise ConflictError("Default starter categories cannot be deleted.")
            if category.user_id != user_id:
                raise ConflictError("Cannot delete category belonging to another user.")
            await self.db.delete(category)
            await self.db.flush()
