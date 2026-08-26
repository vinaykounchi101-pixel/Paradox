import { client } from "./client";
import { ExpenseRead, DataResponse } from "./expenses";

export interface DashboardCategoryBreakdown {
  category_id: string;
  category_name: string;
  total: string;
  percentage: number;
}

export interface DashboardTopCategory {
  category_id: string;
  category_name: string;
  total: string;
}

export interface DashboardTrendItem {
  label: string;
  total: string;
}

export interface DashboardBudget {
  amount: string | null;
  spent: string;
  remaining: string | null;
  status: string | null;
}

export interface DashboardRead {
  period: string;
  total_spent: string;
  budget: DashboardBudget;
  category_breakdown: DashboardCategoryBreakdown[];
  top_categories: DashboardTopCategory[];
  trend: DashboardTrendItem[];
  recent_expenses: ExpenseRead[];
}

export const dashboardApi = {
  get: (period: "current_month" | "last_30_days" | "current_week" = "current_month") =>
    client.get<DataResponse<DashboardRead>>(`/dashboard?period=${period}`),
};
