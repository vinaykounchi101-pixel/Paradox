from decimal import Decimal
from typing import List, Optional
from pydantic import BaseModel, Field


class CategorizeRequest(BaseModel):
    description: str = Field(..., min_length=1, max_length=500, description="Description or note of the expense")
    available_categories: Optional[List[str]] = Field(
        default=None,
        description="List of available category names to choose from. If omitted, default starter categories are used.",
    )


class CategorizeResponse(BaseModel):
    category_name: str = Field(..., description="Recommended category name")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence score between 0.0 and 1.0")
    reasoning: Optional[str] = Field(default=None, description="Brief explanation for the recommendation")
    provider_used: str = Field(..., description="AI provider used (gemini, openai, anthropic, or heuristic)")


class ParseExpenseRequest(BaseModel):
    text: str = Field(
        ...,
        min_length=2,
        max_length=500,
        description="Freeform natural language description of the expense (e.g. 'Paid 450 for auto rickshaw via UPI yesterday')",
    )
    available_categories: Optional[List[str]] = Field(
        default=None,
        description="List of available category names for matching",
    )
    available_payment_methods: Optional[List[str]] = Field(
        default=None,
        description="List of available payment method names for matching",
    )


class ParseExpenseResponse(BaseModel):
    amount: Optional[Decimal] = Field(default=None, description="Extracted expense monetary amount")
    category_name: Optional[str] = Field(default=None, description="Matched category name")
    payment_method_name: Optional[str] = Field(default=None, description="Matched payment method name")
    date: Optional[str] = Field(default=None, description="Extracted date in YYYY-MM-DD format")
    description: Optional[str] = Field(default=None, description="Cleaned description or note")
    confidence: float = Field(default=0.5, ge=0.0, le=1.0, description="Confidence score")
    reasoning: Optional[str] = Field(default=None, description="Brief explanation")
    provider_used: str = Field(..., description="AI provider used")
