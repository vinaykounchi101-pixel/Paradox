import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "@/lib/api/dashboard";

export function useDashboard(period: "current_month" | "last_30_days" | "current_week" = "current_month") {
  return useQuery({
    queryKey: ["dashboard", period],
    queryFn: async () => {
      const response = await dashboardApi.get(period);
      return response.data;
    },
  });
}
