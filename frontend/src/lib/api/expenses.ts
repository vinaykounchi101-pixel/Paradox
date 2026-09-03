import { client } from "./client";

export interface CategoryRead {
  id: string;
  name: string;
  is_default: boolean;
  created_at: string;
  updated_at: string;
}

export interface PaymentMethodRead {
  id: string;
  name: string;
  is_default: boolean;
  created_at: string;
  updated_at: string;
}

export interface ExpenseRead {
  id: string;
  amount: string;
  category_id: string;
  payment_method_id: string;
  date: string;
  description: string | null;
  is_recurring?: boolean;
  recurring_frequency?: string | null;
  created_at: string;
  updated_at: string;
  category?: CategoryRead;
  payment_method?: PaymentMethodRead;
}

export interface ExpenseCreate {
  amount: number;
  category_id: string;
  payment_method_id: string;
  date: string;
  description?: string;
  is_recurring?: boolean;
  recurring_frequency?: string;
}

export interface ExpenseUpdate {
  amount?: number;
  category_id?: string;
  payment_method_id?: string;
  date?: string;
  description?: string;
  is_recurring?: boolean;
  recurring_frequency?: string;
}

export interface ExpenseFilters {
  search?: string;
  category_id?: string;
  date_from?: string;
  date_to?: string;
  sort_by?: "date" | "amount" | "category";
  sort_order?: "asc" | "desc";
  page?: number;
  page_size?: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  meta: {
    page: number;
    page_size: number;
    total_items: number;
    total_pages: number;
  };
}

export interface DataResponse<T> {
  data: T;
}

export const expensesApi = {
  list: (filters: ExpenseFilters = {}) => {
    const query = new URLSearchParams();
    if (filters.search) query.append("search", filters.search);
    if (filters.category_id) query.append("category_id", filters.category_id);
    if (filters.date_from) query.append("date_from", filters.date_from);
    if (filters.date_to) query.append("date_to", filters.date_to);
    if (filters.sort_by) query.append("sort_by", filters.sort_by);
    if (filters.sort_order) query.append("sort_order", filters.sort_order);
    if (filters.page) query.append("page", String(filters.page));
    if (filters.page_size) query.append("page_size", String(filters.page_size));

    const queryString = query.toString();
    return client.get<PaginatedResponse<ExpenseRead>>(`/expenses${queryString ? `?${queryString}` : ""}`);
  },

  get: (id: string) => client.get<DataResponse<ExpenseRead>>(`/expenses/${id}`),

  create: (data: ExpenseCreate) => client.post<DataResponse<ExpenseRead>>("/expenses", data),

  update: (id: string, data: ExpenseUpdate) => client.patch<DataResponse<ExpenseRead>>(`/expenses/${id}`, data),

  delete: (id: string) => client.delete<void>(`/expenses/${id}`),

  exportCsv: async (params?: { date_from?: string; date_to?: string }): Promise<void> => {
    const query = new URLSearchParams();
    if (params?.date_from) query.append("date_from", params.date_from);
    if (params?.date_to) query.append("date_to", params.date_to);
    const qs = query.toString();
    const token = (await import("./client")).getAccessToken();
    const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://127.0.0.1:8000/api/v1";
    const res = await fetch(`${baseUrl}/expenses/export${qs ? `?${qs}` : ""}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      credentials: "include",
    });
    if (!res.ok) throw new Error("Failed to export CSV");
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `paradox_expenses_${new Date().toISOString().split("T")[0]}.csv`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  importCsv: async (file: File): Promise<{ imported: number; skipped: number; message: string }> => {
    const formData = new FormData();
    formData.append("file", file);
    const res = await client.post<{ data: { imported: number; skipped: number; message: string } }>(
      "/expenses/import",
      formData
    );
    return res.data;
  },

  getRecurring: () =>
    client.get<DataResponse<{ total_monthly_commitment: string; total_count: number; recurring_expenses: ExpenseRead[] }>>(
      "/expenses/recurring"
    ),
};
