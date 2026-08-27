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
    # 1. Add month column as nullable first
    op.add_column('budgets', sa.Column('month', sa.String(length=7), nullable=True))
    
    # 2. Backfill any existing budget records with current month YYYY-MM
    current_month = datetime.utcnow().strftime("%Y-%m")
    op.execute(f"UPDATE budgets SET month = '{current_month}' WHERE month IS NULL")
    
    # 3. Alter month column to nullable=False, unique constraint, and index
    op.alter_column('budgets', 'month', nullable=False)
    op.create_unique_constraint('uq_budgets_month', 'budgets', ['month'])
    op.create_index('idx_budgets_month', 'budgets', ['month'], unique=False)


def downgrade() -> None:
    op.drop_index('idx_budgets_month', table_name='budgets')
    op.drop_constraint('uq_budgets_month', 'budgets', type_='unique')
    op.drop_column('budgets', 'month')
