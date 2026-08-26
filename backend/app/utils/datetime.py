from datetime import date, datetime
from zoneinfo import ZoneInfo

from app.core.config import settings


def get_current_date() -> date:
    """Returns the current date in the configured APP_TIMEZONE (defaulting to UTC)."""
    try:
        tz = ZoneInfo(settings.APP_TIMEZONE)
    except Exception:
        tz = ZoneInfo("UTC")
    return datetime.now(tz).date()
