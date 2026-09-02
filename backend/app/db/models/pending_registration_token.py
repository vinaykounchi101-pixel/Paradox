import uuid
from datetime import datetime, timezone
from sqlalchemy import Column, String, DateTime, Boolean, Index
from sqlalchemy.dialects.postgresql import UUID

from app.db.base import Base


class PendingRegistrationToken(Base):
    """
    Model for storing pre-registration email verification tokens.
    Token hash is checked before creating a user account in the database.
    """
    __tablename__ = "pending_registration_tokens"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email = Column(String(255), nullable=False, index=True)
    token_hash = Column(String(64), nullable=False, unique=True, index=True)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    is_used = Column(Boolean, default=False, nullable=False)
    created_at = Column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    __table_args__ = (
        Index("ix_pending_reg_email_hash", "email", "token_hash"),
    )
