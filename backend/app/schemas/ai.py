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


class SimulatePurchaseRequest(BaseModel):
    amount: Decimal = Field(..., gt=0, description="Purchase amount to simulate")
    category_name: Optional[str] = Field(default=None, description="Category of purchase")
    description: Optional[str] = Field(default=None, description="Item or merchant description")


class SimulatePurchaseResponse(BaseModel):
    verdict: str = Field(..., description="'safe', 'caution', or 'over_budget'")
    headline: str = Field(..., description="Punchy verdict summary")
    advice: str = Field(..., description="Reasoned explanation and behavioral recommendation")
    current_remaining_budget: Decimal = Field(..., description="Current budget remaining before purchase")
    projected_remaining_budget: Decimal = Field(..., description="Budget remaining after purchase")
    safe_to_spend_daily_before: Decimal = Field(..., description="Daily safe spend before purchase")
    safe_to_spend_daily_after: Decimal = Field(..., description="Daily safe spend after purchase")
    category_impact: Optional[str] = Field(default=None, description="Category-level impact explanation")
    savings_impact: str = Field(..., description="Impact on month-end savings trajectory")
    can_proceed: bool = Field(..., description="True if safe or cautious, False if severely over-budget")


class SafeToSpendResponse(BaseModel):
    safe_daily_allowance: Decimal = Field(..., description="Recommended maximum spending allowance per day")
    current_daily_burn_rate: Decimal = Field(..., description="Actual average spending per day this period")
    remaining_budget: Decimal = Field(..., description="Unspent budget amount remaining")
    days_remaining: int = Field(..., description="Number of days remaining in the current period")
    depletion_date: Optional[str] = Field(default=None, description="Projected date budget will run out at current burn")
    status: str = Field(..., description="'optimal', 'warning', or 'danger'")
    burn_status_message: str = Field(..., description="Explanatory status message")


class HealthScorePillar(BaseModel):
    name: str = Field(..., description="Pillar name (Budget Adherence, Savings Velocity, Category Discipline)")
    score: int = Field(..., description="Earned points")
    max_score: int = Field(..., description="Maximum possible points")
    feedback: str = Field(..., description="Assessment feedback for this pillar")


class FinancialHealthScoreResponse(BaseModel):
    score: int = Field(..., ge=0, le=100, description="Composite score between 0 and 100")
    status: str = Field(..., description="'excellent', 'good', or 'needs_attention'")
    headline: str = Field(..., description="Summary headline")
    pillars: List[HealthScorePillar] = Field(default_factory=list, description="Score components breakdown")
    recommendations: List[str] = Field(default_factory=list, description="Targeted actions to boost score")


class SpendingLeakItem(BaseModel):
    merchant_or_pattern: str = Field(..., description="Merchant name or spending habit")
    frequency_per_month: int = Field(..., description="Estimated occurrences per month")
    avg_amount: Decimal = Field(..., description="Average amount per transaction")
    monthly_drain: Decimal = Field(..., description="Estimated monthly total cost")
    annualized_drain: Decimal = Field(..., description="Estimated 12-month annual drain")
    category_name: str = Field(..., description="Category tag")
    savings_tip: str = Field(..., description="Actionable recommendation to plug this leak")


class LeakAnalysisResponse(BaseModel):
    total_monthly_leak: Decimal = Field(..., description="Combined monthly drain from micro-expenses")
    total_annual_leak: Decimal = Field(..., description="Combined annual projected drain")
    leaks: List[SpendingLeakItem] = Field(default_factory=list, description="Detected micro-spending leaks")
    summary: str = Field(..., description="AI synthesis of spending leaks")


class AuditSubscriptionItem(BaseModel):
    merchant: str = Field(..., description="Merchant or service name")
    category_name: str = Field(..., description="Category name")
    estimated_amount: Decimal = Field(..., description="Regular recurring charge amount")
    frequency: str = Field(..., description="'monthly', 'weekly', 'yearly'")
    annual_cost: Decimal = Field(..., description="Projected annual cost")
    flag: Optional[str] = Field(default=None, description="Flag such as 'duplicate_category', 'pricey', 'frequent'")
    optimization_tip: Optional[str] = Field(default=None, description="Suggestion (e.g. bundle, cancel, switch to annual)")


class SubscriptionAuditResponse(BaseModel):
    total_monthly_commitment: Decimal = Field(..., description="Total monthly commitment across subscriptions")
    total_annual_commitment: Decimal = Field(..., description="Total annual commitment")
    active_subscriptions: List[AuditSubscriptionItem] = Field(default_factory=list, description="Audited subscriptions")
    duplicate_warnings: List[str] = Field(default_factory=list, description="Warnings about redundant overlapping services")
    potential_annual_savings: Decimal = Field(default=Decimal("0.00"), description="Estimated savings if optimized")
    insights: List[str] = Field(default_factory=list, description="Audit insights and tips")

