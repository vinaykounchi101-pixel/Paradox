"""Initial schema

Revision ID: 80a4bc410a4b
Revises: 
Create Date: 2026-08-26 17:10:39.781727

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '80a4bc410a4b'
down_revision: Union[str, Sequence[str], None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create users table
    op.create_table(
        'users',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('display_name', sa.String(length=100), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.PrimaryKeyConstraint('id')
    )

    # 2. Create categories table
    op.create_table(
        'categories',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('name', sa.String(length=60), nullable=False),
        sa.Column('is_default', sa.Boolean(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('name')
    )

    # 3. Create payment_methods table
    op.create_table(
        'payment_methods',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('name', sa.String(length=60), nullable=False),
        sa.Column('is_default', sa.Boolean(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('name')
    )

    # 4. Create budgets table
    op.create_table(
        'budgets',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('amount', sa.Numeric(precision=12, scale=2), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.CheckConstraint('amount >= 0', name='chk_budget_amount_non_negative')
    )

    # 5. Create expenses table
    op.create_table(
        'expenses',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('amount', sa.Numeric(precision=12, scale=2), nullable=False),
        sa.Column('category_id', sa.UUID(), nullable=False),
        sa.Column('payment_method_id', sa.UUID(), nullable=False),
        sa.Column('date', sa.Date(), nullable=False),
        sa.Column('description', sa.String(length=255), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['category_id'], ['categories.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['payment_method_id'], ['payment_methods.id'], ondelete='RESTRICT'),
        sa.PrimaryKeyConstraint('id'),
        sa.CheckConstraint('amount > 0', name='chk_expense_amount_positive')
    )
    
    # 6. Create indexes for expenses
    op.create_index('idx_expenses_date', 'expenses', ['date'], unique=False)
    op.create_index('idx_expenses_category_id', 'expenses', ['category_id'], unique=False)
    op.create_index('idx_expenses_payment_method_id', 'expenses', ['payment_method_id'], unique=False)
    op.create_index('idx_expenses_date_category', 'expenses', ['date', 'category_id'], unique=False)

    # 7. Seed single placeholder user
    op.execute(
        "INSERT INTO users (id, display_name, created_at, updated_at) "
        "VALUES ('00000000-0000-0000-0000-000000000001', 'Primary User', NOW(), NOW())"
    )

    # 8. Seed starter categories
    from app.constants.categories import STARTER_CATEGORIES
    for name, cid in STARTER_CATEGORIES.items():
        op.execute(
            f"INSERT INTO categories (id, name, is_default, created_at, updated_at) "
            f"VALUES ('{cid}', '{name}', true, NOW(), NOW())"
        )

    # 9. Seed starter payment methods
    from app.constants.payment_methods import STARTER_PAYMENT_METHODS
    for name, pmid in STARTER_PAYMENT_METHODS.items():
        op.execute(
            f"INSERT INTO payment_methods (id, name, is_default, created_at, updated_at) "
            f"VALUES ('{pmid}', '{name}', true, NOW(), NOW())"
        )


def downgrade() -> None:
    # 1. Drop indexes
    op.drop_index('idx_expenses_date_category', table_name='expenses')
    op.drop_index('idx_expenses_payment_method_id', table_name='expenses')
    op.drop_index('idx_expenses_category_id', table_name='expenses')
    op.drop_index('idx_expenses_date', table_name='expenses')

    # 2. Drop tables
    op.drop_table('expenses')
    op.drop_table('budgets')
    op.drop_table('payment_methods')
    op.drop_table('categories')
    op.drop_table('users')
