import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { budgetApi } from "@/lib/api/budget";

export function useBudget() {
  return useQuery({
    queryKey: ["budget"],
    queryFn: async () => {
      const response = await budgetApi.get();
      return response.data;
    },
  });
}

export function useBudgetMutation() {
  const queryClient = useQueryClient();

  const upsertMutation = useMutation({
    mutationFn: (amount: number) => budgetApi.upsert(amount),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["budget"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => budgetApi.delete(),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["budget"] }),
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
