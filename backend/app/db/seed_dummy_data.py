import asyncio
import sys
import os
from decimal import Decimal
from datetime import date

# Insert parent directory to path to allow absolute imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from sqlalchemy import select
from app.db.session import async_session_maker
from app.db.models.user import User
from app.db.models.expense import Expense
from app.db.models.category import Category
from app.db.models.payment_method import PaymentMethod
from app.constants.categories import STARTER_CATEGORIES
from app.constants.payment_methods import STARTER_PAYMENT_METHODS


async def seed_dummy_data() -> None:
    """Idempotent startup seeder for starter categories, payment methods, and default user."""
    today = date.today()
    year = today.year
    month = today.month

    async with async_session_maker() as session:
        try:
            # 1. Fetch or create primary default user
            user_stmt = select(User).order_by(User.created_at.asc()).limit(1)
            user_res = await session.execute(user_stmt)
            user = user_res.scalar_one_or_none()

            if not user:
                user = User(
                    email="primary@paradox.local",
                    display_name="Primary User",
                    is_verified=True,
                )
                session.add(user)
                await session.flush()
                await session.refresh(user)

            # 2. Seed starter categories if not present
            for name, cat_id in STARTER_CATEGORIES.items():
                stmt = select(Category).where(Category.id == cat_id)
                res = await session.execute(stmt)
                if not res.scalar_one_or_none():
                    session.add(Category(id=cat_id, name=name, is_default=True, user_id=None))

            # 3. Seed starter payment methods if not present
            for name, pm_id in STARTER_PAYMENT_METHODS.items():
                stmt = select(PaymentMethod).where(PaymentMethod.id == pm_id)
                res = await session.execute(stmt)
                if not res.scalar_one_or_none():
                    session.add(PaymentMethod(id=pm_id, name=name, is_default=True, user_id=None))

            await session.commit()
        except Exception as e:
            await session.rollback()
            # Do not crash the entire app if starter records already exist
            print(f"Startup seed notice: {e}")


if __name__ == "__main__":
    asyncio.run(seed_dummy_data())
