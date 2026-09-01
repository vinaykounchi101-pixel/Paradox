"""add auth and user isolation

Revision ID: d4e8f2a1b9c3
Revises: c3a7b9e1f5d2
Create Date: 2026-08-31 19:40:00.000000

"""
from typing import Sequence, Union
import uuid

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID


# revision identifiers, used by Alembic.
revision: str = 'd4e8f2a1b9c3'
down_revision: Union[str, Sequence[str], None] = 'c3a7b9e1f5d2'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Update users table
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE NOT NULL;")
    
    # Backfill default email for existing user if email is null
    op.execute("UPDATE users SET email = 'default_user@paradox.local' WHERE email IS NULL;")
    op.execute("ALTER TABLE users ALTER COLUMN email SET NOT NULL;")
    op.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users (email);")
    op.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_users_google_id ON users (google_id) WHERE google_id IS NOT NULL;")

    # Ensure at least one default user exists for foreign key backfills
    op.execute("""
        INSERT INTO users (id, email, display_name, is_verified, created_at, updated_at)
        SELECT '00000000-0000-0000-0000-000000000001'::uuid, 'primary@paradox.local', 'Primary User', true, NOW(), NOW()
        WHERE NOT EXISTS (SELECT 1 FROM users);
    """)

    # 2. Create refresh_tokens table
    op.create_table(
        'refresh_tokens',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, default=uuid.uuid4),
        sa.Column('user_id', UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('token_hash', sa.String(255), nullable=False),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('is_revoked', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('user_agent', sa.String(255), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_index('idx_refresh_tokens_user_id', 'refresh_tokens', ['user_id'])
    op.create_index('idx_refresh_tokens_token_hash', 'refresh_tokens', ['token_hash'], unique=True)
    op.create_index('idx_refresh_tokens_is_revoked', 'refresh_tokens', ['is_revoked'])

    # 3. Create password_reset_tokens table
    op.create_table(
        'password_reset_tokens',
        sa.Column('id', UUID(as_uuid=True), primary_key=True, default=uuid.uuid4),
        sa.Column('user_id', UUID(as_uuid=True), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False),
        sa.Column('token_hash', sa.String(255), nullable=False),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('is_used', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_index('idx_password_reset_tokens_user_id', 'password_reset_tokens', ['user_id'])
    op.create_index('idx_password_reset_tokens_token_hash', 'password_reset_tokens', ['token_hash'], unique=True)

    # 4. Add user_id to expenses
    op.execute("ALTER TABLE expenses ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;")
    op.execute("UPDATE expenses SET user_id = (SELECT id FROM users ORDER BY created_at ASC LIMIT 1) WHERE user_id IS NULL;")
    op.execute("ALTER TABLE expenses ALTER COLUMN user_id SET NOT NULL;")
    op.execute("CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON expenses (user_id);")
    op.execute("CREATE INDEX IF NOT EXISTS idx_expenses_user_date ON expenses (user_id, date);")

    # 5. Add user_id to budgets and update unique constraint
    op.execute("ALTER TABLE budgets ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;")
    op.execute("UPDATE budgets SET user_id = (SELECT id FROM users ORDER BY created_at ASC LIMIT 1) WHERE user_id IS NULL;")
    op.execute("ALTER TABLE budgets ALTER COLUMN user_id SET NOT NULL;")
    op.execute("ALTER TABLE budgets DROP CONSTRAINT IF EXISTS uq_budget_period;")
    op.execute("ALTER TABLE budgets DROP CONSTRAINT IF EXISTS uq_user_budget_period;")
    op.execute("ALTER TABLE budgets ADD CONSTRAINT uq_user_budget_period UNIQUE (user_id, period_type, period_key);")
    op.execute("CREATE INDEX IF NOT EXISTS idx_budgets_user_period ON budgets (user_id, period_type, period_key);")

    # 6. Add user_id to categories and relax global name uniqueness
    op.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;")
    op.execute("CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories (user_id);")
    op.execute("ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_name_key;")
    op.execute("ALTER TABLE categories DROP CONSTRAINT IF EXISTS uq_categories_name;")

    # 7. Add user_id to payment_methods and relax global name uniqueness
    op.execute("ALTER TABLE payment_methods ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;")
    op.execute("CREATE INDEX IF NOT EXISTS idx_payment_methods_user_id ON payment_methods (user_id);")
    op.execute("ALTER TABLE payment_methods DROP CONSTRAINT IF EXISTS payment_methods_name_key;")
    op.execute("ALTER TABLE payment_methods DROP CONSTRAINT IF EXISTS uq_payment_methods_name;")


def downgrade() -> None:
    op.execute("ALTER TABLE payment_methods DROP COLUMN IF EXISTS user_id;")
    op.execute("ALTER TABLE categories DROP COLUMN IF EXISTS user_id;")
    op.execute("ALTER TABLE budgets DROP CONSTRAINT IF EXISTS uq_user_budget_period;")
    op.execute("ALTER TABLE budgets DROP COLUMN IF EXISTS user_id;")
    op.execute("ALTER TABLE expenses DROP COLUMN IF EXISTS user_id;")
    op.drop_table('password_reset_tokens')
    op.drop_table('refresh_tokens')
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS is_verified;")
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS avatar_url;")
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS google_id;")
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS password_hash;")
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS email;")
