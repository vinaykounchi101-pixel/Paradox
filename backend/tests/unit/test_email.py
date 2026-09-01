import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from app.services.email_service import EmailService


@pytest.mark.asyncio
async def test_email_service_unconfigured():
    service = EmailService()
    service.host = None
    service.user = None
    service.password = None

    # Should return True and log without raising an exception
    result = await service.send_password_reset_email("user@example.com", "User", "test-token-123")
    assert result is True


@pytest.mark.asyncio
async def test_email_service_send_smtp_dispatch():
    service = EmailService()
    service.host = "smtp.gmail.com"
    service.user = "test@gmail.com"
    service.password = "app-password"

    with patch.object(service, "_send_smtp_email_sync", return_value=True) as mock_send:
        result = await service.send_password_reset_email("target@example.com", "Target User", "token-xyz")
        assert result is True
        mock_send.assert_called_once()
        call_kwargs = mock_send.call_args.kwargs
        assert call_kwargs["to_email"] == "target@example.com"
        assert "token-xyz" in call_kwargs["html_body"]
        assert "token-xyz" in call_kwargs["plain_body"]
