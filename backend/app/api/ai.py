import calendar
from datetime import date, timedelta
from decimal import Decimal
from typing import Optional

from fastapi import APIRouter, Depends, File, Query, UploadFile, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.repositories.expense_repository import ExpenseRepository
from app.schemas.ai import (
    AIInsightsResponse,
    CategorizeRequest,
    CategorizeResponse,
    ParseExpenseRequest,
    ParseExpenseResponse,
    ParseReceiptRequest,
    ParseReceiptResponse,
    SuggestBudgetResponse,
)
from app.schemas.common import DataEnvelope
from app.services.ai_service import ai_service
from app.services.category_service import CategoryService
from app.services.dashboard_service import DashboardService
from app.services.payment_method_service import PaymentMethodService
from app.utils.datetime import get_current_date

router = APIRouter(prefix="/ai", tags=["AI & Recommendations"])


@router.post("/categorize", response_model=DataEnvelope[CategorizeResponse], status_code=status.HTTP_200_OK)
async def categorize_expense(
    payload: CategorizeRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Suggest the most relevant expense category based on the description or merchant.
    Utilizes Google Gemini, OpenAI, Claude, or intelligent heuristic fallback.
    """
    categories = payload.available_categories
    if not categories:
        cat_service = CategoryService(db)
        user_cats = await cat_service.list_categories(current_user.id)
        categories = [c.name for c in user_cats]

    result = await ai_service.categorize_expense(
        description=payload.description,
        available_categories=categories,
    )
    return {"data": result}


@router.post("/parse-expense", response_model=DataEnvelope[ParseExpenseResponse], status_code=status.HTTP_200_OK)
async def parse_expense(
    payload: ParseExpenseRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Parse a freeform natural language expense string (e.g. 'Paid 350 for lunch via UPI')
    into structured expense fields (amount, category, payment method, date, description).
    """
    categories = payload.available_categories
    if not categories:
        cat_service = CategoryService(db)
        user_cats = await cat_service.list_categories(current_user.id)
        categories = [c.name for c in user_cats]

    payment_methods = payload.available_payment_methods
    if not payment_methods:
        pm_service = PaymentMethodService(db)
        user_pms = await pm_service.list_payment_methods(current_user.id)
        payment_methods = [p.name for p in user_pms]

    result = await ai_service.parse_expense_text(
        text=payload.text,
        available_categories=categories,
        available_payment_methods=payment_methods,
    )
    return {"data": result}


@router.get("/insights", response_model=DataEnvelope[AIInsightsResponse], status_code=status.HTTP_200_OK)
async def get_ai_insights(
    period: str = Query("current_month", pattern="^(current_month|last_30_days|current_week)$"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Generate AI Financial Copilot insights, burn rate projections, and savings tips.
    Scattered across Gemini, OpenAI, Claude, or local statistical heuristic.
    """
    dashboard_service = DashboardService(db)
    dash_data = await dashboard_service.get_dashboard_data(user_id=current_user.id, period=period)

    today = get_current_date()
    if period == "current_week":
        days_elapsed = today.weekday() + 1
        total_days = 7
    elif period == "last_30_days":
        days_elapsed = 30
        total_days = 30
    else:  # current_month
        days_elapsed = today.day
        total_days = calendar.monthrange(today.year, today.month)[1]

    budget_limit = Decimal(dash_data.budget.amount) if dash_data.budget.amount else None
    cat_breakdown = [
        {"category_name": c.category_name, "total": str(c.total), "percentage": c.percentage}
        for c in dash_data.category_breakdown
    ]

    insights = await ai_service.generate_insights(
        period=period,
        total_spent=Decimal(str(dash_data.total_spent)),
        budget_limit=budget_limit,
        category_breakdown=cat_breakdown,
        days_elapsed=days_elapsed,
        total_days=total_days,
    )
    return {"data": insights}


@router.get("/suggest-budget", response_model=DataEnvelope[SuggestBudgetResponse], status_code=status.HTTP_200_OK)
async def suggest_budget(
    period_type: str = Query("month", pattern="^(month|week|day)$"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Predictively recommend an optimal budget target for month, week, or day
    based on past 90 days of user spending patterns and standard deviation.
    """
    today = get_current_date()
    date_from = today - timedelta(days=90)
    expense_repo = ExpenseRepository(db)
    past_expenses = await expense_repo.get_expenses_for_period(current_user.id, date_from, today)

    suggestion = await ai_service.suggest_budget(
        period_type=period_type,
        past_expenses=past_expenses,
    )
    return {"data": suggestion}


@router.post("/parse-receipt", response_model=DataEnvelope[ParseReceiptResponse], status_code=status.HTTP_200_OK)
async def parse_receipt(
    payload: ParseReceiptRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Parse a multi-line raw bill, receipt text, or SMS transaction alert into
    structured items, merchant, and total amount.
    """
    cat_service = CategoryService(db)
    user_cats = await cat_service.list_categories(current_user.id)
    categories = [c.name for c in user_cats]

    pm_service = PaymentMethodService(db)
    user_pms = await pm_service.list_payment_methods(current_user.id)
    payment_methods = [p.name for p in user_pms]

    result = await ai_service.parse_receipt_text(
        text=payload.text,
        categories=categories,
        payment_methods=payment_methods,
    )
    return {"data": result}


@router.post("/scan-receipt", response_model=DataEnvelope[ParseExpenseResponse], status_code=status.HTTP_200_OK)
async def scan_receipt(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Scan a photo, invoice, or receipt image using Gemini Vision OCR
    to automatically extract amount, date, description, category, and payment method.
    """
    cat_service = CategoryService(db)
    user_cats = await cat_service.list_categories(current_user.id)
    categories = [c.name for c in user_cats]

    pm_service = PaymentMethodService(db)
    user_pms = await pm_service.list_payment_methods(current_user.id)
    payment_methods = [p.name for p in user_pms]

    content_bytes = await file.read()
    result = await ai_service.scan_receipt_image(
        image_bytes=content_bytes,
        mime_type=file.content_type or "image/jpeg",
        categories=categories,
        payment_methods=payment_methods,
    )
    return {"data": result}

