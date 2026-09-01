from fastapi import APIRouter

from app.api import auth, budget, categories, dashboard, expenses, health, payment_methods

api_router = APIRouter()

# Register sub-routers
api_router.include_router(health.router, tags=["health"])
api_router.include_router(auth.router, prefix="/auth", tags=["auth"])
api_router.include_router(expenses.router, prefix="/expenses", tags=["expenses"])
api_router.include_router(categories.router, prefix="/categories", tags=["categories"])
api_router.include_router(payment_methods.router, prefix="/payment-methods", tags=["payment-methods"])
api_router.include_router(budget.router, prefix="/budget", tags=["budget"])
api_router.include_router(dashboard.router, prefix="/dashboard", tags=["dashboard"])
