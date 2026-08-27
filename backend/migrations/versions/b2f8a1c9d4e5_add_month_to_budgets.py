"""add month to budgets

Revision ID: b2f8a1c9d4e5
Revises: 80a4bc410a4b
Create Date: 2026-08-27 19:20:00.000000

"""
from typing import Sequence, Union
from datetime import datetime

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'b2f8a1c9d4e5'
down_revision: Union[str, Sequence[str], None] = '80a4bc410a4b'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute("ALTER TABLE budgets ADD COLUMN IF NOT EXISTS month VARCHAR(7);")
    current_month = datetime.utcnow().strftime("%Y-%m")
    op.execute(f"UPDATE budgets SET month = '{current_month}' WHERE month IS NULL;")
    op.execute("CREATE INDEX IF NOT EXISTS idx_budgets_month ON budgets (month);")


def downgrade() -> None:
    op.execute("DROP INDEX IF EXISTS idx_budgets_month;")
    op.execute("ALTER TABLE budgets DROP COLUMN IF EXISTS month;")
