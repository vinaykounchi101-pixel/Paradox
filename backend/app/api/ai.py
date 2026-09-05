import calendar
from datetime import date, timedelta
from decimal import Decimal
from typing import Optional

from fastapi import APIRouter, Depends, File, Query, UploadFile, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.repositories.budget_repository import BudgetRepository
from app.repositories.expense_repository import ExpenseRepository
from app.schemas.ai import (
    AchievementBadge,
    AchievementsResponse,
    AIChatRequest,
    AIChatResponse,
    AIInsightsResponse,
    AnalyzeSentimentRequest,
    AnalyzeSentimentResponse,
    AnomaliesResponse,
    CategorizeRequest,
    CategorizeResponse,
    CheckDuplicateRequest,
    CheckDuplicateResponse,
    FiftyThirtyTwentyResponse,
    FinancialHealthScoreResponse,
    LeakAnalysisResponse,
    MonthlyWrappedResponse,
    ParseExpenseRequest,
    ParseExpenseResponse,
    ParseReceiptRequest,
    ParseReceiptResponse,
    ParseSmsRequest,
    ParseSmsResponse,
    SafeToSpendResponse,
    SavingsPlanRequest,
    SavingsPlanResponse,
    SimulatePurchaseRequest,
    SimulatePurchaseResponse,
    SpendingForecastResponse,
    SubscriptionAuditResponse,
    SuggestBudgetResponse,
    VibeCheckResponse,
)
from app.schemas.common import DataEnvelope
from app.services.ai_service import ai_service
from app.services.category_service import CategoryService
from app.services.dashboard_service import DashboardService
from app.services.expense_service import ExpenseService
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


@router.post("/simulate-purchase", response_model=DataEnvelope[SimulatePurchaseResponse], status_code=status.HTTP_200_OK)
async def simulate_purchase(
    payload: SimulatePurchaseRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Simulate a purchase decision ('Can I Afford This?') against active monthly budgets,
    category allocations, and daily safe-to-spend burn velocity.
    """
    dashboard_service = DashboardService(db)
    dash_data = await dashboard_service.get_dashboard_data(user_id=current_user.id, period="current_month")

    today = get_current_date()
    days_elapsed = today.day
    total_days = calendar.monthrange(today.year, today.month)[1]

    budget_limit = Decimal(dash_data.budget.amount) if dash_data.budget.amount else None
    total_spent = Decimal(str(dash_data.total_spent))

    # Find category spent if category is supplied
    category_spent = Decimal("0.00")
    if payload.category_name:
        for c in dash_data.category_breakdown:
            if c.category_name.lower() == payload.category_name.lower():
                category_spent = Decimal(str(c.total))
                break

    result = await ai_service.simulate_purchase(
        amount=payload.amount,
        category_name=payload.category_name,
        description=payload.description,
        total_spent=total_spent,
        budget_limit=budget_limit,
        days_elapsed=days_elapsed,
        total_days=total_days,
        category_spent=category_spent,
    )
    return {"data": result}


@router.get("/safe-to-spend", response_model=DataEnvelope[SafeToSpendResponse], status_code=status.HTTP_200_OK)
async def get_safe_to_spend(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Calculate deterministic daily safe spending allowance and budget depletion date.
    """
    dashboard_service = DashboardService(db)
    dash_data = await dashboard_service.get_dashboard_data(user_id=current_user.id, period="current_month")

    today = get_current_date()
    days_elapsed = today.day
    total_days = calendar.monthrange(today.year, today.month)[1]

    budget_limit = Decimal(dash_data.budget.amount) if dash_data.budget.amount else None
    total_spent = Decimal(str(dash_data.total_spent))

    result = await ai_service.calculate_safe_to_spend(
        total_spent=total_spent,
        budget_limit=budget_limit,
        days_elapsed=days_elapsed,
        total_days=total_days,
    )
    return {"data": result}


@router.get("/health-score", response_model=DataEnvelope[FinancialHealthScoreResponse], status_code=status.HTTP_200_OK)
async def get_financial_health_score(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Get deterministic 0-100 Financial Health Score across Budget Adherence,
    Savings Velocity, and Category Discipline.
    """
    dashboard_service = DashboardService(db)
    dash_data = await dashboard_service.get_dashboard_data(user_id=current_user.id, period="current_month")

    today = get_current_date()
    days_elapsed = today.day
    total_days = calendar.monthrange(today.year, today.month)[1]

    budget_limit = Decimal(dash_data.budget.amount) if dash_data.budget.amount else None
    total_spent = Decimal(str(dash_data.total_spent))
    cat_breakdown = [
        {"category_name": c.category_name, "total": str(c.total), "percentage": c.percentage}
        for c in dash_data.category_breakdown
    ]

    result = await ai_service.calculate_health_score(
        total_spent=total_spent,
        budget_limit=budget_limit,
        days_elapsed=days_elapsed,
        total_days=total_days,
        category_breakdown=cat_breakdown,
    )
    return {"data": result}


@router.get("/leak-analysis", response_model=DataEnvelope[LeakAnalysisResponse], status_code=status.HTTP_200_OK)
async def get_leak_analysis(
    threshold: Decimal = Query(Decimal("150.00"), description="Maximum amount for micro-spending classification"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Analyze past 90 days for recurring micro-spending leaks and compute annualized impact.
    """
    today = get_current_date()
    date_from = today - timedelta(days=90)
    expense_repo = ExpenseRepository(db)
    past_expenses = await expense_repo.get_expenses_for_period(current_user.id, date_from, today)

    result = await ai_service.analyze_spending_leaks(
        past_expenses=past_expenses,
        threshold=threshold,
    )
    return {"data": result}


@router.get("/subscription-audit", response_model=DataEnvelope[SubscriptionAuditResponse], status_code=status.HTTP_200_OK)
async def get_subscription_audit(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Audit active recurring subscriptions and flag duplicate overlapping services.
    """
    today = get_current_date()
    date_from = today - timedelta(days=90)
    expense_repo = ExpenseRepository(db)
    past_expenses = await expense_repo.get_expenses_for_period(current_user.id, date_from, today)
    recurring_expenses = await expense_repo.get_recurring_expenses(current_user.id)

    result = await ai_service.audit_subscriptions(
        past_expenses=past_expenses,
        recurring_expenses=recurring_expenses,
    )
    return {"data": result}


@router.post("/parse-sms", response_model=DataEnvelope[ParseSmsResponse], status_code=status.HTTP_200_OK)
async def parse_sms(
    payload: ParseSmsRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Parse an Indian banking or UPI SMS alert into a structured transaction draft.
    """
    cat_service = CategoryService(db)
    user_cats = await cat_service.list_categories(current_user.id)
    categories = [c.name for c in user_cats]

    pm_service = PaymentMethodService(db)
    user_pms = await pm_service.list_payment_methods(current_user.id)
    payment_methods = [p.name for p in user_pms]

    result = await ai_service.parse_sms_text(
        text=payload.text,
        categories=categories,
        payment_methods=payment_methods,
    )
    return {"data": result}


@router.post("/check-duplicate", response_model=DataEnvelope[CheckDuplicateResponse], status_code=status.HTTP_200_OK)
async def check_duplicate(
    payload: CheckDuplicateRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Check if a candidate transaction matches an existing expense within a window of days.
    """
    expense_repo = ExpenseRepository(db)
    try:
        cand_date = date.fromisoformat(payload.date)
    except ValueError:
        cand_date = get_current_date()

    duplicates = await expense_repo.find_potential_duplicates(
        user_id=current_user.id,
        amount=payload.amount,
        target_date=cand_date,
        description=payload.description,
        window_days=payload.window_days,
    )

    if duplicates:
        match = duplicates[0]
        desc = match.description or "Expense"
        cat = match.category.name if match.category else "Uncategorized"
        msg = f"Potential duplicate found: A {match.amount} ({cat} - '{desc}') was logged on {match.date.strftime('%Y-%m-%d')}."
        return {
            "data": CheckDuplicateResponse(
                has_duplicate=True,
                duplicate_id=str(match.id),
                duplicate_amount=match.amount,
                duplicate_date=match.date.strftime("%Y-%m-%d"),
                duplicate_description=match.description,
                message=msg,
            )
        }

    return {
        "data": CheckDuplicateResponse(
            has_duplicate=False,
            message="No identical transaction detected in this date window.",
        )
    }


@router.get("/fifty-thirty-twenty", response_model=DataEnvelope[FiftyThirtyTwentyResponse], status_code=status.HTTP_200_OK)
async def get_fifty_thirty_twenty(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Compute 50/30/20 budget framework analysis (Needs, Wants, Savings) for current month.
    """
    today = get_current_date()
    month_start = date(today.year, today.month, 1)
    month_end = date(today.year, today.month, calendar.monthrange(today.year, today.month)[1])

    expense_repo = ExpenseRepository(db)
    current_expenses = await expense_repo.get_expenses_for_period(current_user.id, month_start, month_end)

    dashboard_service = DashboardService(db)
    dash_data = await dashboard_service.get_dashboard_data(user_id=current_user.id, period="current_month")

    budget_limit = Decimal(dash_data.budget.amount) if dash_data.budget.amount else None
    total_spent = Decimal(str(dash_data.total_spent))

    result = await ai_service.calculate_fifty_thirty_twenty(
        expenses=current_expenses,
        total_spent=total_spent,
        budget_limit=budget_limit,
    )
    return {"data": result}


@router.get("/achievements", response_model=DataEnvelope[AchievementsResponse], status_code=status.HTTP_200_OK)
async def get_achievements(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Retrieve gamified discipline streaks, milestone badges, and motivation quotes.
    """
    today = get_current_date()
    month_start = date(today.year, today.month, 1)
    month_end = date(today.year, today.month, calendar.monthrange(today.year, today.month)[1])

    expense_repo = ExpenseRepository(db)
    current_expenses = await expense_repo.get_expenses_for_period(current_user.id, month_start, month_end)
    past_90_expenses = await expense_repo.get_expenses_for_period(current_user.id, today - timedelta(days=90), today)

    budget_repo = BudgetRepository(db)
    budget = await budget_repo.get_budget(current_user.id, period_type="month")

    result = await ai_service.calculate_achievements(
        expenses=current_expenses,
        budget=budget,
        past_expenses=past_90_expenses,
    )
    return {"data": result}


@router.post("/chat", response_model=DataEnvelope[AIChatResponse], status_code=status.HTTP_200_OK)
async def chat_assistant(
    payload: AIChatRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Interactive conversational AI Financial Assistant grounded in live financial data.
    """
    dashboard_service = DashboardService(db)
    dash_data = await dashboard_service.get_dashboard_data(user_id=current_user.id, period="current_month")

    today = get_current_date()
    days_elapsed = today.day
    total_days = calendar.monthrange(today.year, today.month)[1]
    budget_limit = Decimal(dash_data.budget.amount) if dash_data.budget.amount else None
    total_spent = Decimal(str(dash_data.total_spent))

    safe_spend_res = await ai_service.calculate_safe_to_spend(
        total_spent=total_spent,
        budget_limit=budget_limit,
        days_elapsed=days_elapsed,
        total_days=total_days,
    )

    top_cats = [
        {"name": c.category_name, "amount": str(c.total), "percentage": c.percentage}
        for c in dash_data.category_breakdown[:3]
    ]

    recent_txs = [
        {"desc": e.description or "Expense", "amount": str(e.amount), "date": str(e.date)}
        for e in dash_data.recent_expenses[:5]
    ]

    context = {
        "current_month_spent": str(total_spent),
        "budget_limit": str(budget_limit) if budget_limit else None,
        "safe_daily_spend": str(safe_spend_res.safe_daily_spend),
        "burn_rate": str(safe_spend_res.daily_burn_rate),
        "days_elapsed": days_elapsed,
        "days_remaining": safe_spend_res.days_remaining,
        "top_categories": top_cats,
        "recent_transactions": recent_txs,
    }

    result = await ai_service.chat_with_financial_assistant(
        message=payload.message,
        history=payload.history,
        context=context,
    )
    return {"data": result}


@router.get("/anomalies", response_model=DataEnvelope[AnomaliesResponse], status_code=status.HTTP_200_OK)
async def get_spending_anomalies(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Flag statistically abnormal spending spikes and out-of-pattern transactions.
    """
    today = get_current_date()
    date_from = today - timedelta(days=90)

    expense_repo = ExpenseRepository(db)
    past_expenses = await expense_repo.get_expenses_for_period(current_user.id, date_from, today)

    budget_repo = BudgetRepository(db)
    budget = await budget_repo.get_budget(current_user.id, period_type="month")
    budget_limit = Decimal(str(budget.amount)) if budget and budget.amount else None

    result = ai_service.detect_spending_anomalies(
        expenses=past_expenses,
        budget_limit=budget_limit,
    )
    return {"data": result}


@router.get("/forecast", response_model=DataEnvelope[SpendingForecastResponse], status_code=status.HTTP_200_OK)
async def get_spending_forecast(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Predictive 30-day category-wise and overall spend forecast based on moving velocity.
    """
    today = get_current_date()
    month_start = date(today.year, today.month, 1)
    month_end = date(today.year, today.month, calendar.monthrange(today.year, today.month)[1])

    expense_repo = ExpenseRepository(db)
    current_expenses = await expense_repo.get_expenses_for_period(current_user.id, month_start, month_end)
    past_90_expenses = await expense_repo.get_expenses_for_period(current_user.id, today - timedelta(days=90), today)

    result = ai_service.generate_spending_forecast(
        current_expenses=current_expenses,
        past_expenses=past_90_expenses,
        days_elapsed=today.day,
        total_days=calendar.monthrange(today.year, today.month)[1],
    )
    return {"data": result}


@router.post("/savings-plan", response_model=DataEnvelope[SavingsPlanResponse], status_code=status.HTTP_200_OK)
async def create_savings_plan(
    payload: SavingsPlanRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Generate an actionable goal-based savings roadmap with category cut recommendations.
    """
    today = get_current_date()
    date_from = today - timedelta(days=90)

    expense_repo = ExpenseRepository(db)
    past_expenses = await expense_repo.get_expenses_for_period(current_user.id, date_from, today)

    result = ai_service.generate_savings_plan(
        target_amount=payload.target_amount,
        target_months=payload.target_months,
        goal_name=payload.goal_name,
        past_expenses=past_expenses,
    )
    return {"data": result}


@router.post("/analyze-sentiment", response_model=DataEnvelope[AnalyzeSentimentResponse], status_code=status.HTTP_200_OK)
async def analyze_sentiment(
    payload: AnalyzeSentimentRequest,
    current_user: User = Depends(get_current_user),
) -> dict:
    """
    Analyze expense note or description for psychological spending triggers and remorse/stress.
    """
    result = ai_service.analyze_expense_sentiment(
        text=payload.text,
        amount=payload.amount,
    )
    return {"data": result}


@router.get("/monthly-wrapped", response_model=DataEnvelope[MonthlyWrappedResponse], status_code=status.HTTP_200_OK)
async def get_monthly_wrapped(
    month: Optional[str] = Query(None, pattern=r"^\d{4}-\d{2}$", description="Month in YYYY-MM format"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Spotify-Wrapped-style monthly spending retrospective and financial personality archetype.
    """
    today = get_current_date()
    if not month:
        month = today.strftime("%Y-%m")

    year_val, month_val = [int(p) for p in month.split("-")]
    m_start = date(year_val, month_val, 1)
    m_end = date(year_val, month_val, calendar.monthrange(year_val, month_val)[1])

    expense_repo = ExpenseRepository(db)
    month_expenses = await expense_repo.get_expenses_for_period(current_user.id, m_start, m_end)

    budget_repo = BudgetRepository(db)
    budget = await budget_repo.get_budget(current_user.id, period_type="month", period_key=month)
    budget_limit = Decimal(str(budget.amount)) if budget and budget.amount else None

    achievements_res = await ai_service.calculate_achievements(
        expenses=month_expenses,
        budget=budget,
        past_expenses=month_expenses,
    )

    month_name = date(year_val, month_val, 1).strftime("%B %Y")
    result = ai_service.generate_monthly_wrapped(
        expenses=month_expenses,
        month_str=month_name,
        budget_limit=budget_limit,
        active_streak_days=achievements_res.active_streak_days,
    )
    return {"data": result}


@router.get("/vibe-check", response_model=DataEnvelope[VibeCheckResponse], status_code=status.HTTP_200_OK)
async def get_vibe_check(
    roast_mode: bool = Query(True, description="Enable spicy Hinglish / humorous roast commentary"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """
    Live emoji vibe check and roast commentary tied to burn velocity.
    """
    dashboard_service = DashboardService(db)
    dash_data = await dashboard_service.get_dashboard_data(user_id=current_user.id, period="current_month")

    today = get_current_date()
    days_elapsed = today.day
    total_days = calendar.monthrange(today.year, today.month)[1]

    budget_limit = Decimal(dash_data.budget.amount) if dash_data.budget.amount else None
    total_spent = Decimal(str(dash_data.total_spent))

    result = ai_service.generate_vibe_check(
        total_spent=total_spent,
        budget_limit=budget_limit,
        days_elapsed=days_elapsed,
        total_days=total_days,
        is_roast_mode=roast_mode,
    )
    return {"data": result}




