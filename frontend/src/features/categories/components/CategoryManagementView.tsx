"use client";

import React, { useState } from "react";
import { motion } from "framer-motion";
import {
  Tags,
  CreditCard,
  Plus,
  Trash2,
  Lock,
  Edit2,
  AlertTriangle,
  HelpCircle,
} from "lucide-react";
import { useCategories, usePaymentMethods } from "../hooks/useCategories";
import { useCategoryMutations, usePaymentMethodMutations } from "../hooks/useCategoryMutations";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Dialog } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";
import { CategoryRead, PaymentMethodRead } from "@/lib/api/expenses";

export default function CategoryManagementView() {
  const { success, error: toastError } = useToast();
  const [activeTab, setActiveTab] = useState<"categories" | "payment-methods">("categories");

  // Load Data
  const { data: categories = [], isLoading: loadingCats } = useCategories();
  const { data: paymentMethods = [], isLoading: loadingPms } = usePaymentMethods();

  // Mutations
  const { createCategory, deleteCategory, isDeletingCategory, isCreatingCategory } = useCategoryMutations();
  const { createPaymentMethod, deletePaymentMethod, isDeletingPM, isCreatingPM } = usePaymentMethodMutations();

  // Form Input States
  const [newCatName, setNewCatName] = useState("");
  const [newPmName, setNewPmName] = useState("");

  // Modals States
  const [deleteCategoryTarget, setDeleteCategoryTarget] = useState<CategoryRead | null>(null);
  const [deletePMTarget, setDeletePMTarget] = useState<PaymentMethodRead | null>(null);

  // Submit category creation
  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    const name = newCatName.trim();
    if (!name) return;
    try {
      await createCategory({ name });
      success(`Category "${name}" created successfully`);
      setNewCatName("");
    } catch (err: any) {
      toastError(err.message || "Failed to create category");
    }
  };

  // Submit payment method creation
  const handleCreatePM = async (e: React.FormEvent) => {
    e.preventDefault();
    const name = newPmName.trim();
    if (!name) return;
    try {
      await createPaymentMethod({ name });
      success(`Payment method "${name}" created successfully`);
      setNewPmName("");
    } catch (err: any) {
      toastError(err.message || "Failed to create payment method");
    }
  };

  // Handle category deletion
  const handleDeleteCategory = async () => {
    if (!deleteCategoryTarget) return;
    try {
      await deleteCategory(deleteCategoryTarget.id);
      success(`Category "${deleteCategoryTarget.name}" deleted`);
      setDeleteCategoryTarget(null);
    } catch (err: any) {
      toastError(err.message || "Failed to delete category");
    }
  };

  // Handle payment method deletion
  const handleDeletePM = async () => {
    if (!deletePMTarget) return;
    try {
      await deletePaymentMethod(deletePMTarget.id);
      success(`Payment method "${deletePMTarget.name}" deleted`);
      setDeletePMTarget(null);
    } catch (err: any) {
      toastError(err.message || "Failed to delete payment method");
    }
  };

  return (
    <div className="page-container space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-glow">
          Settings & Metadata
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          Manage your transaction categorization rules and channels.
        </p>
      </div>

      {/* Tabs list */}
      <div className="flex border-b border-border">
        <button
          onClick={() => setActiveTab("categories")}
          className={`flex items-center space-x-2 px-6 py-3 border-b-2 font-semibold text-sm transition-colors cursor-pointer ${
            activeTab === "categories"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Tags className="h-4 w-4" />
          <span>Categories</span>
        </button>
        <button
          onClick={() => setActiveTab("payment-methods")}
          className={`flex items-center space-x-2 px-6 py-3 border-b-2 font-semibold text-sm transition-colors cursor-pointer ${
            activeTab === "payment-methods"
              ? "border-primary text-primary"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <CreditCard className="h-4 w-4" />
          <span>Payment Methods</span>
        </button>
      </div>

      {/* Categories Tab Content */}
      {activeTab === "categories" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Create category form */}
          <Card variant="glass" className="md:col-span-1 h-fit">
            <CardHeader>
              <CardTitle>Add Category</CardTitle>
              <CardDescription>Create a custom transaction group</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateCategory} className="space-y-4">
                <Input
                  label="Category Name"
                  placeholder="e.g. Subscriptions"
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                  maxLength={60}
                  required
                />
                <Button
                  type="submit"
                  fullWidth
                  className="cursor-pointer"
                  isLoading={isCreatingCategory}
                >
                  <Plus className="h-4 w-4 mr-2" /> Add Category
                </Button>
              </form>
            </CardContent>
          </Card>

          {/* Categories List */}
          <Card variant="glass" className="md:col-span-2">
            <CardHeader>
              <CardTitle>Category List</CardTitle>
              <CardDescription>
                Starter categories are protected. Custom categories can be deleted.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {loadingCats ? (
                <div className="space-y-2 animate-pulse">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="h-10 bg-zinc-800 rounded" />
                  ))}
                </div>
              ) : (
                <div className="divide-y divide-border">
                  {categories.map((c) => (
                    <div
                      key={c.id}
                      className="flex items-center justify-between py-3 first:pt-0 last:pb-0"
                    >
                      <span className="font-medium text-sm text-foreground">{c.name}</span>
                      
                      {c.is_default ? (
                        <div className="flex items-center text-xs text-muted-foreground bg-zinc-800/50 px-2 py-1 rounded">
                          <Lock className="h-3.5 w-3.5 mr-1" />
                          <span>Protected</span>
                        </div>
                      ) : (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setDeleteCategoryTarget(c)}
                          className="text-destructive hover:bg-destructive/10 hover:text-red-500 cursor-pointer"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Payment Methods Tab Content */}
      {activeTab === "payment-methods" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Create payment method form */}
          <Card variant="glass" className="md:col-span-1 h-fit">
            <CardHeader>
              <CardTitle>Add Payment Method</CardTitle>
              <CardDescription>Register a new transaction channel</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreatePM} className="space-y-4">
                <Input
                  label="Payment Method Name"
                  placeholder="e.g. Venmo"
                  value={newPmName}
                  onChange={(e) => setNewPmName(e.target.value)}
                  maxLength={60}
                  required
                />
                <Button
                  type="submit"
                  fullWidth
                  className="cursor-pointer"
                  isLoading={isCreatingPM}
                >
                  <Plus className="h-4 w-4 mr-2" /> Add Method
                </Button>
              </form>
            </CardContent>
          </Card>

          {/* Payment Methods List */}
          <Card variant="glass" className="md:col-span-2">
            <CardHeader>
              <CardTitle>Payment Method List</CardTitle>
              <CardDescription>
                Starter channels are protected. Custom channels can be deleted.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {loadingPms ? (
                <div className="space-y-2 animate-pulse">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="h-10 bg-zinc-800 rounded" />
                  ))}
                </div>
              ) : (
                <div className="divide-y divide-border">
                  {paymentMethods.map((pm) => (
                    <div
                      key={pm.id}
                      className="flex items-center justify-between py-3 first:pt-0 last:pb-0"
                    >
                      <span className="font-medium text-sm text-foreground">{pm.name}</span>
                      
                      {pm.is_default ? (
                        <div className="flex items-center text-xs text-muted-foreground bg-zinc-800/50 px-2 py-1 rounded">
                          <Lock className="h-3.5 w-3.5 mr-1" />
                          <span>Protected</span>
                        </div>
                      ) : (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setDeletePMTarget(pm)}
                          className="text-destructive hover:bg-destructive/10 hover:text-red-500 cursor-pointer"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Delete Category Confirmation Dialog */}
      <Dialog
        isOpen={!!deleteCategoryTarget}
        onClose={() => setDeleteCategoryTarget(null)}
        title="Delete Custom Category"
      >
        <div className="space-y-4">
          <div className="flex items-center space-x-3 text-amber-500 bg-amber-500/10 p-3 rounded-lg border border-amber-500/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <p className="text-xs font-medium">
              Reassignment warning: All transactions referencing this category will be reassigned to the default <strong>"Uncategorized"</strong> fallback.
            </p>
          </div>
          <p className="text-sm text-foreground">
            Are you sure you want to delete the category <strong>"{deleteCategoryTarget?.name}"</strong>?
          </p>
          <div className="flex space-x-3 pt-2">
            <Button
              variant="secondary"
              className="flex-1 cursor-pointer"
              onClick={() => setDeleteCategoryTarget(null)}
              disabled={isDeletingCategory}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              className="flex-1 cursor-pointer"
              onClick={handleDeleteCategory}
              isLoading={isDeletingCategory}
            >
              Delete
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Delete Payment Method Confirmation Dialog */}
      <Dialog
        isOpen={!!deletePMTarget}
        onClose={() => setDeletePMTarget(null)}
        title="Delete Custom Payment Method"
      >
        <div className="space-y-4">
          <div className="flex items-center space-x-3 text-amber-500 bg-amber-500/10 p-3 rounded-lg border border-amber-500/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <p className="text-xs font-medium">
              Reassignment warning: All transactions referencing this payment method will be reassigned to the default <strong>"Other"</strong> fallback.
            </p>
          </div>
          <p className="text-sm text-foreground">
            Are you sure you want to delete the payment method <strong>"{deletePMTarget?.name}"</strong>?
          </p>
          <div className="flex space-x-3 pt-2">
            <Button
              variant="secondary"
              className="flex-1 cursor-pointer"
              onClick={() => setDeletePMTarget(null)}
              disabled={isDeletingPM}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              className="flex-1 cursor-pointer"
              onClick={handleDeletePM}
              isLoading={isDeletingPM}
            >
              Delete
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
