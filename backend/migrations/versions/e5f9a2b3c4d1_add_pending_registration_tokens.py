"""add pending registration tokens table

Revision ID: e5f9a2b3c4d1
Revises: d4e8f2a1b9c3
Create Date: 2026-09-02 17:45:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "e5f9a2b3c4d1"
down_revision: Union[str, None] = "d4e8f2a1b9c3"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "pending_registration_tokens",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("email", sa.String(length=255), nullable=False),
        sa.Column("token_hash", sa.String(length=64), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_used", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
    )
    op.create_index("ix_pending_registration_tokens_email", "pending_registration_tokens", ["email"])
    op.create_index("ix_pending_registration_tokens_token_hash", "pending_registration_tokens", ["token_hash"], unique=True)
    op.create_index("ix_pending_reg_email_hash", "pending_registration_tokens", ["email", "token_hash"])


def downgrade() -> None:
    op.drop_index("ix_pending_reg_email_hash", table_name="pending_registration_tokens")
    op.drop_index("ix_pending_registration_tokens_token_hash", table_name="pending_registration_tokens")
    op.drop_index("ix_pending_registration_tokens_email", table_name="pending_registration_tokens")
    op.drop_table("pending_registration_tokens")
