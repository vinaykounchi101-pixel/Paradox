import uuid
from decimal import Decimal
from datetime import date, timedelta
from typing import Any, Dict, List, Optional, Tuple

from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.expense import Expense
from app.repositories.expense_repository import ExpenseRepository
from app.repositories.budget_repository import BudgetRepository
from app.repositories.category_repository import CategoryRepository
from app.utils.datetime import get_current_date
from app.utils.money import round_monetary
from app.schemas.dashboard import (
    DashboardBudget,
    DashboardCategoryBreakdown,
    DashboardRead,
    DashboardTopCategory,
    DashboardTrendItem,
)
from app.schemas.expense import ExpenseRead


class DashboardService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.expense_repo = ExpenseRepository(db)
        self.budget_repo = BudgetRepository(db)
        self.category_repo = CategoryRepository(db)

    async def get_dashboard_data(self, period: str = "current_month") -> DashboardRead:
        today = get_current_date()

        # 1. Resolve date range for the selected period
        if period == "current_week":
            date_from = today - timedelta(days=today.weekday())
            date_to = today
        elif period == "last_30_days":
            date_from = today - timedelta(days=29)
            date_to = today
        else:  # default is current_month
            date_from = date(today.year, today.month, 1)
            date_to = today

        # 2. Fetch expenses within the selected period
        expenses = await self.expense_repo.get_expenses_for_period(date_from, date_to)

        # Calculate total spent in the period
        total_spent = sum((e.amount for e in expenses), Decimal("0.00"))

        # 3. Calculate budget status for the *current calendar month* (strictly)
        month_start = date(today.year, today.month, 1)
        if period == "current_month":
            month_expenses = expenses
        else:
            month_expenses = await self.expense_repo.get_expenses_for_period(month_start, today)
        
        month_spent = sum((e.amount for e in month_expenses), Decimal("0.00"))

        budget = await self.budget_repo.get_budget()
        budget_data = self._calculate_budget_status(budget, month_spent)

        # 4. Calculate category breakdown & top categories
        category_breakdown, top_categories = self._calculate_category_stats(expenses, total_spent)

        # 5. Calculate trend buckets
        trend = self._calculate_trend(expenses, period, date_from, date_to)

        # 6. Fetch 5 most recent expenses (regardless of period date range)
        recent_expenses_list, _ = await self.expense_repo.list_expenses(
            page=1,
            page_size=5,
            sort_by="date",
            sort_order="desc",
        )
        recent_expenses = [ExpenseRead.model_validate(e) for e in recent_expenses_list]

        return DashboardRead(
            period=period,
            total_spent=round_monetary(total_spent),
            budget=budget_data,
            category_breakdown=category_breakdown,
            top_categories=top_categories,
            trend=trend,
            recent_expenses=recent_expenses,
        )

    def _calculate_budget_status(self, budget: Optional[Any] = None, month_spent: Decimal = Decimal("0.00")) -> DashboardBudget:
        # Avoid direct Any imports or typing errors
        if not budget:
            return DashboardBudget(
                amount=None,
                spent=round_monetary(month_spent),
                remaining=None,
                status=None,
            )

        amount = budget.amount
        remaining = amount - month_spent

        # Status derivation (SRS Section 10.2)
        if month_spent < Decimal("0.9") * amount:
            status = "under_budget"
        elif Decimal("0.9") * amount <= month_spent <= amount:
            status = "near_limit"
        else:
            status = "over_budget"

        return DashboardBudget(
            amount=round_monetary(amount),
            spent=round_monetary(month_spent),
            remaining=round_monetary(remaining),
            status=status,
        )

    def _calculate_category_stats(
        self, expenses: List[Expense], total_spent: Decimal
    ) -> Tuple[List[DashboardCategoryBreakdown], List[DashboardTopCategory]]:
        if not expenses:
            return [], []

        # Group by category
        cat_totals: Dict[Tuple[uuid.UUID, str], Decimal] = {}
        for e in expenses:
            key = (e.category.id, e.category.name)
            cat_totals[key] = cat_totals.get(key, Decimal("0.00")) + e.amount

        # Build raw breakdown lists
        breakdown = []
        for (cid, cname), total in cat_totals.items():
            percentage = float((total / total_spent) * 100) if total_spent > 0 else 0.0
            # Round percentage to 1 decimal place
            percentage = round(percentage, 1)
            breakdown.append(
                DashboardCategoryBreakdown(
                    category_id=cid,
                    category_name=cname,
                    total=round_monetary(total),
                    percentage=percentage,
                )
            )

        # Sort breakdown descending by total
        breakdown.sort(key=lambda x: x.total, reverse=True)

        # Build top categories (can just represent the sorted categories list)
        top = [
            DashboardTopCategory(
                category_id=b.category_id,
                category_name=b.category_name,
                total=b.total,
            )
            for b in breakdown
        ]

        return breakdown, top

    def _calculate_trend(
        self, expenses: List[Expense], period: str, date_from: date, date_to: date
    ) -> List[DashboardTrendItem]:
        # Helper to initialize trend items
        trend_map: Dict[str, Decimal] = {}

        if period == "current_week":
            # Group by day names (e.g. "Mon", "Tue", etc.)
            days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            for d in days:
                trend_map[d] = Decimal("0.00")
            
            for e in expenses:
                day_name = e.date.strftime("%A")
                if day_name in trend_map:
                    trend_map[day_name] += e.amount

            return [DashboardTrendItem(label=label, total=round_monetary(total)) for label, total in trend_map.items()]

        elif period == "current_month":
            # Group by week numbers within the calendar month (Week 1, Week 2, Week 3, Week 4, Week 5)
            weeks = ["Week 1", "Week 2", "Week 3", "Week 4", "Week 5"]
            for w in weeks:
                trend_map[w] = Decimal("0.00")

            for e in expenses:
                day = e.date.day
                if 1 <= day <= 7:
                    w = "Week 1"
                elif 8 <= day <= 14:
                    w = "Week 2"
                elif 15 <= day <= 21:
                    w = "Week 3"
                elif 22 <= day <= 28:
                    w = "Week 4"
                else:
                    w = "Week 5"
                trend_map[w] += e.amount

            return [DashboardTrendItem(label=label, total=round_monetary(total)) for label, total in trend_map.items()]

        else:  # last_30_days
            # Group by 4 blocks of 7/8 days in the 30 days window
            weeks = ["Week 1", "Week 2", "Week 3", "Week 4"]
            for w in weeks:
                trend_map[w] = Decimal("0.00")

            for e in expenses:
                days_diff = (e.date - date_from).days
                if 0 <= days_diff <= 7:
                    w = "Week 1"
                elif 8 <= days_diff <= 15:
                    w = "Week 2"
                elif 16 <= days_diff <= 23:
                    w = "Week 3"
                else:
                    w = "Week 4"
                trend_map[w] += e.amount

            return [DashboardTrendItem(label=label, total=round_monetary(total)) for label, total in trend_map.items()]
