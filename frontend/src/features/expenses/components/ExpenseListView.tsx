"use client";

import React, { useState } from "react";
import { motion } from "framer-motion";
import {
  Search,
  Plus,
  Edit2,
  Trash2,
  Calendar,
  Filter,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
  Tags,
  DollarSign,
  Receipt,
} from "lucide-react";
import { useExpenses } from "../hooks/useExpenses";
import { useExpenseMutations } from "../hooks/useExpenseMutations";
import { useCategories } from "@/features/categories/hooks/useCategories";
import { ExpenseFormDialog } from "./ExpenseFormDialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Dialog } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";
import { ExpenseRead } from "@/lib/api/expenses";

export default function ExpenseListView() {
  const { success, error: toastError } = useToast();
  const { data: categories = [] } = useCategories();
  const { deleteExpense, isDeleting } = useExpenseMutations();

  // Filters State
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [sortBy, setSortBy] = useState<"date" | "amount" | "category">("date");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("desc");
  const [page, setPage] = useState(1);

  // Dialog Modals State
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [selectedExpense, setSelectedExpense] = useState<ExpenseRead | null>(null);
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  // Load Expenses with parameters
  const { data, isLoading, isError, error } = useExpenses({
    search: search || undefined,
    category_id: categoryId || undefined,
    date_from: dateFrom || undefined,
    date_to: dateTo || undefined,
    sort_by: sortBy,
    sort_order: sortOrder,
    page,
    page_size: 10, // Page size of 10 for list display
  });

  // Mutually Exclusive Filtering Implementation
  const handleCategoryChange = (val: string) => {
    setCategoryId(val);
    setPage(1);
    if (val) {
      // Clear date range filters if category is selected
      setDateFrom("");
      setDateTo("");
    }
  };

  const handleDateChange = (type: "from" | "to", val: string) => {
    if (type === "from") setDateFrom(val);
    if (type === "to") setDateTo(val);
    setPage(1);
    if (val) {
      // Clear category filter if date range is inputted
      setCategoryId("");
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTargetId) return;
    try {
      await deleteExpense(deleteTargetId);
      success("Expense deleted successfully");
      setDeleteTargetId(null);
    } catch (err: any) {
      toastError(err.message || "Failed to delete expense");
    }
  };

  const expenses = data?.data || [];
  const meta = data?.meta;
  const totalPages = meta?.total_pages || 0;

  return (
    <div className="page-container space-y-6">
      {/* Title & Action */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-glow">Expenses</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Manage and audit your transaction records.
          </p>
        </div>
        <Button
          onClick={() => {
            setSelectedExpense(null);
            setIsFormOpen(true);
          }}
          className="cursor-pointer"
        >
          <Plus className="h-4 w-4 mr-2" /> Record Expense
        </Button>
      </div>

      {/* Filter / Search / Sort Bar */}
      <Card variant="glass" className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Search Box */}
          <div className="relative">
            <Search className="absolute left-3 top-3 h-4.5 w-4.5 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search expenses..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              className="flex h-10 w-full rounded-md border border-input bg-zinc-900/50 pl-10 pr-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
          </div>

          {/* Category Dropdown */}
          <Select
            value={categoryId}
            onChange={(e) => handleCategoryChange(e.target.value)}
            placeholder="All Categories"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>

          {/* Date range filters */}
          <div className="grid grid-cols-2 gap-2">
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => handleDateChange("from", e.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-zinc-900/50 px-2 py-2 text-xs ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2"
              title="Start Date"
            />
            <input
              type="date"
              value={dateTo}
              onChange={(e) => handleDateChange("to", e.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-zinc-900/50 px-2 py-2 text-xs ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2"
              title="End Date"
            />
          </div>

          {/* Sort selection */}
          <div className="grid grid-cols-5 gap-2">
            <div className="col-span-3">
              <Select
                value={sortBy}
                onChange={(e) => {
                  setSortBy(e.target.value as any);
                  setPage(1);
                }}
              >
                <option value="date">Sort by Date</option>
                <option value="amount">Sort by Amount</option>
                <option value="category">Sort by Category</option>
              </Select>
            </div>
            <div className="col-span-2">
              <button
                onClick={() => {
                  setSortOrder(sortOrder === "asc" ? "desc" : "asc");
                  setPage(1);
                }}
                className="flex items-center justify-center w-full h-10 rounded-md border border-input bg-zinc-900/50 text-sm hover:bg-zinc-800 transition-colors cursor-pointer"
                title="Toggle sort direction"
              >
                <ArrowUpDown className="h-4 w-4 mr-1.5" />
                <span className="uppercase text-xs font-semibold">{sortOrder}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Mutually Exclusive Reminder (If category is selected OR date is selected) */}
        {(categoryId || dateFrom || dateTo) && (
          <div className="mt-3 text-[10px] text-muted-foreground flex items-center gap-1.5">
            <Filter className="h-3.5 w-3.5 text-primary" />
            <span>
              {categoryId
                ? "Filtering exclusively by Category (Date filters cleared)"
                : "Filtering exclusively by Date Range (Category filter cleared)"}
            </span>
          </div>
        )}
      </Card>

      {/* Main Expenses Area */}
      {isLoading ? (
        <div className="space-y-4 animate-pulse">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-16 bg-zinc-800 rounded-lg" />
          ))}
        </div>
      ) : isError ? (
        <div className="flex flex-col items-center justify-center min-h-[30vh] space-y-4">
          <AlertTriangle className="h-12 w-12 text-destructive" />
          <h3 className="text-lg font-bold">Error loading expenses</h3>
          <p className="text-sm text-muted-foreground">
            {error instanceof Error ? error.message : "Backend connection error"}
          </p>
        </div>
      ) : expenses.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center text-muted-foreground border border-dashed border-border rounded-lg bg-card/20">
          <Receipt className="h-12 w-12 mb-3 opacity-30" />
          <h3 className="text-lg font-semibold">No expenses found</h3>
          <p className="text-sm mt-1 max-w-xs">
            Try adjusting your search criteria or register a new expense.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Responsive Layout: Mobile Stacked Cards vs Desktop Table */}
          <div className="md:hidden space-y-3">
            {/* Mobile Stacked Card View */}
            {expenses.map((e) => (
              <Card key={e.id} variant="glass" className="p-4 space-y-3">
                <div className="flex justify-between items-start">
                  <div className="space-y-0.5">
                    <p className="font-semibold text-sm text-foreground">{e.description || "Unspecified Expense"}</p>
                    <p className="text-xs text-muted-foreground">
                      {new Date(e.date).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" })}
                    </p>
                  </div>
                  <div className="font-mono text-sm font-bold text-foreground">
                    -${e.amount}
                  </div>
                </div>

                <div className="flex items-center justify-between border-t border-border/50 pt-2 text-xs">
                  <div className="flex space-x-1.5">
                    <span className="bg-zinc-800 px-1.5 py-0.5 rounded text-[9px] font-semibold uppercase text-zinc-300">
                      {e.category?.name || "Uncategorized"}
                    </span>
                    <span className="bg-zinc-800 px-1.5 py-0.5 rounded text-[9px] text-zinc-300">
                      {e.payment_method?.name || "Other"}
                    </span>
                  </div>
                  <div className="flex space-x-2">
                    <button
                      onClick={() => {
                        setSelectedExpense(e);
                        setIsFormOpen(true);
                      }}
                      className="p-1 text-muted-foreground hover:text-foreground cursor-pointer"
                      title="Edit"
                    >
                      <Edit2 className="h-3.5 w-3.5" />
                    </button>
                    <button
                      onClick={() => setDeleteTargetId(e.id)}
                      className="p-1 text-destructive hover:text-red-500 cursor-pointer"
                      title="Delete"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              </Card>
            ))}
          </div>

          <div className="hidden md:block overflow-hidden rounded-lg border border-border bg-card">
            {/* Desktop Table View */}
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-zinc-900 border-b border-border text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  <th className="p-4">Date</th>
                  <th className="p-4">Description</th>
                  <th className="p-4">Category</th>
                  <th className="p-4">Method</th>
                  <th className="p-4 text-right">Amount</th>
                  <th className="p-4 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border text-sm">
                {expenses.map((e) => (
                  <tr key={e.id} className="hover:bg-zinc-900/30 transition-colors">
                    <td className="p-4 whitespace-nowrap">
                      {new Date(e.date).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" })}
                    </td>
                    <td className="p-4 max-w-xs truncate font-medium">
                      {e.description || "—"}
                    </td>
                    <td className="p-4">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold uppercase bg-zinc-800 text-zinc-300">
                        {e.category?.name || "Uncategorized"}
                      </span>
                    </td>
                    <td className="p-4 text-muted-foreground">
                      {e.payment_method?.name || "Other"}
                    </td>
                    <td className="p-4 text-right font-mono font-bold">
                      -${e.amount}
                    </td>
                    <td className="p-4">
                      <div className="flex items-center justify-center space-x-3">
                        <button
                          onClick={() => {
                            setSelectedExpense(e);
                            setIsFormOpen(true);
                          }}
                          className="text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
                          title="Edit Expense"
                        >
                          <Edit2 className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setDeleteTargetId(e.id)}
                          className="text-destructive hover:text-red-500 transition-colors cursor-pointer"
                          title="Delete Expense"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center bg-card border border-border p-4 rounded-lg text-sm">
              <span className="text-muted-foreground">
                Page <span className="font-semibold text-foreground">{page}</span> of{" "}
                <span className="font-semibold text-foreground">{totalPages}</span>
              </span>
              <div className="flex space-x-2">
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => setPage((p) => Math.max(p - 1, 1))}
                  disabled={page === 1}
                  className="cursor-pointer"
                >
                  <ChevronLeft className="h-4 w-4 mr-1" /> Previous
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => setPage((p) => Math.min(p + 1, totalPages))}
                  disabled={page === totalPages}
                  className="cursor-pointer"
                >
                  Next <ChevronRight className="h-4 w-4 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Expense Modal Dialog Form */}
      <ExpenseFormDialog
        isOpen={isFormOpen}
        onClose={() => {
          setIsFormOpen(false);
          setSelectedExpense(null);
        }}
        expense={selectedExpense}
      />

      {/* Delete Confirmation Modal */}
      <Dialog
        isOpen={!!deleteTargetId}
        onClose={() => setDeleteTargetId(null)}
        title="Delete Expense"
      >
        <div className="space-y-4">
          <div className="flex items-center space-x-3 text-amber-500 bg-amber-500/10 p-3 rounded-lg border border-amber-500/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <p className="text-xs font-medium">
              This action cannot be undone. This transaction will be permanently removed.
            </p>
          </div>
          <p className="text-sm text-foreground">
            Are you sure you want to delete this expense record?
          </p>
          <div className="flex space-x-3 pt-2">
            <Button
              variant="secondary"
              className="flex-1 cursor-pointer"
              onClick={() => setDeleteTargetId(null)}
              disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              className="flex-1 cursor-pointer"
              onClick={handleDeleteConfirm}
              isLoading={isDeleting}
            >
              Delete
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
