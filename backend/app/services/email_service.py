import asyncio
import logging
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from typing import Optional

from app.core.config import settings

logger = logging.getLogger(__name__)


class EmailService:
    """Service for dispatching transactional emails via SMTP (e.g. Gmail)."""

    def __init__(self) -> None:
        self.host = settings.SMTP_HOST
        self.port = settings.SMTP_PORT
        self.use_tls = settings.SMTP_TLS
        self.user = settings.SMTP_USER
        self.password = settings.SMTP_PASSWORD
        self.from_email = settings.SMTP_FROM_EMAIL or settings.SMTP_USER or "noreply@paradox.local"
        self.from_name = settings.SMTP_FROM_NAME or "Paradox Expense Tracker"

    @property
    def is_configured(self) -> bool:
        """Returns True if SMTP server and authentication credentials are set."""
        return bool(self.host and self.user and self.password)

    async def send_password_reset_email(
        self, to_email: str, display_name: str, reset_token: str
    ) -> bool:
        """
        Send a password reset email with secure token link.
        Runs asynchronously via a background thread to prevent blocking event loop.
        """
        reset_link = f"{settings.FRONTEND_URL.rstrip('/')}/reset-password?token={reset_token}"

        if not self.is_configured:
            logger.info(
                "SMTP not configured. Password reset link for %s: %s",
                to_email,
                reset_link,
            )
            return True

        subject = "Reset Your Paradox Account Password"
        plain_body = (
            f"Hello {display_name},\n\n"
            f"We received a request to reset the password for your Paradox Expense Tracker account.\n\n"
            f"Click the link below to set a new password:\n{reset_link}\n\n"
            f"This link will expire in 1 hour. If you did not request this, you can safely ignore this email.\n\n"
            f"— The Paradox Team"
        )

        html_body = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Reset Your Password</title>
  <style>
    body {{
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      background-color: #09090b;
      color: #fafafa;
      margin: 0;
      padding: 24px;
    }}
    .container {{
      max-width: 520px;
      margin: 0 auto;
      background: #18181b;
      border: 1px solid #27272a;
      border-radius: 16px;
      padding: 36px 32px;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.4);
    }}
    .logo {{
      display: inline-flex;
      align-items: center;
      gap: 10px;
      font-size: 20px;
      font-weight: 700;
      color: #6366f1;
      text-decoration: none;
      margin-bottom: 24px;
    }}
    h1 {{
      font-size: 22px;
      font-weight: 600;
      color: #ffffff;
      margin: 0 0 12px 0;
    }}
    p {{
      font-size: 14px;
      line-height: 1.6;
      color: #a1a1aa;
      margin: 0 0 20px 0;
    }}
    .btn-container {{
      text-align: center;
      margin: 32px 0;
    }}
    .btn {{
      display: inline-block;
      background-color: #6366f1;
      color: #ffffff !important;
      text-decoration: none;
      font-size: 15px;
      font-weight: 600;
      padding: 12px 32px;
      border-radius: 12px;
      box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
    }}
    .footer {{
      margin-top: 32px;
      padding-top: 20px;
      border-top: 1px solid #27272a;
      font-size: 12px;
      color: #71717a;
      text-align: center;
    }}
    .link-fallback {{
      word-break: break-all;
      color: #818cf8;
      font-size: 12px;
    }}
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">
      <span>✦ Paradox</span>
    </div>
    <h1>Password Reset Request</h1>
    <p>Hello <strong>{display_name}</strong>,</p>
    <p>We received a request to reset your password for your Paradox account. Click the button below to choose a new password.</p>
    
    <div class="btn-container">
      <a href="{reset_link}" class="btn" target="_blank">Reset Password</a>
    </div>

    <p style="font-size: 13px; color: #71717a;">This link is valid for <strong>1 hour</strong>. If you did not request a password reset, you can safely ignore this email.</p>
    
    <div class="footer">
      <p style="margin-bottom: 8px;">If the button doesn't work, copy and paste this link into your browser:</p>
      <a href="{reset_link}" class="link-fallback">{reset_link}</a>
    </div>
  </div>
</body>
</html>"""

        return await asyncio.to_thread(
            self._send_smtp_email_sync,
            to_email=to_email,
            subject=subject,
            plain_body=plain_body,
            html_body=html_body,
        )

    def _send_smtp_email_sync(
        self, to_email: str, subject: str, plain_body: str, html_body: str
    ) -> bool:
        """Synchronous SMTP email delivery."""
        try:
            msg = MIMEMultipart("alternative")
            msg["Subject"] = subject
            msg["From"] = f"{self.from_name} <{self.from_email}>"
            msg["To"] = to_email

            part1 = MIMEText(plain_body, "plain", "utf-8")
            part2 = MIMEText(html_body, "html", "utf-8")

            msg.attach(part1)
            msg.attach(part2)

            # Connect via TLS or SSL based on port
            if self.port == 465:
                server = smtplib.SMTP_SSL(self.host, self.port, timeout=15)
            else:
                server = smtplib.SMTP(self.host, self.port, timeout=15)
                server.ehlo()
                if self.use_tls:
                    server.starttls()
                    server.ehlo()

            # Clean and authenticate (remove spaces from 16-char app passwords)
            clean_user = self.user.strip().strip('"').strip("'")
            clean_pass = self.password.replace(" ", "").strip().strip('"').strip("'")
            server.login(clean_user, clean_pass)
            server.sendmail(self.from_email, [to_email], msg.as_string())
            server.quit()

            logger.info("Password reset email successfully sent to %s via SMTP", to_email)
            return True
        except Exception as exc:
            logger.error("Failed to send email to %s via SMTP: %s", to_email, exc, exc_info=True)
            return False


email_service = EmailService()
