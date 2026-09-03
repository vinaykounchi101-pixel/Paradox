import logging
from typing import Optional

from fastapi import APIRouter, Cookie, Depends, Header, Request, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.core.config import settings
from app.db.models.user import User
from app.schemas.auth import (
    ChangePasswordRequest,
    CompleteRegistrationRequest,
    ForgotPasswordRequest,
    GoogleLoginRequest,
    InitiateRegistrationRequest,
    MessageResponse,
    RegistrationStatusResponse,
    ResetPasswordRequest,
    SwitchAccountRequest,
    TokenResponse,
    UpdateProfileRequest,
    UserLoginRequest,
    UserRegisterRequest,
    UserResponse,
    ValidateRegistrationTokenResponse,
    VerifyOtpRegisterRequest,
)
from app.services.auth_service import AuthService

logger = logging.getLogger(__name__)

router = APIRouter()

REFRESH_COOKIE_NAME = "paradox_refresh_token"
REFRESH_COOKIE_PATH = f"{settings.API_V1_PREFIX}/auth"


def set_refresh_cookie(response: Response, raw_refresh_token: str) -> None:
    """Helper to set secure, HttpOnly, SameSite=Lax refresh token cookie."""
    max_age = settings.REFRESH_TOKEN_EXPIRE_DAYS * 24 * 60 * 60
    # In production, secure=True. In local dev, secure=False if using plain HTTP.
    is_secure = settings.APP_ENV == "production"
    response.set_cookie(
        key=REFRESH_COOKIE_NAME,
        value=raw_refresh_token,
        max_age=max_age,
        expires=max_age,
        path=REFRESH_COOKIE_PATH,
        httponly=True,
        secure=is_secure,
        samesite="lax",
    )


def clear_refresh_cookie(response: Response) -> None:
    """Helper to delete refresh token cookie."""
    response.delete_cookie(
        key=REFRESH_COOKIE_NAME,
        path=REFRESH_COOKIE_PATH,
        httponly=True,
        samesite="lax",
    )


@router.post(
    "/register/initiate",
    response_model=MessageResponse,
    status_code=status.HTTP_200_OK,
    summary="Initiate pre-registration email verification",
)
async def initiate_registration(
    data: InitiateRegistrationRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    service = AuthService(db)
    email = await service.initiate_registration(data.email)
    return MessageResponse(
        message=f"Verification link has been sent to {email}. Please check your inbox to complete registration.",
        success=True,
    )


@router.get(
    "/register/validate-token",
    response_model=ValidateRegistrationTokenResponse,
    status_code=status.HTTP_200_OK,
    summary="Validate pre-registration verification token",
)
async def validate_registration_token(
    token: str,
    db: AsyncSession = Depends(get_db),
) -> ValidateRegistrationTokenResponse:
    service = AuthService(db)
    email = await service.validate_registration_token(token)
    return ValidateRegistrationTokenResponse(email=email, valid=True)


@router.post(
    "/register/complete",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Complete registration after email verification",
)
async def complete_registration(
    data: CompleteRegistrationRequest,
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
) -> TokenResponse:
    user_agent = request.headers.get("user-agent")
    service = AuthService(db)
    access_token, raw_refresh, user = await service.complete_registration(
        data, user_agent=user_agent
    )
    set_refresh_cookie(response, raw_refresh)
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        refresh_token=raw_refresh,
    )


@router.post(
    "/register/verify-otp",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Complete registration instantly using 6-digit OTP code",
)
async def verify_otp_and_register(
    data: VerifyOtpRegisterRequest,
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
) -> TokenResponse:
    user_agent = request.headers.get("user-agent")
    service = AuthService(db)
    access_token, raw_refresh, user = await service.verify_otp_and_register(
        data, user_agent=user_agent
    )
    set_refresh_cookie(response, raw_refresh)
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        refresh_token=raw_refresh,
    )


@router.get(
    "/register/status",
    response_model=RegistrationStatusResponse,
    status_code=status.HTTP_200_OK,
    summary="Check real-time registration verification status for polling laptop/desktop clients",
)
async def check_registration_status(
    email: str,
    db: AsyncSession = Depends(get_db),
) -> RegistrationStatusResponse:
    service = AuthService(db)
    result = await service.check_registration_status(email)
    return RegistrationStatusResponse(**result)


@router.post(
    "/register",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Register a new user",
)
async def register(
    data: UserRegisterRequest,
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
) -> TokenResponse:
    user_agent = request.headers.get("user-agent")
    service = AuthService(db)
    access_token, raw_refresh, user = await service.register(data, user_agent=user_agent)
    set_refresh_cookie(response, raw_refresh)
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        refresh_token=raw_refresh,
    )


@router.post(
    "/login",
    response_model=TokenResponse,
    status_code=status.HTTP_200_OK,
    summary="Authenticate with email and password",
)
async def login(
    data: UserLoginRequest,
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
) -> TokenResponse:
    user_agent = request.headers.get("user-agent")
    service = AuthService(db)
    access_token, raw_refresh, user = await service.login(data, user_agent=user_agent)
    set_refresh_cookie(response, raw_refresh)
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        refresh_token=raw_refresh,
    )


@router.post(
    "/google",
    response_model=TokenResponse,
    status_code=status.HTTP_200_OK,
    summary="Sign in or register with Google OAuth 2.0 / OpenID Connect",
)
async def google_login(
    data: GoogleLoginRequest,
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
) -> TokenResponse:
    user_agent = request.headers.get("user-agent")
    service = AuthService(db)
    access_token, raw_refresh, user = await service.login_with_google(data, user_agent=user_agent)
    set_refresh_cookie(response, raw_refresh)
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        refresh_token=raw_refresh,
    )


@router.post(
    "/refresh",
    response_model=TokenResponse,
    status_code=status.HTTP_200_OK,
    summary="Rotate refresh token and issue new access token",
)
async def refresh_token(
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
    paradox_refresh_token: Optional[str] = Cookie(default=None, alias=REFRESH_COOKIE_NAME),
) -> TokenResponse:
    user_agent = request.headers.get("user-agent")
    service = AuthService(db)
    
    # Fallback to header or body if cookie is not directly present
    token_str = paradox_refresh_token or request.headers.get("x-refresh-token") or ""
    access_token, new_raw_refresh, user = await service.refresh(token_str, user_agent=user_agent)
    set_refresh_cookie(response, new_raw_refresh)
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        refresh_token=new_raw_refresh,
    )


@router.post(
    "/switch-account",
    response_model=TokenResponse,
    status_code=status.HTTP_200_OK,
    summary="Switch active account using a saved refresh token",
)
async def switch_account(
    data: SwitchAccountRequest,
    request: Request,
    response: Response,
    db: AsyncSession = Depends(get_db),
) -> TokenResponse:
    user_agent = request.headers.get("user-agent")
    service = AuthService(db)
    access_token, new_raw_refresh, user = await service.refresh(data.refresh_token, user_agent=user_agent)
    set_refresh_cookie(response, new_raw_refresh)
    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        refresh_token=new_raw_refresh,
    )


@router.post(
    "/logout",
    response_model=MessageResponse,
    status_code=status.HTTP_200_OK,
    summary="Log out and revoke current session refresh token",
)
async def logout(
    response: Response,
    db: AsyncSession = Depends(get_db),
    paradox_refresh_token: Optional[str] = Cookie(default=None, alias=REFRESH_COOKIE_NAME),
) -> MessageResponse:
    service = AuthService(db)
    await service.logout(paradox_refresh_token)
    clear_refresh_cookie(response)
    return MessageResponse(message="Successfully logged out.")


@router.post(
    "/logout-all",
    response_model=MessageResponse,
    status_code=status.HTTP_200_OK,
    summary="Log out from all devices and revoke all user refresh tokens",
)
async def logout_all(
    response: Response,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    service = AuthService(db)
    await service.logout_all(current_user.id)
    clear_refresh_cookie(response)
    return MessageResponse(message="Successfully logged out from all devices.")


@router.get(
    "/me",
    response_model=UserResponse,
    status_code=status.HTTP_200_OK,
    summary="Get current authenticated user profile",
)
async def get_me(
    current_user: User = Depends(get_current_user),
) -> UserResponse:
    return UserResponse.model_validate(current_user)


@router.patch(
    "/me",
    response_model=UserResponse,
    status_code=status.HTTP_200_OK,
    summary="Update current authenticated user profile preferences (currency, display name)",
)
async def update_me(
    data: UpdateProfileRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> UserResponse:
    if data.display_name is not None:
        current_user.display_name = data.display_name.strip()
    if data.currency is not None:
        current_user.currency = data.currency.strip().upper()
    db.add(current_user)
    await db.flush()
    await db.refresh(current_user)
    return UserResponse.model_validate(current_user)


@router.post(
    "/forgot-password",
    response_model=MessageResponse,
    status_code=status.HTTP_200_OK,
    summary="Request a password reset link",
)
async def forgot_password(
    data: ForgotPasswordRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    service = AuthService(db)
    await service.forgot_password(data.email)
    return MessageResponse(
        message="If an account with that email exists, a password reset link has been sent."
    )


@router.post(
    "/reset-password",
    response_model=MessageResponse,
    status_code=status.HTTP_200_OK,
    summary="Reset password using a valid reset token",
)
async def reset_password(
    data: ResetPasswordRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    service = AuthService(db)
    await service.reset_password(data)
    return MessageResponse(message="Password has been successfully reset. Please log in with your new password.")


@router.post(
    "/change-password",
    response_model=MessageResponse,
    status_code=status.HTTP_200_OK,
    summary="Change password for current authenticated user",
)
async def change_password(
    data: ChangePasswordRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    service = AuthService(db)
    await service.change_password(current_user.id, data)
    return MessageResponse(message="Password has been successfully updated.")
