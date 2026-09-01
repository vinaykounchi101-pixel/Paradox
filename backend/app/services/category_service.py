import uuid
from typing import List

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, NotFoundError
from app.db.models.category import Category
from app.repositories.category_repository import CategoryRepository


class CategoryService:
    def __init__(self, db: AsyncSession):
        self.repo = CategoryRepository(db)

    async def get_category(self, id: uuid.UUID, user_id: uuid.UUID) -> Category:
        category = await self.repo.get_by_id(id, user_id=user_id)
        if not category:
            raise NotFoundError("Category not found")
        return category

    async def list_categories(self, user_id: uuid.UUID) -> List[Category]:
        return await self.repo.list_all(user_id=user_id)

    async def create_category(self, name: str, user_id: uuid.UUID) -> Category:
        # Check uniqueness (case-insensitive) among categories visible to this user
        existing = await self.repo.get_by_name(name, user_id=user_id)
        if existing:
            raise ConflictError(f"Category with name '{name}' already exists")

        return await self.repo.create(name, user_id=user_id)

    async def rename_category(self, id: uuid.UUID, name: str, user_id: uuid.UUID) -> Category:
        category = await self.get_category(id, user_id=user_id)

        # Starter categories can be renamed; custom categories can only be renamed by their owner
        if not category.is_default and category.user_id != user_id:
            raise NotFoundError("Category not found")

        # Check if new name conflicts with an existing category visible to this user
        existing = await self.repo.get_by_name(name, user_id=user_id)
        if existing and existing.id != id:
            raise ConflictError(f"Category with name '{name}' already exists")

        return await self.repo.update(category, name)

    async def delete_category(self, id: uuid.UUID, user_id: uuid.UUID) -> None:
        category = await self.get_category(id, user_id=user_id)
        if category.is_default:
            raise ConflictError("Default starter categories cannot be deleted.")
        if category.user_id != user_id:
            raise NotFoundError("Category not found")

        await self.repo.delete_category(id, user_id=user_id)
