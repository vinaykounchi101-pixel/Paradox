import uuid
from typing import List, Optional

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.category import Category
from app.db.models.expense import Expense
from app.constants.categories import UNCATEGORIZED_ID


class CategoryRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, id: uuid.UUID) -> Optional[Category]:
        stmt = select(Category).where(Category.id == id)
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_name(self, name: str) -> Optional[Category]:
        # Case-insensitive unique name lookup
        stmt = select(Category).where(func.lower(Category.name) == name.lower())
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def list_all(self) -> List[Category]:
        stmt = select(Category).order_by(Category.is_default.desc(), Category.name.asc())
        result = await self.db.execute(stmt)
        return list(result.scalars().all())

    async def create(self, name: str) -> Category:
        category = Category(name=name, is_default=False)
        self.db.add(category)
        await self.db.flush()
        await self.db.refresh(category)
        return category

    async def update(self, category: Category, name: str) -> Category:
        category.name = name
        self.db.add(category)
        await self.db.flush()
        await self.db.refresh(category)
        return category

    async def delete_category(self, id: uuid.UUID) -> None:
        # Atomic transaction: reassign expenses to another existing category, then delete
        stmt = select(Category).where(Category.id != id).limit(1)
        res = await self.db.execute(stmt)
        fallback = res.scalar_one_or_none()
        if not fallback:
            from app.core.exceptions import ConflictError
            raise ConflictError("Cannot delete the only remaining category. Please create another category first.")

        # 1. Update referencing expenses to fallback category
        update_stmt = (
            select(Expense)
            .where(Expense.category_id == id)
        )
        expenses_result = await self.db.execute(update_stmt)
        expenses = expenses_result.scalars().all()
        for expense in expenses:
            expense.category_id = fallback.id
            self.db.add(expense)
        
        # Flush the updates
        await self.db.flush()

        # 2. Retrieve and delete the category
        category = await self.get_by_id(id)
        if category:
            await self.db.delete(category)
            await self.db.flush()
