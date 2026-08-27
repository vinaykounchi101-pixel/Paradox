import uuid
from decimal import Decimal
from datetime import datetime
from typing import Optional

from sqlalchemy import CheckConstraint, DateTime, Numeric, String, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class Budget(Base):
    __tablename__ = "budgets"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
    )
    period_type: Mapped[str] = mapped_column(
        String(10),
        default="month",
        nullable=False,
    )
    period_key: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        index=True,
    )
    month: Mapped[Optional[str]] = mapped_column(
        String(7),
        nullable=True,
    )
    amount: Mapped[Decimal] = mapped_column(
        Numeric(12, 2),
        nullable=False,
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )

    __table_args__ = (
        CheckConstraint("amount >= 0", name="chk_budget_amount_non_negative"),
        UniqueConstraint("period_type", "period_key", name="uq_budget_period"),
    )
