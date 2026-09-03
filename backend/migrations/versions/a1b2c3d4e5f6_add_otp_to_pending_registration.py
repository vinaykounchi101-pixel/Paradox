"""add otp and is_verified to pending registration tokens

Revision ID: a1b2c3d4e5f6
Revises: f6a1b2c3d4e5
Create Date: 2026-09-04 03:35:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "a1b2c3d4e5f6"
down_revision: Union[str, None] = "f6a1b2c3d4e5"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "pending_registration_tokens",
        sa.Column("otp_code_hash", sa.String(length=64), nullable=True),
    )
    op.add_column(
        "pending_registration_tokens",
        sa.Column("is_verified", sa.Boolean(), nullable=False, server_default=sa.text("false")),
    )
    op.create_index(
        "ix_pending_registration_tokens_email_otp",
        "pending_registration_tokens",
        ["email", "otp_code_hash"],
    )


def downgrade() -> None:
    op.drop_index("ix_pending_registration_tokens_email_otp", table_name="pending_registration_tokens")
    op.drop_column("pending_registration_tokens", "is_verified")
    op.drop_column("pending_registration_tokens", "otp_code_hash")
