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


class AIInsightsResponse(BaseModel):
    health_status: str = Field(..., description="'healthy', 'cautious', or 'critical'")
    headline: str = Field(..., description="Concise summary headline of financial health")
    alerts: List[str] = Field(default_factory=list, description="List of urgent spending warnings or milestones")
    saving_tips: List[str] = Field(default_factory=list, description="Personalized, actionable savings recommendations")
    projected_spend: Optional[Decimal] = Field(default=None, description="Projected spend by period end based on run rate")
    daily_burn_rate: Optional[Decimal] = Field(default=None, description="Current average daily burn rate")
    confidence: float = Field(default=0.85, ge=0.0, le=1.0, description="Confidence score")
    provider_used: str = Field(..., description="AI provider used (gemini, openai, anthropic, or heuristic)")


class CategoryAllocation(BaseModel):
    category_name: str = Field(..., description="Category name")
    percentage: float = Field(..., description="Recommended percentage of budget allocated to this category")
    suggested_amount: Decimal = Field(..., description="Recommended amount allocated to this category")


class SuggestBudgetResponse(BaseModel):
    period_type: str = Field(..., description="Granularity period ('month', 'week', or 'day')")
    suggested_amount: Decimal = Field(..., description="Recommended budget limit")
    reasoning: str = Field(..., description="Reasoning based on past spending history and variance")
    category_allocations: List[CategoryAllocation] = Field(
        default_factory=list, description="Suggested breakdown by category"
    )
    confidence: float = Field(default=0.85, ge=0.0, le=1.0, description="Confidence score")
    provider_used: str = Field(..., description="AI provider used")


class ParsedReceiptItem(BaseModel):
    item_name: str = Field(..., description="Item or service name")
    amount: Decimal = Field(..., description="Item amount")
    category_suggestion: Optional[str] = Field(default=None, description="Suggested category for this line item")


class ParseReceiptRequest(BaseModel):
    text: str = Field(..., min_length=2, max_length=2000, description="Raw receipt text, SMS, or bill content")


class ParseReceiptResponse(BaseModel):
    merchant_name: Optional[str] = Field(default=None, description="Extracted store/merchant name")
    total_amount: Optional[Decimal] = Field(default=None, description="Total monetary amount")
    date: Optional[str] = Field(default=None, description="Transaction date in YYYY-MM-DD format")
    category_name: Optional[str] = Field(default=None, description="Primary category suggestion")
    payment_method_name: Optional[str] = Field(default=None, description="Extracted payment method")
    items: List[ParsedReceiptItem] = Field(default_factory=list, description="Itemized receipt line items")
    provider_used: str = Field(..., description="AI provider used")
