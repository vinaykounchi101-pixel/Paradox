import asyncio
import sys
import os
from decimal import Decimal
from datetime import date

# Insert parent directory to path to allow absolute imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from app.db.session import async_session_maker
from app.db.models.expense import Expense
from app.db.models.budget import Budget
from app.db.models.category import Category
from app.db.models.payment_method import PaymentMethod
from app.constants.categories import STARTER_CATEGORIES
from app.constants.payment_methods import STARTER_PAYMENT_METHODS


async def seed_dummy_data() -> None:
    print("Starting database seeding of dummy data...")
    today = date.today()
    year = today.year
    month = today.month

    dummy_expenses = [
        {
            "amount": Decimal("45.50"),
            "category_id": STARTER_CATEGORIES["Food & Dining"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Credit Card"],
            "date": date(year, month, 2) if today.day >= 2 else today,
            "description": "Dinner with friends",
        },
        {
            "amount": Decimal("15.00"),
            "category_id": STARTER_CATEGORIES["Transportation"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Debit Card"],
            "date": date(year, month, 5) if today.day >= 5 else today,
            "description": "Uber ride",
        },
        {
            "amount": Decimal("85.20"),
            "category_id": STARTER_CATEGORIES["Groceries"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Debit Card"],
            "date": date(year, month, 8) if today.day >= 8 else today,
            "description": "Weekly groceries",
        },
        {
            "amount": Decimal("120.00"),
            "category_id": STARTER_CATEGORIES["Bills & Utilities"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Bank Transfer"],
            "date": date(year, month, 10) if today.day >= 10 else today,
            "description": "Electricity bill",
        },
        {
            "amount": Decimal("65.00"),
            "category_id": STARTER_CATEGORIES["Shopping"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Credit Card"],
            "date": date(year, month, 12) if today.day >= 12 else today,
            "description": "New clothes",
        },
        {
            "amount": Decimal("30.00"),
            "category_id": STARTER_CATEGORIES["Entertainment"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Digital Wallet"],
            "date": date(year, month, 15) if today.day >= 15 else today,
            "description": "Movie tickets",
        },
        {
            "amount": Decimal("25.00"),
            "category_id": STARTER_CATEGORIES["Health"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Cash"],
            "date": date(year, month, 18) if today.day >= 18 else today,
            "description": "Medicines",
        },
        {
            "amount": Decimal("110.00"),
            "category_id": STARTER_CATEGORIES["Education"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Bank Transfer"],
            "date": date(year, month, 20) if today.day >= 20 else today,
            "description": "Online course",
        },
        {
            "amount": Decimal("18.50"),
            "category_id": STARTER_CATEGORIES["Food & Dining"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Cash"],
            "date": date(year, month, 22) if today.day >= 22 else today,
            "description": "Lunch combo",
        },
        {
            "amount": Decimal("95.00"),
            "category_id": STARTER_CATEGORIES["Shopping"],
            "payment_method_id": STARTER_PAYMENT_METHODS["Credit Card"],
            "date": date(year, month, 24) if today.day >= 24 else today,
            "description": "Running shoes",
        },
    ]

    async with async_session_maker() as session:
        try:
            # 1. Add dummy expenses
            expense_count = 0
            for item in dummy_expenses:
                # Basic check to avoid duplicates if re-run on same day/description
                check_stmt = (
                    Expense.__table__.select()
                    .where(Expense.description == item["description"])
                    .where(Expense.date == item["date"])
                )
                check_result = await session.execute(check_stmt)
                if not check_result.first():
                    expense = Expense(
                        amount=item["amount"],
                        category_id=item["category_id"],
                        payment_method_id=item["payment_method_id"],
                        date=item["date"],
                        description=item["description"],
                    )
                    session.add(expense)
                    expense_count += 1

            await session.commit()
            print(f"Successfully seeded {expense_count} dummy expenses.")
        except Exception as e:
            await session.rollback()
            print(f"Error seeding database: {e}")
            raise e


if __name__ == "__main__":
    asyncio.run(seed_dummy_data())
