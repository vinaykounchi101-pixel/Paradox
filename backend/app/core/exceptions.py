from typing import Any, Dict, List, Optional


class ParadoxException(Exception):
    """Base exception for all Paradox domain errors."""

    def __init__(self, message: str, code: str = "INTERNAL_ERROR"):
        super().__init__(message)
        self.message = message
        self.code = code


class NotFoundError(ParadoxException):
    """Raised when a requested resource is not found."""

    def __init__(self, message: str):
        super().__init__(message, code="NOT_FOUND")


class ValidationError(ParadoxException):
    """Raised when request data violates business validation rules."""

    def __init__(self, message: str, details: Optional[List[Dict[str, Any]]] = None):
        super().__init__(message, code="VALIDATION_ERROR")
        self.details = details or []


class ConflictError(ParadoxException):
    """Raised when a mutation conflicts with the current database state."""

    def __init__(self, message: str):
        super().__init__(message, code="CONFLICT")


class AuthenticationError(ParadoxException):
    """Raised when authentication fails (missing, invalid, or expired credentials/token)."""

    def __init__(self, message: str = "Could not validate credentials"):
        super().__init__(message, code="UNAUTHORIZED")


class AuthorizationError(ParadoxException):
    """Raised when an authenticated user does not have permission to access a resource."""

    def __init__(self, message: str = "Access forbidden"):
        super().__init__(message, code="FORBIDDEN")


class RateLimitError(ParadoxException):
    """Raised when too many requests are sent to sensitive endpoints."""

    def __init__(self, message: str = "Too many requests. Please try again later."):
        super().__init__(message, code="RATE_LIMITED")


class UnprocessableRequestError(ParadoxException):
    """Raised when a request is malformed or contains contradictory parameters."""

    def __init__(self, message: str):
        super().__init__(message, code="INVALID_REQUEST")
