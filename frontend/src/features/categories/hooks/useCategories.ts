import { useQuery } from "@tanstack/react-query";
import { categoriesApi } from "@/lib/api/categories";
import { paymentMethodsApi } from "@/lib/api/paymentMethods";

export function useCategories() {
  return useQuery({
    queryKey: ["categories"],
    queryFn: async () => {
      const response = await categoriesApi.list();
      return response.data;
    },
  });
}

export function usePaymentMethods() {
  return useQuery({
    queryKey: ["payment-methods"],
    queryFn: async () => {
      const response = await paymentMethodsApi.list();
      return response.data;
    },
  });
}
