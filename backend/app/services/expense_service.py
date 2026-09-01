import uuid
from datetime import date
from typing import List, Optional, Tuple

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import NotFoundError, UnprocessableRequestError, ValidationError
from app.db.models.expense import Expense
from app.repositories.category_repository import CategoryRepository
from app.repositories.expense_repository import ExpenseRepository
from app.repositories.payment_method_repository import PaymentMethodRepository
from app.schemas.expense import ExpenseCreate, ExpenseUpdate
from app.utils.datetime import get_current_date
from app.utils.money import round_monetary


class ExpenseService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.repo = ExpenseRepository(db)
        self.category_repo = CategoryRepository(db)
        self.pm_repo = PaymentMethodRepository(db)

    async def _validate_references(
        self, category_id: uuid.UUID, payment_method_id: uuid.UUID, user_id: uuid.UUID
    ) -> None:
        category = await self.category_repo.get_by_id(category_id, user_id=user_id)
        if not category:
            raise NotFoundError("Category not found")

        pm = await self.pm_repo.get_by_id(payment_method_id, user_id=user_id)
        if not pm:
            raise NotFoundError("Payment method not found")

    async def get_expense(self, id: uuid.UUID, user_id: uuid.UUID) -> Expense:
        expense = await self.repo.get_by_id(id, user_id)
        if not expense:
            raise NotFoundError("Expense not found")
        return expense

    async def create_expense(self, user_id: uuid.UUID, data: ExpenseCreate) -> Expense:
        # Enforce positive amount
        if data.amount <= 0:
            raise ValidationError("amount must be greater than 0")

        # Enforce non-future date
        current = get_current_date()
        if data.date > current:
            raise ValidationError("expense date cannot be in the future")

        # Validate existence of foreign keys belonging to or visible to this user
        await self._validate_references(data.category_id, data.payment_method_id, user_id)

        expense = Expense(
            user_id=user_id,
            amount=round_monetary(data.amount),
            category_id=data.category_id,
            payment_method_id=data.payment_method_id,
            date=data.date,
            description=data.description,
        )
        return await self.repo.create(expense)

    async def update_expense(self, id: uuid.UUID, user_id: uuid.UUID, data: ExpenseUpdate) -> Expense:
        expense = await self.get_expense(id, user_id)

        # Enforce positive amount if supplied
        if data.amount is not None:
            if data.amount <= 0:
                raise ValidationError("amount must be greater than 0")
            expense.amount = round_monetary(data.amount)

        # Enforce non-future date if supplied
        if data.date is not None:
            current = get_current_date()
            if data.date > current:
                raise ValidationError("expense date cannot be in the future")
            expense.date = data.date

        # Check references if category or payment method is updated
        category_id = data.category_id if data.category_id is not None else expense.category_id
        payment_method_id = data.payment_method_id if data.payment_method_id is not None else expense.payment_method_id
        if data.category_id is not None or data.payment_method_id is not None:
            await self._validate_references(category_id, payment_method_id, user_id)
            expense.category_id = category_id
            expense.payment_method_id = payment_method_id

        if data.description is not None:
            expense.description = data.description

        return await self.repo.update(expense)

    async def delete_expense(self, id: uuid.UUID, user_id: uuid.UUID) -> None:
        expense = await self.get_expense(id, user_id)
        await self.repo.delete(expense)

    async def list_expenses(
        self,
        user_id: uuid.UUID,
        search: Optional[str] = None,
        category_id: Optional[uuid.UUID] = None,
        date_from: Optional[date] = None,
        date_to: Optional[date] = None,
        sort_by: str = "date",
        sort_order: str = "desc",
        page: int = 1,
        page_size: int = 20,
    ) -> Tuple[List[Expense], int]:
        # Enforce single-dimension filtering mutual exclusion rule (SRS Section 3.4.1)
        if category_id is not None and (date_from is not None or date_to is not None):
            raise UnprocessableRequestError(
                "only one filter — date range or category — may be applied at a time in V1"
            )

        # Validate date range order
        if date_from and date_to and date_from > date_to:
            raise UnprocessableRequestError("date_from must not be after date_to")

        return await self.repo.list_expenses(
            user_id=user_id,
            search=search,
            category_id=category_id,
            date_from=date_from,
            date_to=date_to,
            sort_by=sort_by,
            sort_order=sort_order,
            page=page,
            page_size=page_size,
        )
