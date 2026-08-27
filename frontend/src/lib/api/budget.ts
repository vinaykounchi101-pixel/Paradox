import { client } from "./client";
import { DataResponse } from "./expenses";

export interface BudgetRead {
  id?: string;
  month?: string;
  amount: string | null;
  updated_at: string | null;
}

export interface BudgetCreate {
  amount: number;
  month?: string;
}

export const budgetApi = {
  get: (month?: string) =>
    client.get<DataResponse<BudgetRead>>(month ? `/budget?month=${encodeURIComponent(month)}` : "/budget"),

  list: () =>
    client.get<DataResponse<BudgetRead[]>>("/budget/all"),

  upsert: (amount: number, month?: string) =>
    client.put<DataResponse<BudgetRead>>("/budget", { amount, month }),

  delete: (month?: string) =>
    client.delete<void>(month ? `/budget?month=${encodeURIComponent(month)}` : "/budget"),
};
