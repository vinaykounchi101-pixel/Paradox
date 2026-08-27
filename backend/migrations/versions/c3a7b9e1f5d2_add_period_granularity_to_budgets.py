"""add period granularity to budgets

Revision ID: c3a7b9e1f5d2
Revises: b2f8a1c9d4e5
Create Date: 2026-08-27 19:30:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'c3a7b9e1f5d2'
down_revision: Union[str, Sequence[str], None] = 'b2f8a1c9d4e5'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add period_type with default 'month'
    op.execute("ALTER TABLE budgets ADD COLUMN IF NOT EXISTS period_type VARCHAR(10) DEFAULT 'month' NOT NULL;")
    
    # 2. Add period_key
    op.execute("ALTER TABLE budgets ADD COLUMN IF NOT EXISTS period_key VARCHAR(20);")
    
    # 3. Backfill period_key from month if available
    op.execute("UPDATE budgets SET period_key = month WHERE period_key IS NULL AND month IS NOT NULL;")
    op.execute("UPDATE budgets SET period_key = TO_CHAR(NOW(), 'YYYY-MM') WHERE period_key IS NULL;")
    
    # 4. Make period_key not nullable and make month nullable
    op.execute("ALTER TABLE budgets ALTER COLUMN period_key SET NOT NULL;")
    op.execute("ALTER TABLE budgets ALTER COLUMN month DROP NOT NULL;")
    
    # 5. Drop uq_budgets_month safely if exists
    op.execute("ALTER TABLE budgets DROP CONSTRAINT IF EXISTS uq_budgets_month;")
    op.execute("ALTER TABLE budgets DROP CONSTRAINT IF EXISTS uq_budget_period;")
    
    # 6. Create uq_budget_period constraint and index
    op.execute("ALTER TABLE budgets ADD CONSTRAINT uq_budget_period UNIQUE (period_type, period_key);")
    op.execute("CREATE INDEX IF NOT EXISTS idx_budgets_period ON budgets (period_type, period_key);")


def downgrade() -> None:
    op.execute("DROP INDEX IF EXISTS idx_budgets_period;")
    op.execute("ALTER TABLE budgets DROP CONSTRAINT IF EXISTS uq_budget_period;")
    op.execute("ALTER TABLE budgets DROP COLUMN IF EXISTS period_key;")
    op.execute("ALTER TABLE budgets DROP COLUMN IF EXISTS period_type;")
