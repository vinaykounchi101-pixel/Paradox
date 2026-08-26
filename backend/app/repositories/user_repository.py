import uuid
from typing import Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.user import User


class UserRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_primary_user(self) -> Optional[User]:
        primary_id = uuid.UUID("00000000-0000-0000-0000-000000000001")
        stmt = select(User).where(User.id == primary_id)
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()
