import asyncio
import os
import sys
from datetime import date, timedelta
from decimal import Decimal
from typing import Optional
import uuid

# Insert parent directory to path to allow absolute imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from sqlalchemy import select
from app.db.session import async_session_maker
from app.db.models.user import User
from app.db.models.expense import Expense
from app.db.models.budget import Budget
from app.db.models.category import Category
from app.db.models.payment_method import PaymentMethod


async def seed_user_data(target_email: Optional[str] = None) -> None:
    print("Connecting to database to seed user data...")

    async with async_session_maker() as session:
        try:
            # 1. Fetch target user
            if target_email:
                stmt = select(User).where(User.email == target_email.lower().strip())
                res = await session.execute(stmt)
                user = res.scalar_one_or_none()
            else:
                # Pick the latest non-system registered user
                stmt = select(User).order_by(User.created_at.desc())
                res = await session.execute(stmt)
                users = list(res.scalars().all())
                if not users:
                    print("No users found in database. Please register/sign in first.")
                    return
                # If there are multiple users, prefer the most recently created or signed up
                user = users[0]

            if not user:
                print(f"User '{target_email}' not found.")
                return

            print(f"Seeding data for user: {user.display_name} ({user.email}) [ID: {user.id}]")

            # 2. Fetch starter categories and payment methods
            cat_stmt = select(Category).where(Category.is_default == True)  # noqa: E712
            cat_res = await session.execute(cat_stmt)
            categories = {c.name: c.id for c in cat_res.scalars().all()}

            pm_stmt = select(PaymentMethod).where(PaymentMethod.is_default == True)  # noqa: E712
            pm_res = await session.execute(pm_stmt)
            payment_methods = {pm.name: pm.id for pm in pm_res.scalars().all()}

            if not categories or not payment_methods:
                print("Missing default categories or payment methods in database.")
                return

            # Helper fallbacks
            default_cat_id = next(iter(categories.values()))
            default_pm_id = next(iter(payment_methods.values()))

            def get_cat(name: str) -> uuid.UUID:
                return categories.get(name, default_cat_id)

            def get_pm(name: str) -> uuid.UUID:
                return payment_methods.get(name, default_pm_id)

            today = date.today()
            year = today.year
            month = today.month

            # 3. Create or update budgets for the user
            month_key = today.strftime("%Y-%m")
            week_key = f"{year}-W{today.isocalendar()[1]:02d}"

            # Monthly budget: $2,500.00
            m_budget_stmt = select(Budget).where(
                Budget.user_id == user.id,
                Budget.period_type == "month",
                Budget.period_key == month_key,
            )
            m_budget_res = await session.execute(m_budget_stmt)
            m_budget = m_budget_res.scalar_one_or_none()
            if not m_budget:
                session.add(
                    Budget(
                        user_id=user.id,
                        period_type="month",
                        period_key=month_key,
                        month=month_key,
                        amount=Decimal("2500.00"),
                    )
                )

            # Weekly budget: $600.00
            w_budget_stmt = select(Budget).where(
                Budget.user_id == user.id,
                Budget.period_type == "week",
                Budget.period_key == week_key,
            )
            w_budget_res = await session.execute(w_budget_stmt)
            w_budget = w_budget_res.scalar_one_or_none()
            if not w_budget:
                session.add(
                    Budget(
                        user_id=user.id,
                        period_type="week",
                        period_key=week_key,
                        month=None,
                        amount=Decimal("600.00"),
                    )
                )

            # 4. Add diverse expense transactions
            sample_expenses = [
                {
                    "amount": Decimal("48.50"),
                    "category_id": get_cat("Food & Dining"),
                    "payment_method_id": get_pm("Credit Card"),
                    "date": today - timedelta(days=1),
                    "description": "Italian Bistro Dinner",
                },
                {
                    "amount": Decimal("18.00"),
                    "category_id": get_cat("Transportation"),
                    "payment_method_id": get_pm("Debit Card"),
                    "date": today - timedelta(days=2),
                    "description": "Metro Monthly Pass Refill",
                },
                {
                    "amount": Decimal("112.40"),
                    "category_id": get_cat("Groceries"),
                    "payment_method_id": get_pm("Credit Card"),
                    "date": today - timedelta(days=3),
                    "description": "Whole Foods Market",
                },
                {
                    "amount": Decimal("85.00"),
                    "category_id": get_cat("Bills & Utilities"),
                    "payment_method_id": get_pm("Bank Transfer"),
                    "date": today - timedelta(days=4),
                    "description": "Internet & WiFi Bill",
                },
                {
                    "amount": Decimal("59.99"),
                    "category_id": get_cat("Shopping"),
                    "payment_method_id": get_pm("Credit Card"),
                    "date": today - timedelta(days=5),
                    "description": "Wireless Earbuds",
                },
                {
                    "amount": Decimal("24.00"),
                    "category_id": get_cat("Entertainment"),
                    "payment_method_id": get_pm("Digital Wallet"),
                    "date": today - timedelta(days=6),
                    "description": "Cinema Tickets (2x)",
                },
                {
                    "amount": Decimal("35.00"),
                    "category_id": get_cat("Health"),
                    "payment_method_id": get_pm("Debit Card"),
                    "date": today - timedelta(days=7),
                    "description": "Pharmacy & Vitamins",
                },
                {
                    "amount": Decimal("14.50"),
                    "category_id": get_cat("Food & Dining"),
                    "payment_method_id": get_pm("Cash"),
                    "date": today - timedelta(days=8),
                    "description": "Coffee & Bakery Snacks",
                },
                {
                    "amount": Decimal("129.00"),
                    "category_id": get_cat("Education"),
                    "payment_method_id": get_pm("Credit Card"),
                    "date": today - timedelta(days=10),
                    "description": "Advanced Cloud Certification Course",
                },
                {
                    "amount": Decimal("42.00"),
                    "category_id": get_cat("Transportation"),
                    "payment_method_id": get_pm("Debit Card"),
                    "date": today - timedelta(days=12),
                    "description": "Gas Station Fuel",
                },
                {
                    "amount": Decimal("94.30"),
                    "category_id": get_cat("Groceries"),
                    "payment_method_id": get_pm("Credit Card"),
                    "date": today - timedelta(days=14),
                    "description": "Supermarket Weekly Restock",
                },
                {
                    "amount": Decimal("15.99"),
                    "category_id": get_cat("Entertainment"),
                    "payment_method_id": get_pm("Digital Wallet"),
                    "date": today - timedelta(days=15),
                    "description": "Streaming Service Subscription",
                },
            ]

            expense_count = 0
            for item in sample_expenses:
                # Avoid inserting exact duplicates if script re-run
                check_stmt = select(Expense).where(
                    Expense.user_id == user.id,
                    Expense.description == item["description"],
                    Expense.date == item["date"],
                )
                check_res = await session.execute(check_stmt)
                if not check_res.scalar_one_or_none():
                    expense = Expense(
                        user_id=user.id,
                        amount=item["amount"],
                        category_id=item["category_id"],
                        payment_method_id=item["payment_method_id"],
                        date=item["date"],
                        description=item["description"],
                    )
                    session.add(expense)
                    expense_count += 1

            await session.commit()
            print(f"Successfully seeded {expense_count} expenses and budgets for {user.email}.")
        except Exception as e:
            await session.rollback()
            print(f"Error seeding data: {e}")
            raise e


if __name__ == "__main__":
    email_arg = sys.argv[1] if len(sys.argv) > 1 else None
    asyncio.run(seed_user_data(email_arg))
