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

export const aiApi = {
  categorize: (data: CategorizeRequest) =>
    client.post<DataResponse<CategorizeResponse>>("/ai/categorize", data),

  parseExpense: (data: ParseExpenseRequest) =>
    client.post<DataResponse<ParseExpenseResponse>>("/ai/parse-expense", data),
};
