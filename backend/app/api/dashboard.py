from typing import Optional

from fastapi import APIRouter, Depends, Query

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.schemas.common import DataEnvelope
from app.schemas.dashboard import DashboardRead
from app.services.dashboard_service import DashboardService

router = APIRouter()


@router.get("", response_model=DataEnvelope[DashboardRead])
async def get_dashboard(
    period: str = Query("current_month", pattern="^(current_month|last_30_days|current_week)$"),
    current_user: User = Depends(get_current_user),
    db=Depends(get_db),
) -> dict:
    """Retrieve the aggregated dashboard stats and recent transactions strictly for authenticated user."""
    service = DashboardService(db)
    data = await service.get_dashboard_data(user_id=current_user.id, period=period)
    return {"data": data}
