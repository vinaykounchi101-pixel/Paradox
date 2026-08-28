"use client";

import React, { useState, useEffect } from "react";
import { useCategories, usePaymentMethods } from "@/features/categories/hooks/useCategories";
import { useExpenseMutations } from "../hooks/useExpenseMutations";
import { useCategoryMutations } from "@/features/categories/hooks/useCategoryMutations";
import { expenseFormSchema, ExpenseFormValues } from "../schemas/expense";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { PaymentMethodDropdown } from "@/components/ui/PaymentMethodDropdown";
import { CategoryPicker } from "@/components/ui/CategoryPicker";
import { useToast } from "@/components/ui/toast";
import { ExpenseRead } from "@/lib/api/expenses";
import { CategoryRead } from "@/lib/api/expenses";

interface ExpenseFormDialogProps {
  isOpen: boolean;
  onClose: () => void;
  expense?: ExpenseRead | null;
}

export const ExpenseFormDialog: React.FC<ExpenseFormDialogProps> = ({
  isOpen,
  onClose,
  expense,
}) => {
  const isEditMode = !!expense;
  const { data: categories = [], isLoading: loadingCats } = useCategories();
  const { data: paymentMethods = [], isLoading: loadingPms } = usePaymentMethods();
  const { createExpense, updateExpense, isCreating, isUpdating } = useExpenseMutations();
  const { createCategory } = useCategoryMutations();
  const { success, error: toastError } = useToast();

  // Local category list — merges server list with any newly created ones during this session
  const [localCategories, setLocalCategories] = useState<CategoryRead[]>([]);

  const [amount, setAmount] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [paymentMethodId, setPaymentMethodId] = useState("");
  const [date, setDate] = useState("");
  const [description, setDescription] = useState("");

  const [errors, setErrors] = useState<Partial<Record<keyof ExpenseFormValues, string>>>({});

  // Synchronize form fields whenever modal opens or selected expense changes
  useEffect(() => {
    if (isOpen) {
      setLocalCategories(categories);

      if (expense) {
        setAmount(String(expense.amount));

        // Ensure category exists in current categories list, else fallback to valid default
        const validCat = categories.find((c) => c.id === expense.category_id);
        const defaultCat = categories.find((c) => c.is_default) || categories[0];
        setCategoryId(validCat ? validCat.id : defaultCat?.id || "");

        // Ensure payment method exists in current paymentMethods list, else fallback to valid default
        const validPm = paymentMethods.find((p) => p.id === expense.payment_method_id);
        const defaultPm = paymentMethods.find((p) => p.is_default) || paymentMethods[0];
        setPaymentMethodId(validPm ? validPm.id : defaultPm?.id || "");

        setDate(expense.date ? expense.date.split("T")[0] : "");
        setDescription(expense.description || "");
      } else {
        setAmount("");
        const defaultCat = categories.find((c) => c.is_default) || categories[0];
        const defaultPm = paymentMethods.find((p) => p.is_default) || paymentMethods[0];
        setCategoryId(defaultCat?.id || "");
        setPaymentMethodId(defaultPm?.id || "");
        setDate(new Date().toISOString().split("T")[0]);
        setDescription("");
      }
      setErrors({});
    }
  }, [isOpen, expense, categories, paymentMethods]);

  // Called by CategoryPicker when user creates a new category inline
  const handleCreateCategory = async (name: string): Promise<CategoryRead> => {
    const response = await createCategory({ name });
    const created: CategoryRead = response.data;
    setLocalCategories((prev) => [...prev, created]);
    return created;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});

    const formData = {
      amount,
      category_id: categoryId,
      payment_method_id: paymentMethodId,
      date,
      description: description || undefined,
    };

    const result = expenseFormSchema.safeParse(formData);
    if (!result.success) {
      const fieldErrors: Partial<Record<keyof ExpenseFormValues, string>> = {};
      result.error.errors.forEach((err) => {
        const path = err.path[0] as keyof ExpenseFormValues;
        fieldErrors[path] = err.message;
      });
      setErrors(fieldErrors);
      return;
    }

    try {
      if (isEditMode && expense) {
        await updateExpense({
          id: expense.id,
          data: {
            amount: result.data.amount,
            category_id: result.data.category_id,
            payment_method_id: result.data.payment_method_id,
            date: result.data.date,
            description: result.data.description,
          },
        });
        success("Expense updated successfully");
      } else {
        await createExpense({
          amount: result.data.amount,
          category_id: result.data.category_id,
          payment_method_id: result.data.payment_method_id,
          date: result.data.date,
          description: result.data.description,
        });
        success("Expense recorded successfully");
      }
      onClose();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Failed to save expense";
      toastError(message);
    }
  };

  return (
    <Dialog
      isOpen={isOpen}
      onClose={onClose}
      title={isEditMode ? "Edit Expense" : "Record Expense"}
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        {/* Amount field */}
        <Input
          label="Amount ($)"
          type="number"
          step="0.01"
          min="0.01"
          placeholder="0.00"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          error={errors.amount}
          required
        />

        {/* Date selection */}
        <Input
          label="Transaction Date"
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          error={errors.date}
          required
        />

        {/* Category — premium pill picker with inline creation */}
        <CategoryPicker
          label="Category"
          categories={localCategories}
          value={categoryId}
          onChange={setCategoryId}
          onCreateCategory={handleCreateCategory}
          isLoading={loadingCats}
          error={errors.category_id}
        />

        {/* Payment Method Custom Animated Dropdown Menu */}
        <PaymentMethodDropdown
          label="Payment Method"
          paymentMethods={paymentMethods}
          value={paymentMethodId}
          onChange={setPaymentMethodId}
          isLoading={loadingPms}
          error={errors.payment_method_id}
        />

        {/* Description field */}
        <Input
          label="Description"
          placeholder="What did you buy?"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          error={errors.description}
        />

        {/* Submit actions */}
        <div className="flex space-x-3 pt-2">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            className="flex-1 cursor-pointer"
            disabled={isCreating || isUpdating}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            className="flex-1 cursor-pointer"
            isLoading={isCreating || isUpdating}
          >
            {isEditMode ? "Save Changes" : "Record"}
          </Button>
        </div>
      </form>
    </Dialog>
  );
};
