import hashlib
import logging
import secrets
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional, Tuple

import bcrypt
import httpx
import jwt

from app.core.config import settings

logger = logging.getLogger(__name__)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a plaintext password against a bcrypt hashed password string."""
    try:
        return bcrypt.checkpw(
            plain_password.encode("utf-8"),
            hashed_password.encode("utf-8"),
        )
    except Exception as exc:
        logger.debug("Password verification error: %s", exc)
        return False


def get_password_hash(password: str) -> str:
    """Generate a bcrypt hash of a plaintext password."""
    # Truncate to 72 bytes if needed per standard bcrypt specification
    pw_bytes = password.encode("utf-8")[:72]
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(pw_bytes, salt).decode("utf-8")


def create_access_token(
    subject: str,
    email: str,
    expires_delta: Optional[timedelta] = None,
    extra_claims: Optional[Dict[str, Any]] = None,
) -> str:
    """Create a signed JWT access token."""
    now = datetime.now(timezone.utc)
    if expires_delta:
        expire = now + expires_delta
    else:
        expire = now + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)

    payload: Dict[str, Any] = {
        "sub": subject,
        "email": email,
        "exp": int(expire.timestamp()),
        "iat": int(now.timestamp()),
        "type": "access",
    }
    if extra_claims:
        payload.update(extra_claims)

    encoded_jwt = jwt.encode(
        payload,
        settings.JWT_SECRET_KEY,
        algorithm=settings.JWT_ALGORITHM,
    )
    return encoded_jwt


def decode_access_token(token: str) -> Optional[Dict[str, Any]]:
    """Decode and validate a JWT access token. Returns payload dict or None."""
    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM],
        )
        if payload.get("type") != "access":
            return None
        return payload
    except (jwt.PyJWTError, Exception) as exc:
        logger.debug("Failed to decode JWT access token: %s", exc)
        return None


def hash_token(raw_token: str) -> str:
    """Compute a SHA-256 cryptographic hash of an opaque token string."""
    return hashlib.sha256(raw_token.encode("utf-8")).hexdigest()


def generate_refresh_token() -> Tuple[str, str]:
    """
    Generate an opaque random refresh token.
    Returns:
        (raw_token, token_hash)
    """
    raw_token = secrets.token_urlsafe(48)
    token_hash = hash_token(raw_token)
    return raw_token, token_hash


def generate_password_reset_token() -> Tuple[str, str]:
    """
    Generate a secure password reset token.
    Returns:
        (raw_token, token_hash)
    """
    raw_token = secrets.token_urlsafe(32)
    token_hash = hash_token(raw_token)
    return raw_token, token_hash


def generate_registration_token() -> Tuple[str, str]:
    """
    Generate a secure pre-registration verification token.
    Returns:
        (raw_token, token_hash)
    """
    raw_token = secrets.token_urlsafe(32)
    token_hash = hash_token(raw_token)
    return raw_token, token_hash


def verify_google_id_token(
    id_token_str: str, client_id: Optional[str] = None
) -> Optional[Dict[str, Any]]:
    """
    Verify Google OAuth 2.0 / OpenID Connect ID token.
    Uses google-auth library with automatic Google TokenInfo fallback.
    Returns the verified token payload dict if valid, otherwise None.
    """
    if not id_token_str or not isinstance(id_token_str, str):
        return None

    raw_client_id = client_id or settings.GOOGLE_CLIENT_ID
    target_client_id = (
        raw_client_id.strip().strip('"').strip("'")
        if raw_client_id and not raw_client_id.startswith("your-google-client-id")
        else None
    )

    payload: Optional[Dict[str, Any]] = None

    # Method 1: Local cryptographic verification using google-auth JWKS
    try:
        from google.auth.transport import requests as google_requests
        from google.oauth2 import id_token as google_id_token

        request = google_requests.Request()
        payload = google_id_token.verify_oauth2_token(
            id_token_str, request, audience=target_client_id
        )
    except Exception as exc:
        logger.debug("Local google-auth JWKS verification note: %s. Trying Google TokenInfo...", exc)

    # Method 2: Direct Google TokenInfo endpoint verification fallback
    if not payload:
        try:
            with httpx.Client(timeout=10.0) as http_client:
                resp = http_client.get(
                    "https://oauth2.googleapis.com/tokeninfo",
                    params={"id_token": id_token_str},
                )
                if resp.status_code == 200:
                    data = resp.json()
                    # Verify issuer
                    issuer = data.get("iss", "")
                    if issuer in ["accounts.google.com", "https://accounts.google.com"]:
                        payload = data
                else:
                    logger.warning("Google TokenInfo validation failed with status %d: %s", resp.status_code, resp.text)
        except Exception as exc:
            logger.warning("Google TokenInfo endpoint request error: %s", exc)

    if not payload:
        logger.warning("Google ID token could not be verified by either JWKS or TokenInfo.")
        return None

    # Validate Issuer
    issuer = payload.get("iss", "")
    if issuer not in ["accounts.google.com", "https://accounts.google.com"]:
        logger.warning("Invalid Google ID token issuer: %s", issuer)
        return None

    # Validate Audience if target client ID is configured
    token_aud = payload.get("aud")
    if target_client_id and token_aud and token_aud != target_client_id:
        logger.warning(
            "Google token audience mismatch. Token aud: %s, Configured GOOGLE_CLIENT_ID: %s",
            token_aud,
            target_client_id,
        )
        return None

    return payload
