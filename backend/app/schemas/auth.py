import uuid
from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, EmailStr, Field


class UserRegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=8, description="Password must be at least 8 characters long")
    display_name: Optional[str] = Field(default="User", max_length=100)


class InitiateRegistrationRequest(BaseModel):
    email: EmailStr


class ValidateRegistrationTokenResponse(BaseModel):
    email: EmailStr
    valid: bool = True


class CompleteRegistrationRequest(BaseModel):
    token: str = Field(..., description="Verification token received via email link")
    display_name: str = Field(..., min_length=1, max_length=100, description="User display name")
    password: str = Field(..., min_length=8, description="Password must be at least 8 characters long")


class UserLoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=1)


class GoogleLoginRequest(BaseModel):
    id_token: str = Field(..., description="Google OAuth 2.0 / OpenID Connect ID Token")


class UserResponse(BaseModel):
    id: uuid.UUID
    email: EmailStr
    display_name: str
    avatar_url: Optional[str] = None
    is_verified: bool
    currency: str = "INR"
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class UpdateProfileRequest(BaseModel):
    display_name: Optional[str] = Field(None, min_length=1, max_length=100)
    currency: Optional[str] = Field(None, min_length=3, max_length=10)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse


class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class ResetPasswordRequest(BaseModel):
    token: str = Field(..., description="Password reset token received via link")
    new_password: str = Field(..., min_length=8, description="New password must be at least 8 characters long")


class ChangePasswordRequest(BaseModel):
    current_password: str = Field(..., min_length=1)
    new_password: str = Field(..., min_length=8, description="New password must be at least 8 characters long")


class MessageResponse(BaseModel):
    message: str
    success: bool = True
