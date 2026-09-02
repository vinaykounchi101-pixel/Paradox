import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models.password_reset_token import PasswordResetToken
from app.db.models.refresh_token import RefreshToken
from app.db.models.pending_registration_token import PendingRegistrationToken
from app.db.models.user import User


class AuthRepository:
    def __init__(self, session: AsyncSession):
        self.session = session

    async def get_user_by_email(self, email: str) -> Optional[User]:
        stmt = select(User).where(User.email == email.lower().strip())
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_user_by_google_id(self, google_id: str) -> Optional[User]:
        stmt = select(User).where(User.google_id == google_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_user_by_id(self, user_id: uuid.UUID) -> Optional[User]:
        stmt = select(User).where(User.id == user_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def create_user(
        self,
        email: str,
        display_name: str,
        password_hash: Optional[str] = None,
        google_id: Optional[str] = None,
        avatar_url: Optional[str] = None,
        is_verified: bool = False,
    ) -> User:
        user = User(
            email=email.lower().strip(),
            display_name=display_name.strip() if display_name else "User",
            password_hash=password_hash,
            google_id=google_id,
            avatar_url=avatar_url,
            is_verified=is_verified,
        )
        self.session.add(user)
        await self.session.flush()
        await self.session.refresh(user)
        return user

    async def update_user(self, user: User) -> User:
        await self.session.flush()
        await self.session.refresh(user)
        return user

    async def update_user_password(self, user_id: uuid.UUID, password_hash: str) -> None:
        stmt = (
            update(User)
            .where(User.id == user_id)
            .values(password_hash=password_hash, updated_at=datetime.now(timezone.utc))
        )
        await self.session.execute(stmt)
        await self.session.flush()

    # --- Refresh Token Operations ---

    async def create_refresh_token(
        self,
        user_id: uuid.UUID,
        token_hash: str,
        expires_at: datetime,
        user_agent: Optional[str] = None,
    ) -> RefreshToken:
        token = RefreshToken(
            user_id=user_id,
            token_hash=token_hash,
            expires_at=expires_at,
            user_agent=user_agent[:255] if user_agent else None,
            is_revoked=False,
        )
        self.session.add(token)
        await self.session.flush()
        return token

    async def get_refresh_token_by_hash(self, token_hash: str) -> Optional[RefreshToken]:
        stmt = select(RefreshToken).where(RefreshToken.token_hash == token_hash)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def revoke_refresh_token(self, token_hash: str) -> None:
        stmt = (
            update(RefreshToken)
            .where(RefreshToken.token_hash == token_hash)
            .values(is_revoked=True)
        )
        await self.session.execute(stmt)
        await self.session.flush()

    async def revoke_all_user_refresh_tokens(self, user_id: uuid.UUID) -> None:
        stmt = (
            update(RefreshToken)
            .where(RefreshToken.user_id == user_id, RefreshToken.is_revoked == False)  # noqa: E712
            .values(is_revoked=True)
        )
        await self.session.execute(stmt)
        await self.session.flush()

    # --- Password Reset Token Operations ---

    async def create_password_reset_token(
        self,
        user_id: uuid.UUID,
        token_hash: str,
        expires_at: datetime,
    ) -> PasswordResetToken:
        token = PasswordResetToken(
            user_id=user_id,
            token_hash=token_hash,
            expires_at=expires_at,
            is_used=False,
        )
        self.session.add(token)
        await self.session.flush()
        return token

    async def get_password_reset_token_by_hash(self, token_hash: str) -> Optional[PasswordResetToken]:
        stmt = select(PasswordResetToken).where(PasswordResetToken.token_hash == token_hash)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def mark_password_reset_token_used(self, token_hash: str) -> None:
        stmt = (
            update(PasswordResetToken)
            .where(PasswordResetToken.token_hash == token_hash)
            .values(is_used=True)
        )
        await self.session.execute(stmt)
        await self.session.flush()

    # --- Pre-Registration Token Operations ---

    async def create_pending_registration_token(
        self,
        email: str,
        token_hash: str,
        expires_at: datetime,
    ) -> PendingRegistrationToken:
        token = PendingRegistrationToken(
            email=email.lower().strip(),
            token_hash=token_hash,
            expires_at=expires_at,
            is_used=False,
        )
        self.session.add(token)
        await self.session.flush()
        return token

    async def get_pending_registration_token_by_hash(
        self, token_hash: str
    ) -> Optional[PendingRegistrationToken]:
        stmt = select(PendingRegistrationToken).where(
            PendingRegistrationToken.token_hash == token_hash
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def mark_pending_registration_token_used(self, token_hash: str) -> None:
        stmt = (
            update(PendingRegistrationToken)
            .where(PendingRegistrationToken.token_hash == token_hash)
            .values(is_used=True)
        )
        await self.session.execute(stmt)
        await self.session.flush()
