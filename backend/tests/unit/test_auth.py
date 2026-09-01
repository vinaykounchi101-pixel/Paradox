import uuid
from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.core.exceptions import AuthenticationError, ConflictError
from app.core.security import (
    create_access_token,
    decode_access_token,
    generate_password_reset_token,
    generate_refresh_token,
    get_password_hash,
    hash_token,
    verify_password,
)
from app.db.models.refresh_token import RefreshToken
from app.db.models.user import User
from app.schemas.auth import (
    ChangePasswordRequest,
    ResetPasswordRequest,
    UserLoginRequest,
    UserRegisterRequest,
)
from app.services.auth_service import AuthService


def test_password_hashing():
    password = "SuperSecretPassword123!"
    hashed = get_password_hash(password)
    assert hashed != password
    assert verify_password(password, hashed) is True
    assert verify_password("WrongPassword", hashed) is False


def test_jwt_access_token():
    user_id = str(uuid.uuid4())
    email = "test@paradox.local"
    token = create_access_token(subject=user_id, email=email)
    assert isinstance(token, str)

    payload = decode_access_token(token)
    assert payload is not None
    assert payload["sub"] == user_id
    assert payload["email"] == email
    assert payload["type"] == "access"

    # Expired token test
    expired_token = create_access_token(
        subject=user_id,
        email=email,
        expires_delta=timedelta(seconds=-10),
    )
    assert decode_access_token(expired_token) is None


def test_refresh_token_generation():
    raw_token, token_hash = generate_refresh_token()
    assert isinstance(raw_token, str)
    assert isinstance(token_hash, str)
    assert hash_token(raw_token) == token_hash
    assert len(raw_token) >= 32
    assert len(token_hash) == 64  # SHA-256 hex string


@pytest.mark.asyncio
async def test_auth_service_register_success():
    mock_db = MagicMock()
    service = AuthService(mock_db)

    # Mock repository methods
    service.auth_repo.get_user_by_email = AsyncMock(return_value=None)
    mock_user = User(
        id=uuid.uuid4(),
        email="newuser@example.com",
        display_name="New User",
        password_hash="hashed_pw",
        is_verified=False,
    )
    service.auth_repo.create_user = AsyncMock(return_value=mock_user)
    service.auth_repo.create_refresh_token = AsyncMock()

    reg_data = UserRegisterRequest(
        email="newuser@example.com",
        password="ValidPassword123",
        display_name="New User",
    )
    access_token, raw_refresh, user = await service.register(reg_data)

    assert user.email == "newuser@example.com"
    assert access_token is not None
    assert raw_refresh is not None
    assert service.auth_repo.create_user.called
    assert service.auth_repo.create_refresh_token.called


@pytest.mark.asyncio
async def test_auth_service_register_duplicate_email():
    mock_db = MagicMock()
    service = AuthService(mock_db)

    existing_user = User(
        id=uuid.uuid4(),
        email="existing@example.com",
        display_name="Existing User",
    )
    service.auth_repo.get_user_by_email = AsyncMock(return_value=existing_user)

    reg_data = UserRegisterRequest(
        email="existing@example.com",
        password="ValidPassword123",
    )
    with pytest.raises(ConflictError):
        await service.register(reg_data)


@pytest.mark.asyncio
async def test_auth_service_login_invalid_password():
    mock_db = MagicMock()
    service = AuthService(mock_db)

    user = User(
        id=uuid.uuid4(),
        email="user@example.com",
        password_hash=get_password_hash("CorrectPassword123"),
    )
    service.auth_repo.get_user_by_email = AsyncMock(return_value=user)

    login_data = UserLoginRequest(
        email="user@example.com",
        password="WrongPassword123",
    )
    with pytest.raises(AuthenticationError):
        await service.login(login_data)


@pytest.mark.asyncio
async def test_auth_service_refresh_token_rotation():
    mock_db = MagicMock()
    service = AuthService(mock_db)

    user_id = uuid.uuid4()
    raw_token = "valid_raw_refresh_token_string"
    token_hash = hash_token(raw_token)

    mock_refresh_record = RefreshToken(
        id=uuid.uuid4(),
        user_id=user_id,
        token_hash=token_hash,
        expires_at=datetime.now(timezone.utc) + timedelta(days=7),
        is_revoked=False,
    )
    service.auth_repo.get_refresh_token_by_hash = AsyncMock(return_value=mock_refresh_record)
    service.auth_repo.revoke_refresh_token = AsyncMock()
    mock_user = User(id=user_id, email="user@example.com", display_name="User")
    service.auth_repo.get_user_by_id = AsyncMock(return_value=mock_user)
    service.auth_repo.create_refresh_token = AsyncMock()

    new_access, new_refresh, user = await service.refresh(raw_token)

    assert new_access is not None
    assert new_refresh is not None
    assert user.id == user_id
    # Old token must be revoked (rotation)
    service.auth_repo.revoke_refresh_token.assert_called_once_with(token_hash)
    # New token must be created
    assert service.auth_repo.create_refresh_token.called
