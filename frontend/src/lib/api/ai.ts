import { client } from "./client";
import { DataResponse } from "./expenses";

export interface CategorizeRequest {
  description: string;
  available_categories?: string[];
}

export interface CategorizeResponse {
  category_name: string;
  confidence: number;
  reasoning?: string;
  provider_used: string;
  is_new_category?: boolean;
}

export interface ParseExpenseRequest {
  text: string;
  available_categories?: string[];
  available_payment_methods?: string[];
}

export interface ParseExpenseResponse {
  amount?: number | string | null;
  category_name?: string | null;
  payment_method_name?: string | null;
  date?: string | null;
  description?: string | null;
  confidence: number;
  reasoning?: string | null;
  provider_used: string;
}

export interface AIInsightsResponse {
  health_status: "healthy" | "cautious" | "critical";
  headline: string;
  alerts: string[];
  saving_tips: string[];
  projected_spend?: number | string | null;
  daily_burn_rate?: number | string | null;
  confidence: number;
  provider_used: string;
}

export interface CategoryAllocation {
  category_name: string;
  percentage: number;
  suggested_amount: number | string;
}

export interface SuggestBudgetResponse {
  period_type: "month" | "week" | "day";
  suggested_amount: number | string;
  reasoning: string;
  category_allocations: CategoryAllocation[];
  confidence: number;
  provider_used: string;
}

export interface ParsedReceiptItem {
  item_name: string;
  amount: number | string;
  category_suggestion?: string | null;
}

export interface ParseReceiptRequest {
  text: string;
}

export interface ParseReceiptResponse {
  merchant_name?: string | null;
  total_amount?: number | string | null;
  date?: string | null;
  category_name?: string | null;
  payment_method_name?: string | null;
  items: ParsedReceiptItem[];
  provider_used: string;
}

export interface SimulatePurchaseRequest {
  amount: number;
  category_name?: string;
  description?: string;
}

export interface SimulatePurchaseResponse {
  verdict: "safe" | "caution" | "over_budget";
  headline: string;
  advice: string;
  current_remaining_budget: number | string;
  projected_remaining_budget: number | string;
  safe_to_spend_daily_before: number | string;
  safe_to_spend_daily_after: number | string;
  category_impact?: string | null;
  savings_impact: string;
  can_proceed: boolean;
}

export interface SafeToSpendResponse {
  safe_daily_allowance: number | string;
  current_daily_burn_rate: number | string;
  remaining_budget: number | string;
  days_remaining: number;
  depletion_date?: string | null;
  status: "optimal" | "warning" | "danger";
  burn_status_message: string;
}

export interface HealthScorePillar {
  name: string;
  score: number;
  max_score: number;
  feedback: string;
}

export interface FinancialHealthScoreResponse {
  score: number;
  status: "excellent" | "good" | "needs_attention";
  headline: string;
  pillars: HealthScorePillar[];
  recommendations: string[];
}

export interface SpendingLeakItem {
  merchant_or_pattern: string;
  frequency_per_month: number;
  avg_amount: number | string;
  monthly_drain: number | string;
  annualized_drain: number | string;
  category_name: string;
  savings_tip: string;
}

export interface LeakAnalysisResponse {
  total_monthly_leak: number | string;
  total_annual_leak: number | string;
  leaks: SpendingLeakItem[];
  summary: string;
}

export interface AuditSubscriptionItem {
  merchant: string;
  category_name: string;
  estimated_amount: number | string;
  frequency: string;
  annual_cost: number | string;
  flag?: string | null;
  optimization_tip?: string | null;
}

export interface SubscriptionAuditResponse {
  total_monthly_commitment: number | string;
  total_annual_commitment: number | string;
  active_subscriptions: AuditSubscriptionItem[];
  duplicate_warnings: string[];
  potential_annual_savings: number | string;
  insights: string[];
}

export interface ParseSmsRequest {
  text: string;
  available_categories?: string[];
  available_payment_methods?: string[];
}

export interface ParseSmsResponse {
  amount?: number | string | null;
  merchant?: string | null;
  date?: string | null;
  category_name?: string | null;
  payment_method_name?: string | null;
  reference_id?: string | null;
  transaction_type: "debit" | "credit";
  confidence: number;
  provider_used: string;
}

export interface CheckDuplicateRequest {
  amount: number | string;
  date: string;
  description?: string;
  window_days?: number;
}

export interface CheckDuplicateResponse {
  has_duplicate: boolean;
  duplicate_id?: string | null;
  duplicate_amount?: number | string | null;
  duplicate_date?: string | null;
  duplicate_description?: string | null;
  message?: string | null;
}

export interface FiftyThirtyTwentyItem {
  category_type: "needs" | "wants" | "savings";
  label: string;
  target_percentage: number;
  actual_amount: number | string;
  actual_percentage: number;
  variance_amount: number | string;
  status: "on_track" | "over" | "under";
  top_categories: string[];
}

export interface FiftyThirtyTwentyResponse {
  total_spent: number | string;
  target_budget: number | string;
  needs: FiftyThirtyTwentyItem;
  wants: FiftyThirtyTwentyItem;
  savings: FiftyThirtyTwentyItem;
  rebalance_advice: string[];
  adherence_score: number;
}

export interface AchievementBadge {
  id: string;
  title: string;
  description: string;
  icon: string;
  tier: "bronze" | "silver" | "gold" | "diamond";
  is_unlocked: boolean;
  progress: number;
  progress_label: string;
}

export interface AchievementsResponse {
  badges: AchievementBadge[];
  active_streak_days: number;
  total_unlocked: number;
  motivation_quote: string;
}

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

export interface AIChatRequest {
  message: string;
  history?: ChatMessage[];
}

export interface AIChatResponse {
  reply: string;
  suggested_followups: string[];
  provider_used: string;
}

export interface SpendingAnomalyItem {
  id: string;
  date: string;
  amount: number | string;
  category_name: string;
  description?: string | null;
  severity: "moderate" | "high" | "critical";
  reason: string;
}

export interface AnomaliesResponse {
  anomalies: SpendingAnomalyItem[];
  total_anomalies: number;
  summary: string;
}

export interface CategoryForecastItem {
  category_name: string;
  current_spent: number | string;
  projected_next_month: number | string;
  trend_direction: "up" | "down" | "stable";
  confidence: number;
}

export interface SpendingForecastResponse {
  total_projected_next_month: number | string;
  category_forecasts: CategoryForecastItem[];
  growth_rate_pct: number;
  confidence: number;
  forecast_insights: string[];
}

export interface SavingsPlanRequest {
  goal_name: string;
  target_amount: number;
  target_months: number;
}

export interface SavingsPlanCategoryCut {
  category_name: string;
  current_monthly_spend: number | string;
  suggested_monthly_spend: number | string;
  monthly_cut_amount: number | string;
  cut_percentage: number;
}

export interface SavingsPlanResponse {
  goal_name: string;
  target_amount: number | string;
  target_months: number;
  required_monthly_savings: number | string;
  current_discretionary_spend: number | string;
  feasibility: "highly_achievable" | "achievable" | "challenging" | "unrealistic";
  category_cuts: SavingsPlanCategoryCut[];
  action_steps: string[];
}

export interface AnalyzeSentimentRequest {
  text: string;
  amount?: number;
}

export interface AnalyzeSentimentResponse {
  sentiment: "positive" | "neutral" | "negative" | "remorse" | "stress";
  spending_tag: string;
  confidence: number;
  reflection: string;
}

export interface WrappedTopCategory {
  category_name: string;
  amount: number | string;
  percentage: number;
}

export interface WrappedSplurge {
  amount: number | string;
  description: string;
  date: string;
  category_name: string;
}

export interface MonthlyWrappedResponse {
  month: string;
  total_spent: number | string;
  total_transactions: number;
  active_streak_days: number;
  archetype_title: string;
  archetype_description: string;
  top_categories: WrappedTopCategory[];
  biggest_splurge?: WrappedSplurge | null;
  most_frequent_merchant?: string | null;
  savings_achieved: number | string;
  personalized_recap: string[];
}

export interface VibeCheckResponse {
  vibe_emoji: string;
  vibe_title: string;
  burn_rate_status: "chill" | "steady" | "spicy" | "critical";
  roast_commentary: string;
  is_roast_mode: boolean;
  daily_burn_rate: number | string;
  budget_percent_consumed: number;
}

export const aiApi = {
  categorize: (data: CategorizeRequest) =>
    client.post<DataResponse<CategorizeResponse>>("/ai/categorize", data),

  parseExpense: (data: ParseExpenseRequest) =>
    client.post<DataResponse<ParseExpenseResponse>>("/ai/parse-expense", data),

  getInsights: (period: "current_month" | "last_30_days" | "current_week" = "current_month") =>
    client.get<DataResponse<AIInsightsResponse>>(`/ai/insights?period=${period}`),

  suggestBudget: (periodType: "month" | "week" | "day" = "month") =>
    client.get<DataResponse<SuggestBudgetResponse>>(`/ai/suggest-budget?period_type=${periodType}`),

  parseReceipt: (data: ParseReceiptRequest) =>
    client.post<DataResponse<ParseReceiptResponse>>("/ai/parse-receipt", data),

  parseSms: (data: ParseSmsRequest) =>
    client.post<DataResponse<ParseSmsResponse>>("/ai/parse-sms", data),

  checkDuplicate: (data: CheckDuplicateRequest) =>
    client.post<DataResponse<CheckDuplicateResponse>>("/ai/check-duplicate", data),

  scanReceipt: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return client.post<DataResponse<ParseExpenseResponse>>("/ai/scan-receipt", formData);
  },

  simulatePurchase: (data: SimulatePurchaseRequest) =>
    client.post<DataResponse<SimulatePurchaseResponse>>("/ai/simulate-purchase", data),

  getSafeToSpend: () =>
    client.get<DataResponse<SafeToSpendResponse>>("/ai/safe-to-spend"),

  getHealthScore: () =>
    client.get<DataResponse<FinancialHealthScoreResponse>>("/ai/health-score"),

  getLeakAnalysis: (threshold: number = 150) =>
    client.get<DataResponse<LeakAnalysisResponse>>(`/ai/leak-analysis?threshold=${threshold}`),

  getSubscriptionAudit: () =>
    client.get<DataResponse<SubscriptionAuditResponse>>("/ai/subscription-audit"),

  getFiftyThirtyTwenty: () =>
    client.get<DataResponse<FiftyThirtyTwentyResponse>>("/ai/fifty-thirty-twenty"),

  getAchievements: () =>
    client.get<DataResponse<AchievementsResponse>>("/ai/achievements"),

  chat: (data: AIChatRequest) =>
    client.post<DataResponse<AIChatResponse>>("/ai/chat", data),

  getAnomalies: () =>
    client.get<DataResponse<AnomaliesResponse>>("/ai/anomalies"),

  getForecast: () =>
    client.get<DataResponse<SpendingForecastResponse>>("/ai/forecast"),

  createSavingsPlan: (data: SavingsPlanRequest) =>
    client.post<DataResponse<SavingsPlanResponse>>("/ai/savings-plan", data),

  analyzeSentiment: (data: AnalyzeSentimentRequest) =>
    client.post<DataResponse<AnalyzeSentimentResponse>>("/ai/analyze-sentiment", data),

  getMonthlyWrapped: (month?: string) =>
    client.get<DataResponse<MonthlyWrappedResponse>>(`/ai/monthly-wrapped${month ? `?month=${month}` : ""}`),

  getVibeCheck: (roastMode: boolean = true) =>
    client.get<DataResponse<VibeCheckResponse>>(`/ai/vibe-check?roast_mode=${roastMode}`),
};




