import { useQuery } from "@tanstack/react-query";
import { expensesApi, ExpenseFilters } from "@/lib/api/expenses";

export function useExpenses(filters: ExpenseFilters = {}) {
  return useQuery({
    queryKey: ["expenses", filters],
    queryFn: async () => {
      const response = await expensesApi.list(filters);
      return response;
    },
  });
}
