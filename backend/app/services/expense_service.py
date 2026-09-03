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
            is_recurring=data.is_recurring,
            recurring_frequency=data.recurring_frequency,
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

        if data.is_recurring is not None:
            expense.is_recurring = data.is_recurring

        if data.recurring_frequency is not None:
            expense.recurring_frequency = data.recurring_frequency

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

    async def get_recurring_commitments(self, user_id: uuid.UUID) -> dict:
        recurring = await self.repo.get_recurring_expenses(user_id)
        total_monthly = round_monetary(0)
        from decimal import Decimal
        for e in recurring:
            freq = (e.recurring_frequency or "monthly").lower()
            amt = Decimal(str(e.amount))
            if freq == "weekly":
                total_monthly += amt * Decimal("4.33")
            elif freq == "yearly":
                total_monthly += amt / Decimal("12.00")
            else:  # monthly default
                total_monthly += amt

        return {
            "total_monthly_commitment": str(round_monetary(total_monthly)),
            "total_count": len(recurring),
            "recurring_expenses": recurring,
        }

    async def export_expenses_csv(
        self, user_id: uuid.UUID, date_from: Optional[date] = None, date_to: Optional[date] = None
    ) -> str:
        import csv
        import io

        expenses = await self.repo.get_all_expenses_for_export(user_id, date_from, date_to)
        output = io.StringIO()
        writer = csv.writer(output)
        writer.writerow(["Date", "Category", "Amount", "Payment Method", "Description", "Is Recurring", "Frequency"])

        for e in expenses:
            cat_name = e.category.name if e.category else "Uncategorized"
            pm_name = e.payment_method.name if e.payment_method else "Cash"
            writer.writerow([
                e.date.isoformat(),
                cat_name,
                f"{e.amount:.2f}",
                pm_name,
                e.description or "",
                "Yes" if e.is_recurring else "No",
                e.recurring_frequency or "",
            ])

        return output.getvalue()

    async def import_expenses_csv(self, user_id: uuid.UUID, csv_content: str) -> dict:
        import csv
        import io
        import re
        from decimal import Decimal
        from app.services.ai_service import ai_service

        categories = await self.category_repo.list_all(user_id=user_id)
        cat_map = {c.name.lower(): c.id for c in categories}
        cat_names = [c.name for c in categories]
        default_cat = next((c for c in categories if c.is_default), categories[0] if categories else None)

        pms = await self.pm_repo.list_all(user_id=user_id)
        default_pm = next((p for p in pms if p.is_default), pms[0] if pms else None)
        if not default_cat or not default_pm:
            raise UnprocessableRequestError("Cannot import expenses without active categories and payment methods")

        reader = csv.reader(io.StringIO(csv_content.strip()))
        rows = list(reader)
        if not rows:
            return {"imported": 0, "skipped": 0, "message": "Empty file"}

        # Detect header row
        header = [c.lower().strip() for c in rows[0]]
        date_idx, desc_idx, amount_idx, cat_idx = -1, -1, -1, -1

        for i, col in enumerate(header):
            if any(k in col for k in ["date", "time", "txndate"]):
                date_idx = i
            elif any(k in col for k in ["desc", "narration", "particular", "merchant", "details", "remark"]):
                desc_idx = i
            elif any(k in col for k in ["amount", "debit", "withdrawal", "spent", "val"]):
                amount_idx = i
            elif any(k in col for k in ["category", "cat"]):
                cat_idx = i

        # Fallback if standard headers not found
        start_row = 1 if (date_idx != -1 or amount_idx != -1) else 0
        if date_idx == -1: date_idx = 0
        if amount_idx == -1: amount_idx = 1
        if desc_idx == -1: desc_idx = 2 if len(rows[0]) > 2 else 0

        imported = 0
        skipped = 0

        for row in rows[start_row:]:
            if not row or len(row) <= max(date_idx, amount_idx):
                skipped += 1
                continue
            try:
                # 1. Parse amount
                raw_amt = row[amount_idx].replace(",", "").replace("$", "").replace("₹", "").strip()
                amt_match = re.search(r"\d+(?:\.\d{1,2})?", raw_amt)
                if not amt_match:
                    skipped += 1
                    continue
                amount = Decimal(amt_match.group(0))
                if amount <= 0:
                    skipped += 1
                    continue

                # 2. Parse date
                raw_date = row[date_idx].strip()
                parsed_date = get_current_date()
                d_match = re.search(r"\b(\d{4})[-/](\d{1,2})[-/](\d{1,2})\b", raw_date)
                if d_match:
                    y, m, d = d_match.groups()
                    parsed_date = date(int(y), int(m), int(d))
                else:
                    d_match_rev = re.search(r"\b(\d{1,2})[-/](\d{1,2})[-/](\d{4})\b", raw_date)
                    if d_match_rev:
                        d, m, y = d_match_rev.groups()
                        parsed_date = date(int(y), int(m), int(d))

                if parsed_date > get_current_date():
                    parsed_date = get_current_date()

                # 3. Parse description
                desc = row[desc_idx].strip() if desc_idx < len(row) else "Bank Transaction"

                # 4. Determine category (explicit column or AI heuristic)
                cat_id = default_cat.id
                if cat_idx != -1 and cat_idx < len(row) and row[cat_idx].strip():
                    raw_cat = row[cat_idx].strip().lower()
                    if raw_cat in cat_map:
                        cat_id = cat_map[raw_cat]
                
                if cat_id == default_cat.id and desc:
                    cat_res = await ai_service.categorize_expense(desc, cat_names)
                    matched_name = cat_res.category_name.lower()
                    if matched_name in cat_map:
                        cat_id = cat_map[matched_name]

                # Create expense
                new_exp = Expense(
                    user_id=user_id,
                    amount=round_monetary(amount),
                    category_id=cat_id,
                    payment_method_id=default_pm.id,
                    date=parsed_date,
                    description=desc[:255],
                )
                self.db.add(new_exp)
                imported += 1
            except Exception:
                skipped += 1
                continue

        if imported > 0:
            await self.db.flush()

        return {
            "imported": imported,
            "skipped": skipped,
            "message": f"Successfully imported {imported} transaction(s). {skipped} row(s) skipped.",
        }
