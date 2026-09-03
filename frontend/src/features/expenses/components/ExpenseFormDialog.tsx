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
import { aiApi } from "@/lib/api/ai";
import { Sparkles, Check, Loader2, ArrowRight } from "lucide-react";

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

  // AI Feature States
  const [aiInputText, setAiInputText] = useState("");
  const [isParsingAi, setIsParsingAi] = useState(false);
  const [aiStatusMessage, setAiStatusMessage] = useState<string | null>(null);

  const [suggestedCategory, setSuggestedCategory] = useState<CategoryRead | null>(null);
  const [suggestedReason, setSuggestedReason] = useState<string | null>(null);
  const [isCategorizingAi, setIsCategorizingAi] = useState(false);

  // Synchronize form fields whenever modal opens or selected expense changes
  useEffect(() => {
    if (isOpen) {
      setLocalCategories(categories);
      setAiInputText("");
      setAiStatusMessage(null);
      setSuggestedCategory(null);
      setSuggestedReason(null);

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

  // Debounced real-time category recommendation based on description
  useEffect(() => {
    if (!description || description.trim().length < 3 || isEditMode) {
      setSuggestedCategory(null);
      setSuggestedReason(null);
      return;
    }

    const timer = setTimeout(async () => {
      try {
        setIsCategorizingAi(true);
        const res = await aiApi.categorize({
          description,
          available_categories: localCategories.map((c) => c.name),
        });
        if (res.data?.category_name) {
          const matched = localCategories.find(
            (c) => c.name.toLowerCase() === res.data.category_name.toLowerCase()
          );
          if (matched && matched.id !== categoryId) {
            setSuggestedCategory(matched);
            setSuggestedReason(res.data.reasoning || null);
          } else {
            setSuggestedCategory(null);
            setSuggestedReason(null);
          }
        }
      } catch {
        // Fail quietly on background suggestion
      } finally {
        setIsCategorizingAi(false);
      }
    }, 400);

    return () => clearTimeout(timer);
  }, [description, localCategories, categoryId, isEditMode]);

  // Handle Natural Language Quick AI Parsing
  const handleAiParse = async () => {
    if (!aiInputText.trim()) return;
    try {
      setIsParsingAi(true);
      setAiStatusMessage(null);
      const res = await aiApi.parseExpense({
        text: aiInputText,
        available_categories: localCategories.map((c) => c.name),
        available_payment_methods: paymentMethods.map((p) => p.name),
      });

      const data = res.data;
      if (data) {
        if (data.amount !== null && data.amount !== undefined) {
          setAmount(String(data.amount));
        }
        if (data.date) {
          setDate(data.date);
        }
        if (data.description) {
          setDescription(data.description);
        }
        if (data.category_name) {
          const matchedCat = localCategories.find(
            (c) => c.name.toLowerCase() === data.category_name?.toLowerCase()
          );
          if (matchedCat) setCategoryId(matchedCat.id);
        }
        if (data.payment_method_name) {
          const matchedPm = paymentMethods.find(
            (p) => p.name.toLowerCase() === data.payment_method_name?.toLowerCase()
          );
          if (matchedPm) setPaymentMethodId(matchedPm.id);
        }

        const providerLabel = data.provider_used.toUpperCase();
        setAiStatusMessage(`Filled via ${providerLabel}`);
        success(`Parsed with AI (${providerLabel})`);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Could not parse expense with AI";
      toastError(msg);
    } finally {
      setIsParsingAi(false);
    }
  };

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
        {/* Natural Language AI Quick Add (available when recording new expense) */}
        {!isEditMode && (
          <div className="rounded-xl border border-indigo-500/30 bg-gradient-to-r from-indigo-500/10 via-purple-500/5 to-transparent p-3 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-indigo-400 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                AI Quick Add
              </span>
              {aiStatusMessage && (
                <span className="text-[11px] text-emerald-400 flex items-center gap-1">
                  <Check className="w-3 h-3" /> {aiStatusMessage}
                </span>
              )}
            </div>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="e.g. 'Paid 350 for Zomato pizza via UPI' or 'Metro 40 cash'"
                value={aiInputText}
                onChange={(e) => setAiInputText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAiParse();
                  }
                }}
                className="flex-1 bg-zinc-900/80 border border-zinc-800 rounded-lg px-3 py-1.5 text-xs text-zinc-200 placeholder:text-zinc-500 focus:outline-none focus:border-indigo-500 transition"
              />
              <button
                type="button"
                onClick={handleAiParse}
                disabled={isParsingAi || !aiInputText.trim()}
                className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium transition flex items-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shrink-0"
              >
                {isParsingAi ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Sparkles className="w-3.5 h-3.5" />
                )}
                Auto-Fill
              </button>
            </div>
          </div>
        )}

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

        {/* Description field & AI Category Suggestion */}
        <div>
          <Input
            label="Description"
            placeholder="What did you buy? (e.g. Starbucks coffee, Petrol, Uber)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            error={errors.description}
          />
          {suggestedCategory && suggestedCategory.id !== categoryId && (
            <div className="mt-1.5 flex items-center justify-between rounded-lg bg-indigo-500/10 border border-indigo-500/25 px-3 py-1.5 text-xs text-indigo-300 animate-in fade-in duration-200">
              <div className="flex items-center gap-1.5 truncate">
                <Sparkles className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                <span className="truncate">
                  AI Suggests: <strong className="text-white font-medium">{suggestedCategory.name}</strong>
                  {suggestedReason && (
                    <span className="text-zinc-400 text-[11px] ml-1 hidden sm:inline">
                      ({suggestedReason})
                    </span>
                  )}
                </span>
              </div>
              <button
                type="button"
                onClick={() => {
                  setCategoryId(suggestedCategory.id);
                  setSuggestedCategory(null);
                }}
                className="px-2 py-0.5 rounded bg-indigo-600 hover:bg-indigo-500 text-[11px] text-white font-medium cursor-pointer transition shrink-0 ml-2"
              >
                Apply
              </button>
            </div>
          )}
        </div>

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
