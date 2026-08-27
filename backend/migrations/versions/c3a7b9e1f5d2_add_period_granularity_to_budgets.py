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
    op.add_column('budgets', sa.Column('period_type', sa.String(length=10), server_default='month', nullable=False))
    
    # 2. Add period_key
    op.add_column('budgets', sa.Column('period_key', sa.String(length=20), nullable=True))
    
    # 3. Backfill period_key from month
    op.execute("UPDATE budgets SET period_key = month WHERE period_key IS NULL")
    
    # 4. Make period_key not nullable and make month nullable
    op.alter_column('budgets', 'period_key', nullable=False)
    op.alter_column('budgets', 'month', nullable=True)
    
    # 5. Drop uq_budgets_month if present and create uq_budget_period
    try:
        op.drop_constraint('uq_budgets_month', 'budgets', type_='unique')
    except Exception:
        pass

    op.create_unique_constraint('uq_budget_period', 'budgets', ['period_type', 'period_key'])
    op.create_index('idx_budgets_period', 'budgets', ['period_type', 'period_key'], unique=False)


def downgrade() -> None:
    op.drop_index('idx_budgets_period', table_name='budgets')
    op.drop_constraint('uq_budget_period', 'budgets', type_='unique')
    op.drop_column('budgets', 'period_key')
    op.drop_column('budgets', 'period_type')
