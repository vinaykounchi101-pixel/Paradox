"use client";

import React from "react";
import ExpenseListView from "@/features/expenses/components/ExpenseListView";
import { ProtectedRoute } from "@/components/common/ProtectedRoute";

export default function ExpensesPage() {
  return (
    <ProtectedRoute>
      <ExpenseListView />
    </ProtectedRoute>
  );
}
