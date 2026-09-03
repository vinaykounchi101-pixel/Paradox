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
};

