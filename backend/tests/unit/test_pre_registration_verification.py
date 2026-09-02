import pytest
from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock, patch
import uuid

from app.core.exceptions import AuthenticationError, ConflictError
from app.core.security import generate_registration_token, get_password_hash, hash_token
from app.db.models.pending_registration_token import PendingRegistrationToken
from app.db.models.user import User
from app.schemas.auth import CompleteRegistrationRequest
from app.services.auth_service import AuthService


@pytest.mark.asyncio
async def test_initiate_registration_success():
    mock_db = AsyncMock()
    service = AuthService(mock_db)

    service.auth_repo.get_user_by_email = AsyncMock(return_value=None)
    service.auth_repo.create_pending_registration_token = AsyncMock()

    with patch("app.services.auth_service.email_service.send_registration_verification_email", new_callable=AsyncMock) as mock_email:
        mock_email.return_value = True
        res = await service.initiate_registration("newuser@example.com")
        assert res == "newuser@example.com"
        assert service.auth_repo.create_pending_registration_token.called
        assert mock_email.called


@pytest.mark.asyncio
async def test_initiate_registration_existing_email_conflict():
    mock_db = AsyncMock()
    service = AuthService(mock_db)

    existing_user = User(
        id=uuid.uuid4(),
        email="existing@example.com",
        display_name="Existing User",
        is_verified=True,
    )
    service.auth_repo.get_user_by_email = AsyncMock(return_value=existing_user)

    with pytest.raises(ConflictError) as exc_info:
        await service.initiate_registration("existing@example.com")
    assert "already exists" in str(exc_info.value.message)


@pytest.mark.asyncio
async def test_validate_registration_token_success():
    mock_db = AsyncMock()
    service = AuthService(mock_db)

    raw_token, token_hash = generate_registration_token()
    token_record = PendingRegistrationToken(
        id=uuid.uuid4(),
        email="verified@example.com",
        token_hash=token_hash,
        expires_at=datetime.now(timezone.utc) + timedelta(hours=1),
        is_used=False,
    )

    service.auth_repo.get_pending_registration_token_by_hash = AsyncMock(return_value=token_record)
    service.auth_repo.get_user_by_email = AsyncMock(return_value=None)

    email = await service.validate_registration_token(raw_token)
    assert email == "verified@example.com"


@pytest.mark.asyncio
async def test_validate_registration_token_expired():
    mock_db = AsyncMock()
    service = AuthService(mock_db)

    raw_token, token_hash = generate_registration_token()
    token_record = PendingRegistrationToken(
        id=uuid.uuid4(),
        email="expired@example.com",
        token_hash=token_hash,
        expires_at=datetime.now(timezone.utc) - timedelta(minutes=5),  # Expired
        is_used=False,
    )

    service.auth_repo.get_pending_registration_token_by_hash = AsyncMock(return_value=token_record)

    with pytest.raises(AuthenticationError) as exc_info:
        await service.validate_registration_token(raw_token)
    assert "expired" in str(exc_info.value.message).lower()


@pytest.mark.asyncio
async def test_complete_registration_success():
    mock_db = AsyncMock()
    service = AuthService(mock_db)

    raw_token, token_hash = generate_registration_token()
    token_record = PendingRegistrationToken(
        id=uuid.uuid4(),
        email="completer@example.com",
        token_hash=token_hash,
        expires_at=datetime.now(timezone.utc) + timedelta(hours=1),
        is_used=False,
    )

    created_user = User(
        id=uuid.uuid4(),
        email="completer@example.com",
        display_name="Completer Name",
        is_verified=True,
    )

    service.auth_repo.get_pending_registration_token_by_hash = AsyncMock(return_value=token_record)
    service.auth_repo.get_user_by_email = AsyncMock(return_value=None)
    service.auth_repo.create_user = AsyncMock(return_value=created_user)
    service.auth_repo.mark_pending_registration_token_used = AsyncMock()
    service.auth_repo.create_refresh_token = AsyncMock()

    request_data = CompleteRegistrationRequest(
        token=raw_token,
        display_name="Completer Name",
        password="SecurePassword123!",
    )

    access_token, raw_refresh, user = await service.complete_registration(request_data)

    assert access_token is not None
    assert raw_refresh is not None
    assert user.email == "completer@example.com"
    assert user.is_verified is True
    assert service.auth_repo.mark_pending_registration_token_used.called
