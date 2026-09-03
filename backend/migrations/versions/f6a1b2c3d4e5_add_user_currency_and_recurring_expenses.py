"""add user currency and recurring expenses

Revision ID: f6a1b2c3d4e5
Revises: e5f9a2b3c4d1
Create Date: 2026-09-03 19:30:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "f6a1b2c3d4e5"
down_revision: Union[str, None] = "e5f9a2b3c4d1"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column("currency", sa.String(length=10), nullable=False, server_default=sa.text("'INR'")),
    )
    op.add_column(
        "expenses",
        sa.Column("is_recurring", sa.Boolean(), nullable=False, server_default=sa.text("false")),
    )
    op.add_column(
        "expenses",
        sa.Column("recurring_frequency", sa.String(length=20), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("expenses", "recurring_frequency")
    op.drop_column("expenses", "is_recurring")
    op.drop_column("users", "currency")
