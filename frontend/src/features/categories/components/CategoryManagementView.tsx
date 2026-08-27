"use client";

import React, { useState } from "react";
import {
  Tags,
  CreditCard,
  Plus,
  Trash2,
  Edit2,
  AlertTriangle,
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
  const {
    createCategory,
    renameCategory,
    deleteCategory,
    isDeletingCategory,
    isCreatingCategory,
    isRenamingCategory,
  } = useCategoryMutations();

  const {
    createPaymentMethod,
    renamePaymentMethod,
    deletePaymentMethod,
    isDeletingPM,
    isCreatingPM,
    isRenamingPM,
  } = usePaymentMethodMutations();

  // Form Input States
  const [newCatName, setNewCatName] = useState("");
  const [newPmName, setNewPmName] = useState("");

  // Edit States
  const [editCategoryTarget, setEditCategoryTarget] = useState<CategoryRead | null>(null);
  const [editCategoryName, setEditCategoryName] = useState("");

  const [editPMTarget, setEditPMTarget] = useState<PaymentMethodRead | null>(null);
  const [editPMName, setEditPMName] = useState("");

  // Delete States
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
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to create category";
      toastError(msg);
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
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to create payment method";
      toastError(msg);
    }
  };

  // Submit category rename/edit
  const handleEditCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editCategoryTarget) return;
    const name = editCategoryName.trim();
    if (!name) return;
    try {
      await renameCategory({ id: editCategoryTarget.id, data: { name } });
      success(`Category updated to "${name}"`);
      setEditCategoryTarget(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to update category";
      toastError(msg);
    }
  };

  // Submit payment method rename/edit
  const handleEditPM = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editPMTarget) return;
    const name = editPMName.trim();
    if (!name) return;
    try {
      await renamePaymentMethod({ id: editPMTarget.id, data: { name } });
      success(`Payment method updated to "${name}"`);
      setEditPMTarget(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to update payment method";
      toastError(msg);
    }
  };

  // Handle category deletion
  const handleDeleteCategory = async () => {
    if (!deleteCategoryTarget) return;
    try {
      await deleteCategory(deleteCategoryTarget.id);
      success(`Category "${deleteCategoryTarget.name}" deleted`);
      setDeleteCategoryTarget(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to delete category";
      toastError(msg);
    }
  };

  // Handle payment method deletion
  const handleDeletePM = async () => {
    if (!deletePMTarget) return;
    try {
      await deletePaymentMethod(deletePMTarget.id);
      success(`Payment method "${deletePMTarget.name}" deleted`);
      setDeletePMTarget(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to delete payment method";
      toastError(msg);
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
          Customize your categories and payment channels. You can add, edit, or delete any item.
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
              <CardDescription>Create a new transaction category</CardDescription>
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
                All categories can be edited or deleted freely.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {loadingCats ? (
                <div className="space-y-2 animate-pulse">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="h-10 bg-zinc-800 rounded" />
                  ))}
                </div>
              ) : categories.length === 0 ? (
                <p className="text-sm text-muted-foreground py-4">No categories found. Create one on the left.</p>
              ) : (
                <div className="divide-y divide-border">
                  {categories.map((c) => (
                    <div
                      key={c.id}
                      className="flex items-center justify-between py-3 first:pt-0 last:pb-0"
                    >
                      <span className="font-medium text-sm text-foreground">{c.name}</span>
                      
                      <div className="flex items-center space-x-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => {
                            setEditCategoryTarget(c);
                            setEditCategoryName(c.name);
                          }}
                          className="text-muted-foreground hover:text-foreground hover:bg-secondary cursor-pointer h-8 w-8 p-0"
                          title="Edit category"
                        >
                          <Edit2 className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setDeleteCategoryTarget(c)}
                          className="text-destructive hover:bg-destructive/10 hover:text-red-500 cursor-pointer h-8 w-8 p-0"
                          title="Delete category"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
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
                All payment methods can be edited or deleted freely.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {loadingPms ? (
                <div className="space-y-2 animate-pulse">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="h-10 bg-zinc-800 rounded" />
                  ))}
                </div>
              ) : paymentMethods.length === 0 ? (
                <p className="text-sm text-muted-foreground py-4">No payment methods found. Create one on the left.</p>
              ) : (
                <div className="divide-y divide-border">
                  {paymentMethods.map((pm) => (
                    <div
                      key={pm.id}
                      className="flex items-center justify-between py-3 first:pt-0 last:pb-0"
                    >
                      <span className="font-medium text-sm text-foreground">{pm.name}</span>
                      
                      <div className="flex items-center space-x-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => {
                            setEditPMTarget(pm);
                            setEditPMName(pm.name);
                          }}
                          className="text-muted-foreground hover:text-foreground hover:bg-secondary cursor-pointer h-8 w-8 p-0"
                          title="Edit payment method"
                        >
                          <Edit2 className="h-4 w-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setDeletePMTarget(pm)}
                          className="text-destructive hover:bg-destructive/10 hover:text-red-500 cursor-pointer h-8 w-8 p-0"
                          title="Delete payment method"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Edit Category Dialog */}
      <Dialog
        isOpen={!!editCategoryTarget}
        onClose={() => setEditCategoryTarget(null)}
        title="Edit Category Name"
      >
        <form onSubmit={handleEditCategory} className="space-y-4">
          <Input
            label="New Name"
            value={editCategoryName}
            onChange={(e) => setEditCategoryName(e.target.value)}
            maxLength={60}
            required
            autoFocus
          />
          <div className="flex space-x-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              className="flex-1 cursor-pointer"
              onClick={() => setEditCategoryTarget(null)}
              disabled={isRenamingCategory}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="flex-1 cursor-pointer"
              isLoading={isRenamingCategory}
            >
              Save Changes
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Edit Payment Method Dialog */}
      <Dialog
        isOpen={!!editPMTarget}
        onClose={() => setEditPMTarget(null)}
        title="Edit Payment Method Name"
      >
        <form onSubmit={handleEditPM} className="space-y-4">
          <Input
            label="New Name"
            value={editPMName}
            onChange={(e) => setEditPMName(e.target.value)}
            maxLength={60}
            required
            autoFocus
          />
          <div className="flex space-x-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              className="flex-1 cursor-pointer"
              onClick={() => setEditPMTarget(null)}
              disabled={isRenamingPM}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="flex-1 cursor-pointer"
              isLoading={isRenamingPM}
            >
              Save Changes
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Delete Category Confirmation Dialog */}
      <Dialog
        isOpen={!!deleteCategoryTarget}
        onClose={() => setDeleteCategoryTarget(null)}
        title="Delete Category"
      >
        <div className="space-y-4">
          <div className="flex items-center space-x-3 text-amber-500 bg-amber-500/10 p-3 rounded-lg border border-amber-500/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <p className="text-xs font-medium">
              Transactions referencing this category will automatically be reassigned to another available category.
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
        title="Delete Payment Method"
      >
        <div className="space-y-4">
          <div className="flex items-center space-x-3 text-amber-500 bg-amber-500/10 p-3 rounded-lg border border-amber-500/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <p className="text-xs font-medium">
              Transactions referencing this payment method will automatically be reassigned to another available payment method.
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
