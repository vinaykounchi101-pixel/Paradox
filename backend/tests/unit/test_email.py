import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from app.services.email_service import EmailService
from app.core.config import settings


@pytest.mark.asyncio
async def test_email_service_unconfigured():
    service = EmailService()
    service.host = None
    service.user = None
    service.password = None

    with patch.object(settings, "RESEND_API_KEY", None):
        # Should return True and log without raising an exception
        result = await service.send_password_reset_email("user@example.com", "User", "test-token-123")
        assert result is True


@pytest.mark.asyncio
async def test_email_service_send_smtp_dispatch():
    service = EmailService()
    service.host = "smtp.gmail.com"
    service.user = "test@gmail.com"
    service.password = "app-password"

    with patch.object(settings, "RESEND_API_KEY", None):
        with patch.object(service, "_send_smtp_email_sync", return_value=True) as mock_send:
            result = await service.send_password_reset_email("target@example.com", "Target User", "token-xyz")
            assert result is True
            mock_send.assert_called_once()
            call_kwargs = mock_send.call_args.kwargs
            assert call_kwargs["to_email"] == "target@example.com"
            assert "token-xyz" in call_kwargs["html_body"]
            assert "token-xyz" in call_kwargs["plain_body"]


@pytest.mark.asyncio
async def test_email_service_send_resend_dispatch():
    service = EmailService()

    with patch.object(settings, "RESEND_API_KEY", "re_test_key_12345"):
        with patch.object(settings, "EMAIL_PROVIDER", "resend"):
            with patch.object(service, "_send_resend_email_sync", return_value=True) as mock_resend:
                result = await service.send_registration_verification_email("resend_user@example.com", "token-abc-999")
                assert result is True
                mock_resend.assert_called_once()
                call_kwargs = mock_resend.call_args.kwargs
                assert call_kwargs["to_email"] == "resend_user@example.com"
                assert "token-abc-999" in call_kwargs["html_body"]


@pytest.mark.asyncio
async def test_email_service_provider_selection():
    service = EmailService()
    service.host = "smtp.gmail.com"
    service.user = "smtp_user@gmail.com"
    service.password = "smtp_pass"

    # When only SMTP is configured
    with patch.object(settings, "RESEND_API_KEY", None):
        with patch.object(settings, "EMAIL_PROVIDER", "auto"):
            assert service.active_provider == "smtp"

    # When Resend is configured, auto prioritizes Resend
    with patch.object(settings, "RESEND_API_KEY", "re_12345"):
        with patch.object(settings, "EMAIL_PROVIDER", "auto"):
            assert service.active_provider == "resend"

    # When explicitly configured as smtp
    with patch.object(settings, "RESEND_API_KEY", "re_12345"):
        with patch.object(settings, "EMAIL_PROVIDER", "smtp"):
            assert service.active_provider == "smtp"
