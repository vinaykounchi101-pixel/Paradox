from typing import List, Union
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=(".env", ".env.local", "../.env", "../.env.local"),
        env_file_encoding="utf-8",
        env_ignore_empty=True,
        case_sensitive=False,
        extra="ignore",
    )

    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/paradox"
    CORS_ALLOWED_ORIGINS: Union[str, List[str]] = ["http://localhost:3000"]
    APP_ENV: str = "local"
    DEBUG: bool = True
    API_V1_PREFIX: str = "/api/v1"
    APP_NAME: str = "Paradox"
    LOG_LEVEL: str = "INFO"
    APP_TIMEZONE: str = "UTC"

    # Security & JWT Configuration
    JWT_SECRET_KEY: str = "paradox-jwt-secret-key-change-in-production-min-32-chars"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    # Google OAuth 2.0 / OpenID Connect
    GOOGLE_CLIENT_ID: Union[str, None] = None
    GOOGLE_CLIENT_SECRET: Union[str, None] = None

    # Application URLs
    FRONTEND_URL: str = "http://localhost:3000"

    # Email Configuration (Supports Brevo API, Resend API, and SMTP)
    EMAIL_PROVIDER: str = "auto"  # "auto", "brevo", "resend", or "smtp"

    # Brevo (Sendinblue) API Configuration (Recommended for Production without custom domain)
    BREVO_API_KEY: Union[str, None] = None
    BREVO_SENDER_EMAIL: Union[str, None] = None
    BREVO_SENDER_NAME: str = "Paradox Expense Tracker"

    # Resend API Configuration
    RESEND_API_KEY: Union[str, None] = None
    RESEND_FROM_EMAIL: str = "Paradox <onboarding@resend.dev>"

    # SMTP Email Configuration (Fallback / Local Development / Brevo SMTP Relay)
    SMTP_HOST: Union[str, None] = None
    SMTP_PORT: int = 587
    SMTP_TLS: bool = True
    SMTP_USER: Union[str, None] = None
    SMTP_PASSWORD: Union[str, None] = None
    SMTP_FROM_EMAIL: Union[str, None] = None
    SMTP_FROM_NAME: str = "Paradox Expense Tracker"

    @field_validator("DATABASE_URL", mode="before")
    @classmethod
    def assemble_db_url(cls, v: str) -> str:
        if v:
            if v.startswith("postgresql://"):
                return v.replace("postgresql://", "postgresql+asyncpg://", 1)
            elif v.startswith("postgres://"):
                return v.replace("postgres://", "postgresql+asyncpg://", 1)
        return v

    @field_validator("CORS_ALLOWED_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        if isinstance(v, str):
            return [i.strip() for i in v.split(",") if i.strip()]
        return v


settings = Settings()
