import { client } from "./client";
import { DataResponse } from "./expenses";

export type BudgetPeriodType = "month" | "week" | "day";

export interface BudgetRead {
  id?: string;
  period_type: BudgetPeriodType;
  period_key?: string;
  month?: string;
  amount: string | null;
  updated_at: string | null;
}

export interface BudgetCreate {
  amount: number;
  period_type?: BudgetPeriodType;
  period_key?: string;
  month?: string;
}

export const budgetApi = {
  get: (period_type: BudgetPeriodType = "month", period_key?: string) => {
    const params = new URLSearchParams();
    params.set("period_type", period_type);
    if (period_key) params.set("period_key", period_key);
    return client.get<DataResponse<BudgetRead>>(`/budget?${params.toString()}`);
  },

  list: (period_type?: BudgetPeriodType) => {
    const params = new URLSearchParams();
    if (period_type) params.set("period_type", period_type);
    return client.get<DataResponse<BudgetRead[]>>(`/budget/all${period_type ? `?${params.toString()}` : ""}`);
  },

  upsert: (data: BudgetCreate) =>
    client.put<DataResponse<BudgetRead>>("/budget", data),

  delete: (period_type: BudgetPeriodType = "month", period_key?: string) => {
    const params = new URLSearchParams();
    params.set("period_type", period_type);
    if (period_key) params.set("period_key", period_key);
    return client.delete<void>(`/budget?${params.toString()}`);
  },
};
