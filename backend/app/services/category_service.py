import uuid
from typing import List

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, NotFoundError
from app.db.models.category import Category
from app.repositories.category_repository import CategoryRepository


class CategoryService:
    def __init__(self, db: AsyncSession):
        self.repo = CategoryRepository(db)

    async def get_category(self, id: uuid.UUID) -> Category:
        category = await self.repo.get_by_id(id)
        if not category:
            raise NotFoundError("Category not found")
        return category

    async def list_categories(self) -> List[Category]:
        return await self.repo.list_all()

    async def create_category(self, name: str) -> Category:
        # Check uniqueness (case-insensitive)
        existing = await self.repo.get_by_name(name)
        if existing:
            raise ConflictError(f"Category with name '{name}' already exists")
        
        return await self.repo.create(name)

    async def rename_category(self, id: uuid.UUID, name: str) -> Category:
        category = await self.get_category(id)
        
        # Check if the new name belongs to a different category
        existing = await self.repo.get_by_name(name)
        if existing and existing.id != id:
            raise ConflictError(f"Category with name '{name}' already exists")

        return await self.repo.update(category, name)

    async def delete_category(self, id: uuid.UUID) -> None:
        category = await self.get_category(id)
        
        # Block deleting starter categories
        if category.is_default:
            raise ConflictError("Starter categories cannot be deleted")

        await self.repo.delete_custom_category(id)
