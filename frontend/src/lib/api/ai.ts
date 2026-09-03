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
};


