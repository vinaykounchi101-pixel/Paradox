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
}

export interface ExpenseUpdate {
  amount?: number;
  category_id?: string;
  payment_method_id?: string;
  date?: string;
  description?: string;
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
};
