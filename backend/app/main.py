import asyncio
import logging
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware

from app.api.router import api_router
from app.core.config import settings
from app.core.error_handlers import register_error_handlers
from app.core.logging import setup_logging

# 1. Initialize logging configuration
setup_logging()
logger = logging.getLogger("app.startup")


def _run_migrations() -> None:
    """Run Alembic migrations on startup.

    Derives a synchronous psycopg2-compatible URL from the async DATABASE_URL
    so that Alembic's standard sync runner works correctly.
    """
    from alembic import command
    from alembic.config import Config

    # Derive sync URL: strip +asyncpg driver suffix for Alembic's sync engine
    sync_url = (
        settings.DATABASE_URL
        .replace("postgresql+asyncpg://", "postgresql://", 1)
    )

    alembic_cfg = Config("alembic.ini")
    alembic_cfg.set_main_option("sqlalchemy.url", sync_url)

    logger.info("Running Alembic migrations...")
    try:
        command.upgrade(alembic_cfg, "head")
        logger.info("Alembic migrations completed successfully.")
    except Exception as exc:
        logger.error("Alembic migration failed: %s", exc, exc_info=True)
        raise


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan: run migrations before accepting requests.

    _run_migrations is dispatched to a thread pool so that Alembic's
    internal asyncio.run() call gets a clean event loop (not the
    already-running uvicorn loop).
    """
    await asyncio.to_thread(_run_migrations)
    yield


# 2. Instantiate FastAPI Application
app = FastAPI(
    title=settings.APP_NAME,
    description="Personal Expense Tracker Backend - Phase 1 MVP",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# 3. Add CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 4. Request Logging Middleware
class RequestLoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        start_time = time.perf_counter()

        # Process the request
        try:
            response = await call_next(request)
        except Exception as e:
            # General exception handler will catch this, but we log the duration here as well
            duration = (time.perf_counter() - start_time) * 1000
            logger.error(
                "Request failed: %s %s - Duration: %.2fms - Error: %s",
                request.method,
                request.url.path,
                duration,
                str(e),
            )
            raise e

        duration = (time.perf_counter() - start_time) * 1000

        # Log request summary at INFO level (or WARNING/ERROR for 4xx/5xx)
        log_msg = f"Request: {request.method} {request.url.path} - Status: {response.status_code} - Duration: {duration:.2f}ms"
        if response.status_code >= 500:
            logger.error(log_msg)
        elif response.status_code >= 400:
            logger.warning(log_msg)
        else:
            logger.info(log_msg)

        return response


# Re-init request logger after setup (startup logger used above)
request_logger = logging.getLogger("app.request")

app.add_middleware(RequestLoggingMiddleware)

# 5. Register custom error handlers
register_error_handlers(app)

# 6. Mount API Router under prefix
app.include_router(api_router, prefix=settings.API_V1_PREFIX)
