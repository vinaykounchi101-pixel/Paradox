import logging
from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.core.exceptions import (
    ConflictError,
    NotFoundError,
    ParadoxException,
    UnprocessableRequestError,
    ValidationError,
)

logger = logging.getLogger(__name__)


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(NotFoundError)
    async def not_found_handler(request: Request, exc: NotFoundError) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={"error": {"code": exc.code, "message": exc.message, "details": []}},
        )

    @app.exception_handler(ValidationError)
    async def validation_handler(request: Request, exc: ValidationError) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={
                "error": {
                    "code": exc.code,
                    "message": exc.message,
                    "details": exc.details,
                }
            },
        )

    @app.exception_handler(ConflictError)
    async def conflict_handler(request: Request, exc: ConflictError) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_409_CONFLICT,
            content={"error": {"code": exc.code, "message": exc.message, "details": []}},
        )

    @app.exception_handler(UnprocessableRequestError)
    async def unprocessable_handler(
        request: Request, exc: UnprocessableRequestError
    ) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={"error": {"code": exc.code, "message": exc.message, "details": []}},
        )

    @app.exception_handler(ParadoxException)
    async def paradox_exception_handler(
        request: Request, exc: ParadoxException
    ) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"error": {"code": exc.code, "message": exc.message, "details": []}},
        )

    @app.exception_handler(RequestValidationError)
    async def pydantic_validation_handler(
        request: Request, exc: RequestValidationError
    ) -> JSONResponse:
        details = []
        for error in exc.errors():
            # Get field path
            loc = error.get("loc", [])
            field = ".".join(str(x) for x in loc[1:]) if len(loc) > 1 else str(loc[0])
            details.append({"field": field, "message": error.get("msg", "")})

        message = "Validation error"
        if details:
            message = details[0]["message"]

        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={
                "error": {
                    "code": "VALIDATION_ERROR",
                    "message": message,
                    "details": details,
                }
            },
        )

    @app.exception_handler(Exception)
    async def general_exception_handler(
        request: Request, exc: Exception
    ) -> JSONResponse:
        logger.error("Unhandled server exception: %s", str(exc), exc_info=True)

        message = "Something went wrong, please try again"
        if settings.DEBUG:
            message = str(exc)

        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "error": {
                    "code": "INTERNAL_ERROR",
                    "message": message,
                    "details": [],
                }
            },
        )
