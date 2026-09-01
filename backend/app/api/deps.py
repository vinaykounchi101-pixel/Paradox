import uuid
from typing import Optional

from fastapi import Depends, Header, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import AuthenticationError
from app.core.security import decode_access_token
from app.db.models.user import User
from app.db.session import get_db
from app.repositories.auth_repository import AuthRepository

# HTTPBearer scheme for OpenAPI documentation
security_scheme = HTTPBearer(auto_error=False)


async def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    """
    Authenticate request via JWT Bearer token.
    Extracts token, decodes claims, verifies user existence in database,
    and returns authenticated User object.
    Raises AuthenticationError (401) if token is missing, invalid, or expired.
    """
    if not credentials or not credentials.credentials:
        raise AuthenticationError("Not authenticated. Please provide a Bearer access token.")

    token = credentials.credentials
    payload = decode_access_token(token)
    if not payload:
        raise AuthenticationError("Invalid or expired access token.")

    user_id_str = payload.get("sub")
    if not user_id_str:
        raise AuthenticationError("Invalid token payload.")

    try:
        user_id = uuid.UUID(user_id_str)
    except ValueError:
        raise AuthenticationError("Invalid token subject identifier.")

    auth_repo = AuthRepository(db)
    user = await auth_repo.get_user_by_id(user_id)
    if not user:
        raise AuthenticationError("User account not found or has been removed.")

    return user


async def get_current_user_optional(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_scheme),
    db: AsyncSession = Depends(get_db),
) -> Optional[User]:
    """Optional auth dependency for endpoints that support both anonymous and authenticated usage."""
    if not credentials or not credentials.credentials:
        return None

    try:
        return await get_current_user(credentials, db)
    except AuthenticationError:
        return None


__all__ = ["get_db", "get_current_user", "get_current_user_optional"]
