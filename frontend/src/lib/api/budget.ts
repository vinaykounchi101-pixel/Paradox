import { client } from "./client";
import { DataResponse } from "./expenses";

export interface BudgetRead {
  amount: string | null;
  updated_at: string | null;
}

export interface BudgetCreate {
  amount: number;
}

export const budgetApi = {
  get: () => client.get<DataResponse<BudgetRead>>("/budget"),
  
  upsert: (amount: number) => client.put<DataResponse<BudgetRead>>("/budget", { amount }),
};
