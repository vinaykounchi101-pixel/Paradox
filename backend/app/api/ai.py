from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.schemas.ai import (
    CategorizeRequest,
    CategorizeResponse,
    ParseExpenseRequest,
    ParseExpenseResponse,
)
from app.schemas.common import DataEnvelope
from app.services.ai_service import ai_service
from app.services.category_service import CategoryService
from app.services.payment_method_service import PaymentMethodService

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
