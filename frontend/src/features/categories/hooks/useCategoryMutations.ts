import { useMutation, useQueryClient } from "@tanstack/react-query";
import { categoriesApi, CategoryCreate, CategoryUpdate } from "@/lib/api/categories";
import { paymentMethodsApi, PaymentMethodCreate, PaymentMethodUpdate } from "@/lib/api/paymentMethods";

export function useCategoryMutations() {
  const queryClient = useQueryClient();

  const invalidateCache = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["categories"] }),
      queryClient.invalidateQueries({ queryKey: ["expenses"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]);
  };

  const createCategory = useMutation({
    mutationFn: (data: CategoryCreate) => categoriesApi.create(data),
    onSuccess: invalidateCache,
  });

  const renameCategory = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CategoryUpdate }) =>
      categoriesApi.update(id, data),
    onSuccess: invalidateCache,
  });

  const deleteCategory = useMutation({
    mutationFn: (id: string) => categoriesApi.delete(id),
    onSuccess: invalidateCache,
  });

  return {
    createCategory: createCategory.mutateAsync,
    isCreatingCategory: createCategory.isPending,
    renameCategory: renameCategory.mutateAsync,
    isRenamingCategory: renameCategory.isPending,
    deleteCategory: deleteCategory.mutateAsync,
    isDeletingCategory: deleteCategory.isPending,
  };
}

export function usePaymentMethodMutations() {
  const queryClient = useQueryClient();

  const invalidateCache = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["payment-methods"] }),
      queryClient.invalidateQueries({ queryKey: ["expenses"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]);
  };

  const createPaymentMethod = useMutation({
    mutationFn: (data: PaymentMethodCreate) => paymentMethodsApi.create(data),
    onSuccess: invalidateCache,
  });

  const renamePaymentMethod = useMutation({
    mutationFn: ({ id, data }: { id: string; data: PaymentMethodUpdate }) =>
      paymentMethodsApi.update(id, data),
    onSuccess: invalidateCache,
  });

  const deletePaymentMethod = useMutation({
    mutationFn: (id: string) => paymentMethodsApi.delete(id),
    onSuccess: invalidateCache,
  });

  return {
    createPaymentMethod: createPaymentMethod.mutateAsync,
    isCreatingPM: createPaymentMethod.isPending,
    renamePaymentMethod: renamePaymentMethod.mutateAsync,
    isRenamingPM: renamePaymentMethod.isPending,
    deletePaymentMethod: deletePaymentMethod.mutateAsync,
    isDeletingPM: deletePaymentMethod.isPending,
  };
}
