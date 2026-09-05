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
    is_new_category: bool = Field(default=False, description="True if the category is newly recommended and not currently in available_categories")


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


class ParseSmsRequest(BaseModel):
    text: str = Field(..., min_length=5, max_length=1000, description="Raw bank or UPI SMS text")
    available_categories: Optional[List[str]] = Field(default=None, description="Available user categories")
    available_payment_methods: Optional[List[str]] = Field(default=None, description="Available payment methods")


class ParseSmsResponse(BaseModel):
    amount: Optional[Decimal] = Field(default=None, description="Extracted transaction amount")
    merchant: Optional[str] = Field(default=None, description="Extracted merchant or payee")
    date: Optional[str] = Field(default=None, description="Transaction date in YYYY-MM-DD format")
    category_name: Optional[str] = Field(default=None, description="Suggested category name")
    payment_method_name: Optional[str] = Field(default=None, description="Inferred payment method (UPI, Card, NetBanking)")
    reference_id: Optional[str] = Field(default=None, description="Transaction reference or UTR number")
    transaction_type: str = Field(default="debit", description="'debit' or 'credit'")
    confidence: float = Field(default=0.85, ge=0.0, le=1.0, description="Parser confidence score")
    provider_used: str = Field(..., description="Provider or engine used")


class CheckDuplicateRequest(BaseModel):
    amount: Decimal = Field(..., gt=0, description="Amount to verify")
    date: str = Field(..., description="Date of the candidate expense (YYYY-MM-DD)")
    description: Optional[str] = Field(default=None, description="Candidate description")
    window_days: int = Field(default=2, ge=0, le=7, description="Search tolerance window in days")


class CheckDuplicateResponse(BaseModel):
    has_duplicate: bool = Field(..., description="True if a probable matching transaction exists")
    duplicate_id: Optional[str] = Field(default=None, description="ID of existing matching expense")
    duplicate_amount: Optional[Decimal] = Field(default=None, description="Amount of existing expense")
    duplicate_date: Optional[str] = Field(default=None, description="Date of existing expense")
    duplicate_description: Optional[str] = Field(default=None, description="Description of existing expense")
    message: Optional[str] = Field(default=None, description="Warning notification text")


class FiftyThirtyTwentyItem(BaseModel):
    category_type: str = Field(..., description="'needs', 'wants', or 'savings'")
    label: str = Field(..., description="Display label (e.g. 'Needs (50%)')")
    target_percentage: float = Field(..., description="Target allocation percentage (50, 30, 20)")
    actual_amount: Decimal = Field(..., description="Actual amount spent/saved in this pillar")
    actual_percentage: float = Field(..., description="Actual percentage of total spent")
    variance_amount: Decimal = Field(..., description="Actual amount minus target allowance")
    status: str = Field(..., description="'on_track', 'over', or 'under'")
    top_categories: List[str] = Field(default_factory=list, description="Top categories contributing to this pillar")


class FiftyThirtyTwentyResponse(BaseModel):
    total_spent: Decimal = Field(..., description="Total spent in period")
    target_budget: Decimal = Field(..., description="Monthly budget baseline used for 50/30/20")
    needs: FiftyThirtyTwentyItem = Field(..., description="Essential living costs (50%)")
    wants: FiftyThirtyTwentyItem = Field(..., description="Discretionary lifestyle spending (30%)")
    savings: FiftyThirtyTwentyItem = Field(..., description="Surplus & investments (20%)")
    rebalance_advice: List[str] = Field(default_factory=list, description="Actionable tips to align with 50/30/20")
    adherence_score: int = Field(..., ge=0, le=100, description="Overall alignment score (0-100)")


class AchievementBadge(BaseModel):
    id: str = Field(..., description="Badge unique identifier")
    title: str = Field(..., description="Badge title")
    description: str = Field(..., description="Achievement criteria")
    icon: str = Field(..., description="Icon identifier")
    tier: str = Field(..., description="'bronze', 'silver', 'gold', 'diamond'")
    is_unlocked: bool = Field(..., description="Whether user earned this badge")
    progress: int = Field(..., ge=0, le=100, description="Progress percentage (0-100)")
    progress_label: str = Field(..., description="Progress indicator text (e.g. '4/7 days')")


class AchievementsResponse(BaseModel):
    badges: List[AchievementBadge] = Field(default_factory=list, description="Earned and in-progress achievement badges")
    active_streak_days: int = Field(..., description="Consecutive days maintaining budget discipline")
    total_unlocked: int = Field(..., description="Total unlocked achievements count")
    motivation_quote: str = Field(..., description="Punchy gamified motivation quote")


# =========================================================================
# New AI Suite Feature Schemas
# =========================================================================

class ChatMessage(BaseModel):
    role: str = Field(..., description="'user' or 'assistant'")
    content: str = Field(..., description="Message content")


class AIChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=1000, description="User question or statement")
    history: List[ChatMessage] = Field(default_factory=list, description="Recent conversation history")


class AIChatResponse(BaseModel):
    reply: str = Field(..., description="Conversational financial assistant response")
    suggested_followups: List[str] = Field(default_factory=list, description="Quick followup question chips")
    provider_used: str = Field(..., description="AI provider used")


class SpendingAnomalyItem(BaseModel):
    id: str = Field(..., description="Expense ID")
    date: str = Field(..., description="Transaction date (YYYY-MM-DD)")
    amount: Decimal = Field(..., description="Expense amount")
    category_name: str = Field(..., description="Category")
    description: Optional[str] = Field(default=None, description="Merchant or item description")
    severity: str = Field(..., description="'moderate', 'high', or 'critical'")
    reason: str = Field(..., description="Explanation of why this transaction is an anomaly")


class AnomaliesResponse(BaseModel):
    anomalies: List[SpendingAnomalyItem] = Field(default_factory=list, description="List of detected anomalies")
    total_anomalies: int = Field(..., description="Number of anomalies flagged")
    summary: str = Field(..., description="Brief health summary of spending spikes")


class CategoryForecastItem(BaseModel):
    category_name: str = Field(..., description="Category name")
    current_spent: Decimal = Field(..., description="Spend in current period")
    projected_next_month: Decimal = Field(..., description="Predicted spend for next month")
    trend_direction: str = Field(..., description="'up', 'down', or 'stable'")
    confidence: float = Field(default=0.85, ge=0.0, le=1.0)


class SpendingForecastResponse(BaseModel):
    total_projected_next_month: Decimal = Field(..., description="Predicted total spend for next 30 days")
    category_forecasts: List[CategoryForecastItem] = Field(default_factory=list, description="Category-wise projections")
    growth_rate_pct: float = Field(..., description="Projected spend change percentage relative to current month")
    confidence: float = Field(default=0.85, ge=0.0, le=1.0)
    forecast_insights: List[str] = Field(default_factory=list, description="Actionable insights on upcoming trends")


class SavingsPlanRequest(BaseModel):
    goal_name: str = Field(..., min_length=2, max_length=100, description="Name of the savings goal (e.g. 'Emergency Fund')")
    target_amount: Decimal = Field(..., gt=0, description="Target savings amount to accumulate")
    target_months: int = Field(..., ge=1, le=120, description="Timeframe in months")


class SavingsPlanCategoryCut(BaseModel):
    category_name: str = Field(..., description="Category to trim")
    current_monthly_spend: Decimal = Field(..., description="Current monthly average spend")
    suggested_monthly_spend: Decimal = Field(..., description="Target monthly spend after cut")
    monthly_cut_amount: Decimal = Field(..., description="Monthly reduction amount")
    cut_percentage: float = Field(..., description="Percentage reduction")


class SavingsPlanResponse(BaseModel):
    goal_name: str = Field(..., description="Savings goal name")
    target_amount: Decimal = Field(..., description="Total target amount")
    target_months: int = Field(..., description="Target duration in months")
    required_monthly_savings: Decimal = Field(..., description="Required savings per month")
    current_discretionary_spend: Decimal = Field(..., description="Total monthly spend in trimmable categories")
    feasibility: str = Field(..., description="'highly_achievable', 'achievable', 'challenging', or 'unrealistic'")
    category_cuts: List[SavingsPlanCategoryCut] = Field(default_factory=list, description="Category-wise budget cuts")
    action_steps: List[str] = Field(default_factory=list, description="Step-by-step roadmap to achieve goal")


class AnalyzeSentimentRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=500, description="Expense description or note to analyze")
    amount: Optional[Decimal] = Field(default=None, description="Optional transaction amount for context")


class AnalyzeSentimentResponse(BaseModel):
    sentiment: str = Field(..., description="'positive', 'neutral', 'negative', 'remorse', or 'stress'")
    spending_tag: str = Field(..., description="Behavioral spending tag (e.g. 'Buyer\'s Remorse', 'Stress Spending', 'Celebration', 'Essential')")
    confidence: float = Field(default=0.85, ge=0.0, le=1.0)
    reflection: str = Field(..., description="Mindful reflection or tip regarding this spending behavior")


class WrappedTopCategory(BaseModel):
    category_name: str = Field(..., description="Category name")
    amount: Decimal = Field(..., description="Total spent in category")
    percentage: float = Field(..., description="Percentage of monthly spend")


class WrappedSplurge(BaseModel):
    amount: Decimal = Field(..., description="Splurge transaction amount")
    description: str = Field(..., description="Item or merchant")
    date: str = Field(..., description="Transaction date")
    category_name: str = Field(..., description="Category")


class MonthlyWrappedResponse(BaseModel):
    month: str = Field(..., description="Formatted month (e.g. 'August 2026')")
    total_spent: Decimal = Field(..., description="Total spend for month")
    total_transactions: int = Field(..., description="Total transactions recorded")
    active_streak_days: int = Field(..., description="Discipline streak recorded in month")
    archetype_title: str = Field(..., description="Personal financial archetype title (e.g. 'The Mindful Strategist')")
    archetype_description: str = Field(..., description="Flavor text explaining the archetype")
    top_categories: List[WrappedTopCategory] = Field(default_factory=list, description="Top 3 spending categories")
    biggest_splurge: Optional[WrappedSplurge] = Field(default=None, description="Single largest purchase of the month")
    most_frequent_merchant: Optional[str] = Field(default=None, description="Most visited merchant")
    savings_achieved: Decimal = Field(..., description="Estimated savings or surplus relative to budget")
    personalized_recap: List[str] = Field(default_factory=list, description="Personalized highlight cards")


class VibeCheckResponse(BaseModel):
    vibe_emoji: str = Field(..., description="Primary emoji representation (e.g. 🤑, 🧘, 💀, ☕)")
    vibe_title: str = Field(..., description="Vibe title (e.g. 'Living Large', 'Zen Saver', 'Down Bad')")
    burn_rate_status: str = Field(..., description="'chill', 'steady', 'spicy', 'critical'")
    roast_commentary: str = Field(..., description="Witty, humorous financial reality check")
    is_roast_mode: bool = Field(default=True, description="Whether roast mode is active")
    daily_burn_rate: Decimal = Field(..., description="Current daily spend rate")
    budget_percent_consumed: float = Field(..., description="Percentage of monthly budget spent")



