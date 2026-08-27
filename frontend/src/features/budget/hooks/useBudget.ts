import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { budgetApi } from "@/lib/api/budget";

export function useBudget(month?: string) {
  return useQuery({
    queryKey: ["budget", month || "current"],
    queryFn: async () => {
      const response = await budgetApi.get(month);
      return response.data;
    },
  });
}

export function useBudgetsList() {
  return useQuery({
    queryKey: ["budgets-all"],
    queryFn: async () => {
      const response = await budgetApi.list();
      return response.data;
    },
  });
}

export function useBudgetMutation() {
  const queryClient = useQueryClient();

  const upsertMutation = useMutation({
    mutationFn: ({ amount, month }: { amount: number; month?: string }) =>
      budgetApi.upsert(amount, month),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["budget"] }),
        queryClient.invalidateQueries({ queryKey: ["budgets-all"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (month?: string) => budgetApi.delete(month),
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
