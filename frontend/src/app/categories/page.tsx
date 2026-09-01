"use client";

import React from "react";
import CategoryManagementView from "@/features/categories/components/CategoryManagementView";
import { ProtectedRoute } from "@/components/common/ProtectedRoute";

export default function CategoriesPage() {
  return (
    <ProtectedRoute>
      <CategoryManagementView />
    </ProtectedRoute>
  );
}
