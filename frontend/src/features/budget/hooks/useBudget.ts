import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { budgetApi, BudgetCreate, BudgetPeriodType } from "@/lib/api/budget";

export function useBudget(period_type: BudgetPeriodType = "month", period_key?: string) {
  return useQuery({
    queryKey: ["budget", period_type, period_key || "current"],
    queryFn: async () => {
      const response = await budgetApi.get(period_type, period_key);
      return response.data;
    },
  });
}

export function useBudgetsList(period_type?: BudgetPeriodType) {
  return useQuery({
    queryKey: ["budgets-all", period_type || "all"],
    queryFn: async () => {
      const response = await budgetApi.list(period_type);
      return response.data;
    },
  });
}

export function useBudgetMutation() {
  const queryClient = useQueryClient();

  const upsertMutation = useMutation({
    mutationFn: (data: BudgetCreate) => budgetApi.upsert(data),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["budget"] }),
        queryClient.invalidateQueries({ queryKey: ["budgets-all"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ({ period_type, period_key }: { period_type: BudgetPeriodType; period_key?: string }) =>
      budgetApi.delete(period_type, period_key),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["budget"] }),
        queryClient.invalidateQueries({ queryKey: ["budgets-all"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
    },
  });

  return {
    upsertBudget: upsertMutation.mutateAsync,
    isSaving: upsertMutation.isPending,
    deleteBudget: deleteMutation.mutateAsync,
    isDeleting: deleteMutation.isPending,
  };
}
