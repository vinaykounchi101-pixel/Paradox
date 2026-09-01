"use client";

import React from "react";
import DashboardView from "@/features/dashboard/components/DashboardView";
import { ProtectedRoute } from "@/components/common/ProtectedRoute";

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <DashboardView />
    </ProtectedRoute>
  );
}
