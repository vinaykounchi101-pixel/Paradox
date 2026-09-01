import logging
import uuid
from datetime import datetime, timedelta, timezone
from typing import Optional, Tuple

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.exceptions import AuthenticationError, ConflictError, NotFoundError
from app.core.security import (
    create_access_token,
    generate_password_reset_token,
    generate_refresh_token,
    get_password_hash,
    hash_token,
    verify_google_id_token,
    verify_password,
)
from app.db.models.user import User
from app.repositories.auth_repository import AuthRepository
from app.schemas.auth import (
    ChangePasswordRequest,
    GoogleLoginRequest,
    ResetPasswordRequest,
    UserLoginRequest,
    UserRegisterRequest,
)

logger = logging.getLogger(__name__)


class AuthService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.auth_repo = AuthRepository(db)

    async def register(
        self, data: UserRegisterRequest, user_agent: Optional[str] = None
    ) -> Tuple[str, str, User]:
        """Register a new user, hashes password, and issues initial access + refresh tokens."""
        existing_user = await self.auth_repo.get_user_by_email(data.email)
        if existing_user:
            raise ConflictError("An account with this email already exists.")

        password_hash = get_password_hash(data.password)
        user = await self.auth_repo.create_user(
            email=data.email,
            display_name=data.display_name or "User",
            password_hash=password_hash,
            is_verified=False,
        )

        # Issue Tokens
        access_token = create_access_token(subject=str(user.id), email=user.email)
        raw_refresh, token_hash = generate_refresh_token()
        expires_at = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)

        await self.auth_repo.create_refresh_token(
            user_id=user.id,
            token_hash=token_hash,
            expires_at=expires_at,
            user_agent=user_agent,
        )

        return access_token, raw_refresh, user

    async def login(
        self, data: UserLoginRequest, user_agent: Optional[str] = None
    ) -> Tuple[str, str, User]:
        """Validate email/password and issue fresh access + refresh tokens."""
        user = await self.auth_repo.get_user_by_email(data.email)
        if not user or not user.password_hash:
            raise AuthenticationError("Invalid email or password.")

        if not verify_password(data.password, user.password_hash):
            raise AuthenticationError("Invalid email or password.")

        # Issue Tokens
        access_token = create_access_token(subject=str(user.id), email=user.email)
        raw_refresh, token_hash = generate_refresh_token()
        expires_at = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)

        await self.auth_repo.create_refresh_token(
            user_id=user.id,
            token_hash=token_hash,
            expires_at=expires_at,
            user_agent=user_agent,
        )

        return access_token, raw_refresh, user

    async def login_with_google(
        self, data: GoogleLoginRequest, user_agent: Optional[str] = None
    ) -> Tuple[str, str, User]:
        """Verify Google ID token, link/provision user, and issue access + refresh tokens."""
        payload = verify_google_id_token(data.id_token)
        if not payload:
            raise AuthenticationError("Invalid Google authentication token.")

        google_id = payload.get("sub")
        email = payload.get("email")
        display_name = payload.get("name") or "Google User"
        avatar_url = payload.get("picture")

        if not email or not google_id:
            raise AuthenticationError("Incomplete profile data received from Google.")

        user = await self.auth_repo.get_user_by_email(email)
        if user:
            # Link google_id or update avatar if needed
            if not user.google_id:
                user.google_id = google_id
            if avatar_url and not user.avatar_url:
                user.avatar_url = avatar_url
            user = await self.auth_repo.update_user(user)
        else:
            # Provision new verified OAuth user
            user = await self.auth_repo.create_user(
                email=email,
                display_name=display_name,
                google_id=google_id,
                avatar_url=avatar_url,
                is_verified=True,
            )

        # Issue Tokens
        access_token = create_access_token(subject=str(user.id), email=user.email)
        raw_refresh, token_hash = generate_refresh_token()
        expires_at = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)

        await self.auth_repo.create_refresh_token(
            user_id=user.id,
            token_hash=token_hash,
            expires_at=expires_at,
            user_agent=user_agent,
        )

        return access_token, raw_refresh, user

    async def refresh(
        self, raw_refresh_token: str, user_agent: Optional[str] = None
    ) -> Tuple[str, str, User]:
        """
        Refresh Token Rotation:
        Validates refresh token hash, revokes it immediately, and returns a new access + refresh pair.
        """
        if not raw_refresh_token:
            raise AuthenticationError("Missing refresh token.")

        token_hash = hash_token(raw_refresh_token)
        refresh_token_record = await self.auth_repo.get_refresh_token_by_hash(token_hash)

        now = datetime.now(timezone.utc)
        if not refresh_token_record or refresh_token_record.is_revoked:
            raise AuthenticationError("Invalid or revoked refresh token.")

        if refresh_token_record.expires_at < now:
            await self.auth_repo.revoke_refresh_token(token_hash)
            raise AuthenticationError("Refresh token has expired.")

        # Revoke the used token (Rotation)
        await self.auth_repo.revoke_refresh_token(token_hash)

        user = await self.auth_repo.get_user_by_id(refresh_token_record.user_id)
        if not user:
            raise AuthenticationError("User not found.")

        # Issue brand new token pair
        access_token = create_access_token(subject=str(user.id), email=user.email)
        new_raw_refresh, new_token_hash = generate_refresh_token()
        expires_at = now + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)

        await self.auth_repo.create_refresh_token(
            user_id=user.id,
            token_hash=new_token_hash,
            expires_at=expires_at,
            user_agent=user_agent,
        )

        return access_token, new_raw_refresh, user

    async def logout(self, raw_refresh_token: Optional[str]) -> None:
        """Revoke current session's refresh token."""
        if raw_refresh_token:
            token_hash = hash_token(raw_refresh_token)
            await self.auth_repo.revoke_refresh_token(token_hash)

    async def logout_all(self, user_id: uuid.UUID) -> None:
        """Revoke all active refresh tokens for the user across all devices."""
        await self.auth_repo.revoke_all_user_refresh_tokens(user_id)

    async def forgot_password(self, email: str) -> Optional[str]:
        """Generate a password reset token for valid user."""
        user = await self.auth_repo.get_user_by_email(email)
        if not user:
            # Return None to avoid email enumeration
            return None

        raw_token, token_hash = generate_password_reset_token()
        expires_at = datetime.now(timezone.utc) + timedelta(hours=1)

        await self.auth_repo.create_password_reset_token(
            user_id=user.id,
            token_hash=token_hash,
            expires_at=expires_at,
        )

        # In production, dispatch email via provider. In local/dev, return reset token.
        logger.info("Password reset token generated for user %s: %s", user.email, raw_token)
        return raw_token

    async def reset_password(self, data: ResetPasswordRequest) -> None:
        """Validate reset token, update password, and revoke all active sessions."""
        token_hash = hash_token(data.token)
        reset_record = await self.auth_repo.get_password_reset_token_by_hash(token_hash)

        now = datetime.now(timezone.utc)
        if not reset_record or reset_record.is_used or reset_record.expires_at < now:
            raise AuthenticationError("Invalid or expired password reset token.")

        # Update password hash
        new_hash = get_password_hash(data.new_password)
        await self.auth_repo.update_user_password(reset_record.user_id, new_hash)

        # Mark token as used
        await self.auth_repo.mark_password_reset_token_used(token_hash)

        # Revoke all existing sessions for security
        await self.auth_repo.revoke_all_user_refresh_tokens(reset_record.user_id)

    async def change_password(self, user_id: uuid.UUID, data: ChangePasswordRequest) -> None:
        """Change password for an authenticated user verifying their current password."""
        user = await self.auth_repo.get_user_by_id(user_id)
        if not user:
            raise NotFoundError("User not found.")

        if not user.password_hash or not verify_password(data.current_password, user.password_hash):
            raise AuthenticationError("Incorrect current password.")

        new_hash = get_password_hash(data.new_password)
        await self.auth_repo.update_user_password(user_id, new_hash)
