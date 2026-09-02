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
    generate_registration_token,
    get_password_hash,
    hash_token,
    verify_google_id_token,
    verify_password,
)
from app.db.models.user import User
from app.repositories.auth_repository import AuthRepository
from app.services.email_service import email_service
from app.schemas.auth import (
    ChangePasswordRequest,
    CompleteRegistrationRequest,
    GoogleLoginRequest,
    InitiateRegistrationRequest,
    ResetPasswordRequest,
    UserLoginRequest,
    UserRegisterRequest,
)

logger = logging.getLogger(__name__)


class AuthService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.auth_repo = AuthRepository(db)

    async def initiate_registration(self, email: str) -> str:
        """
        Initiates pre-registration verification.
        Checks if user exists, generates secure token, and emails the verification setup link.
        """
        clean_email = email.lower().strip()
        existing_user = await self.auth_repo.get_user_by_email(clean_email)
        if existing_user:
            raise ConflictError("An account with this email address already exists.")

        raw_token, token_hash = generate_registration_token()
        expires_at = datetime.now(timezone.utc) + timedelta(hours=1)

        await self.auth_repo.create_pending_registration_token(
            email=clean_email,
            token_hash=token_hash,
            expires_at=expires_at,
        )

        await email_service.send_registration_verification_email(
            to_email=clean_email,
            token=raw_token,
        )

        logger.info("Pre-registration verification link dispatched to: %s", clean_email)
        return clean_email

    async def validate_registration_token(self, token: str) -> str:
        """
        Validates the pre-registration token and returns the verified email.
        """
        if not token:
            raise AuthenticationError("Missing registration verification token.")

        token_hash = hash_token(token)
        record = await self.auth_repo.get_pending_registration_token_by_hash(token_hash)
        now = datetime.now(timezone.utc)

        if not record or record.is_used or record.expires_at < now:
            raise AuthenticationError("Invalid or expired registration verification link.")

        # Ensure user wasn't registered in the interim
        existing_user = await self.auth_repo.get_user_by_email(record.email)
        if existing_user:
            raise ConflictError("An account with this email address is already registered.")

        return record.email

    async def complete_registration(
        self, data: CompleteRegistrationRequest, user_agent: Optional[str] = None
    ) -> Tuple[str, str, User]:
        """
        Completes user registration after email has been verified via token.
        Creates verified user in database and issues JWT session tokens.
        """
        email = await self.validate_registration_token(data.token)
        token_hash = hash_token(data.token)

        password_hash = get_password_hash(data.password)
        user = await self.auth_repo.create_user(
            email=email,
            display_name=data.display_name.strip() if data.display_name else "User",
            password_hash=password_hash,
            is_verified=True,
        )

        # Mark token as used
        await self.auth_repo.mark_pending_registration_token_used(token_hash)

        # Issue Tokens
        access_token = create_access_token(subject=str(user.id), email=user.email)
        raw_refresh, refresh_hash = generate_refresh_token()
        expires_at = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)

        await self.auth_repo.create_refresh_token(
            user_id=user.id,
            token_hash=refresh_hash,
            expires_at=expires_at,
            user_agent=user_agent,
        )

        logger.info("User registration completed for verified email: %s", user.email)
        return access_token, raw_refresh, user

    async def register(
        self, data: UserRegisterRequest, user_agent: Optional[str] = None
    ) -> Tuple[str, str, User]:
        """Legacy direct register fallback (marks is_verified=True)."""
        existing_user = await self.auth_repo.get_user_by_email(data.email)
        if existing_user:
            raise ConflictError("An account with this email already exists.")

        password_hash = get_password_hash(data.password)
        user = await self.auth_repo.create_user(
            email=data.email,
            display_name=data.display_name or "User",
            password_hash=password_hash,
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
        """Generate a password reset token for valid user and dispatch email."""
        user = await self.auth_repo.get_user_by_email(email)
        if not user:
            raise NotFoundError("No user account found with this email address.")

        raw_token, token_hash = generate_password_reset_token()
        expires_at = datetime.now(timezone.utc) + timedelta(hours=1)

        await self.auth_repo.create_password_reset_token(
            user_id=user.id,
            token_hash=token_hash,
            expires_at=expires_at,
        )

        # Dispatch via SMTP
        await email_service.send_password_reset_email(
            to_email=user.email,
            display_name=user.display_name or "User",
            reset_token=raw_token,
        )

        logger.info("Password reset token generated and dispatched for user: %s", user.email)
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
