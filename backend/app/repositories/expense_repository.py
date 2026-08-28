import uuid
from datetime import date
from typing import List, Optional, Tuple

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.db.models.category import Category
from app.db.models.expense import Expense


class ExpenseRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, id: uuid.UUID) -> Optional[Expense]:
        stmt = (
            select(Expense)
            .where(Expense.id == id)
            .options(
                selectinload(Expense.category),
                selectinload(Expense.payment_method)
            )
            .execution_options(populate_existing=True)
        )
        result = await self.db.execute(stmt)
        return result.scalar_one_or_none()

    async def create(self, expense: Expense) -> Expense:
        self.db.add(expense)
        await self.db.flush()
        await self.db.refresh(expense)
        return await self.get_by_id(expense.id)

    async def update(self, expense: Expense) -> Expense:
        self.db.add(expense)
        await self.db.flush()
        await self.db.refresh(expense)
        return await self.get_by_id(expense.id)

    async def delete(self, expense: Expense) -> None:
        await self.db.delete(expense)
        await self.db.flush()

    async def list_expenses(
        self,
        search: Optional[str] = None,
        category_id: Optional[uuid.UUID] = None,
        date_from: Optional[date] = None,
        date_to: Optional[date] = None,
        sort_by: str = "date",
        sort_order: str = "desc",
        page: int = 1,
        page_size: int = 20,
    ) -> Tuple[List[Expense], int]:
        # Build base query
        stmt = (
            select(Expense)
            .options(
                selectinload(Expense.category),
                selectinload(Expense.payment_method)
            )
        )
        count_stmt = select(func.count()).select_from(Expense)

        # 1. Apply free-text search on description
        if search:
            search_filter = Expense.description.ilike(f"%{search}%")
            stmt = stmt.where(search_filter)
            count_stmt = count_stmt.where(search_filter)

        # 2. Apply single-dimension filter (date range OR category_id)
        if category_id:
            stmt = stmt.where(Expense.category_id == category_id)
            count_stmt = count_stmt.where(Expense.category_id == category_id)
        elif date_from or date_to:
            if date_from:
                stmt = stmt.where(Expense.date >= date_from)
                count_stmt = count_stmt.where(Expense.date >= date_from)
            if date_to:
                stmt = stmt.where(Expense.date <= date_to)
                count_stmt = count_stmt.where(Expense.date <= date_to)

        # 3. Get total count
        count_result = await self.db.execute(count_stmt)
        total_items = count_result.scalar_one()

        # 4. Apply sorting
        if sort_by == "amount":
            order_col = Expense.amount
        elif sort_by == "category":
            stmt = stmt.join(Expense.category)
            order_col = Category.name
        else:  # default sorting is by date
            order_col = Expense.date

        if sort_order == "asc":
            stmt = stmt.order_by(order_col.asc(), Expense.id.asc())
        else:
            stmt = stmt.order_by(order_col.desc(), Expense.id.desc())

        # 5. Apply pagination limit and offset
        offset = (page - 1) * page_size
        stmt = stmt.offset(offset).limit(page_size)

        # Execute query
        result = await self.db.execute(stmt)
        expenses = list(result.scalars().all())

        return expenses, total_items

    async def get_expenses_for_period(
        self,
        date_from: date,
        date_to: date,
    ) -> List[Expense]:
        stmt = (
            select(Expense)
            .where(Expense.date >= date_from, Expense.date <= date_to)
            .options(selectinload(Expense.category))
        )
        result = await self.db.execute(stmt)
        return list(result.scalars().all())
