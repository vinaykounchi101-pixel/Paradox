"use client";

import React from "react";
import BudgetConfigView from "@/features/budget/components/BudgetConfigView";
import { ProtectedRoute } from "@/components/common/ProtectedRoute";

export default function BudgetPage() {
  return (
    <ProtectedRoute>
      <BudgetConfigView />
    </ProtectedRoute>
  );
}
