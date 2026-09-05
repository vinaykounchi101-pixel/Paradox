"use client";

import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Plus, Check, Loader2, Tag, Sparkles } from "lucide-react";
import { CategoryRead } from "@/lib/api/expenses";
import { predictCategoryLocally } from "@/features/expenses/components/ExpenseFormDialog";

export interface CategoryPickerProps {
  categories: CategoryRead[];
  value: string;
  onChange: (id: string) => void;
  /** Async handler to create a new category; must return the created CategoryRead */
  onCreateCategory: (name: string) => Promise<CategoryRead>;
  isLoading?: boolean;
  error?: string;
  label?: string;
  suggestedNewCategory?: string | null;
  suggestedExistingCategory?: CategoryRead | null;
  onApplySuggestedNewCategory?: (name: string) => Promise<void>;
  isAddingNewCategory?: boolean;
}

export const CategoryPicker: React.FC<CategoryPickerProps> = ({
  categories,
  value,
  onChange,
  onCreateCategory,
  isLoading = false,
  error,
  label = "Category",
  suggestedNewCategory,
  suggestedExistingCategory,
  onApplySuggestedNewCategory,
  isAddingNewCategory = false,
}) => {
  const [showInput, setShowInput] = useState(false);
  const [newName, setNewName] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [inputError, setInputError] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  // Auto-focus the inline input when it opens
  useEffect(() => {
    if (showInput) {
      const t = setTimeout(() => inputRef.current?.focus(), 120);
      return () => clearTimeout(t);
    }
  }, [showInput]);

  const handleCreate = async () => {
    if (isSaving) return;
    const trimmed = newName.trim();
    if (!trimmed) {
      setInputError("Name cannot be empty.");
      return;
    }
    if (trimmed.length > 50) {
      setInputError("Max 50 characters.");
      return;
    }
    const dup = categories.find(
      (c) => c.name.toLowerCase() === trimmed.toLowerCase()
    );
    if (dup) {
      setInputError("Already exists — select it below.");
      return;
    }

    setIsSaving(true);
    setInputError("");
    try {
      const created = await onCreateCategory(trimmed);
      // Auto-select the new category
      onChange(created.id);
      setNewName("");
      setShowInput(false);
    } catch {
      setInputError("Failed to create category. Try again.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleCreate();
    }
    if (e.key === "Escape") {
      setShowInput(false);
      setNewName("");
      setInputError("");
    }
  };

  // Deduplicate categories by id and lower-case name to guarantee zero visual duplicates
  const deduplicated = React.useMemo(() => {
    const seenIds = new Set<string>();
    const seenNames = new Set<string>();
    const result: CategoryRead[] = [];
    for (const cat of categories) {
      const lower = cat.name.toLowerCase().trim();
      if (!seenIds.has(cat.id) && !seenNames.has(lower)) {
        seenIds.add(cat.id);
        seenNames.add(lower);
        result.push(cat);
      }
    }
    return result;
  }, [categories]);

  // Sort: defaults first, then alphabetical
  const sorted = [...deduplicated].sort((a, b) => {
    if (a.is_default && !b.is_default) return -1;
    if (!a.is_default && b.is_default) return 1;
    return a.name.localeCompare(b.name);
  });

  const selectedCat = deduplicated.find((c) => c.id === value);

  return (
    <div className="flex flex-col space-y-2 w-full">
      {/* Label */}
      {label && (
        <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {label}
        </label>
      )}

      {/* Pill grid or loading skeleton */}
      {isLoading ? (
        <div className="flex flex-wrap gap-2 py-1">
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="h-8 w-20 rounded-full bg-zinc-800 animate-pulse"
            />
          ))}
        </div>
      ) : (
        <div className="flex flex-wrap gap-2 py-1">
          {sorted.map((cat) => {
            const isSelected = value === cat.id;
            return (
              <motion.button
                key={cat.id}
                type="button"
                onClick={() => onChange(cat.id)}
                whileTap={{ scale: 0.94 }}
                className={[
                  "relative flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold",
                  "border transition-all duration-150 cursor-pointer select-none outline-none",
                  "focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1",
                  isSelected
                    ? "bg-primary border-primary text-primary-foreground shadow-[0_0_14px_rgba(99,102,241,0.45)]"
                    : "bg-zinc-900/60 border-border text-muted-foreground hover:border-primary/40 hover:text-foreground hover:bg-zinc-800/80",
                ].join(" ")}
              >
                {isSelected ? (
                  <Check className="h-3 w-3 shrink-0" />
                ) : (
                  <Tag className="h-3 w-3 shrink-0 opacity-50" />
                )}
                <span>{cat.name}</span>
              </motion.button>
            );
          })}

          {/* Suggested New Category Pill (Direct 1-click database creation and selection) */}
          {suggestedNewCategory && (
            <motion.button
              type="button"
              onClick={() => onApplySuggestedNewCategory?.(suggestedNewCategory)}
              disabled={isAddingNewCategory}
              whileHover={{ scale: 1.04 }}
              whileTap={{ scale: 0.94 }}
              className="relative flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border border-amber-500/60 bg-gradient-to-r from-amber-500/25 via-orange-500/20 to-amber-500/15 text-amber-200 shadow-[0_0_14px_rgba(245,158,11,0.35)] hover:border-amber-400 cursor-pointer select-none transition-all duration-150 animate-pulse outline-none"
              title={`Click to add "${suggestedNewCategory}" and select it`}
            >
              {isAddingNewCategory ? (
                <Loader2 className="h-3 w-3 animate-spin text-amber-400 shrink-0" />
              ) : (
                <Sparkles className="h-3 w-3 text-amber-400 shrink-0" />
              )}
              <span>+ Add &quot;{suggestedNewCategory}&quot; (Suggested)</span>
            </motion.button>
          )}

          {/* Suggested Existing Category Pill */}
          {suggestedExistingCategory && suggestedExistingCategory.id !== value && (
            <motion.button
              type="button"
              onClick={() => onChange(suggestedExistingCategory.id)}
              whileHover={{ scale: 1.04 }}
              whileTap={{ scale: 0.94 }}
              className="relative flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border border-indigo-500/60 bg-gradient-to-r from-indigo-500/25 via-purple-500/20 to-indigo-500/15 text-indigo-200 shadow-[0_0_14px_rgba(99,102,241,0.35)] hover:border-indigo-400 cursor-pointer select-none transition-all duration-150 animate-pulse outline-none"
              title={`Click to select suggested category "${suggestedExistingCategory.name}"`}
            >
              <Sparkles className="h-3 w-3 text-indigo-400 shrink-0" />
              <span>Select {suggestedExistingCategory.name} (Suggested)</span>
            </motion.button>
          )}

          {/* + New Category pill — hidden while input is open */}
          {!showInput && (
            <motion.button
              type="button"
              onClick={() => setShowInput(true)}
              whileTap={{ scale: 0.94 }}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border border-dashed border-primary/40 text-primary/70 hover:border-primary hover:text-primary hover:bg-primary/5 transition-all duration-150 cursor-pointer outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <Plus className="h-3 w-3" />
              New Category
            </motion.button>
          )}
        </div>
      )}

      {/* Inline new-category input — slides in/out */}
      <AnimatePresence>
        {showInput && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2, ease: "easeOut" }}
            className="overflow-hidden"
          >
            <div className="flex items-center gap-2 mt-1 px-3 py-2.5 rounded-xl border border-primary/25 bg-primary/5 backdrop-blur-sm">
              <Tag className="h-3.5 w-3.5 text-primary shrink-0" />
              <input
                ref={inputRef}
                type="text"
                value={newName}
                onChange={(e) => {
                  setNewName(e.target.value);
                  setInputError("");
                }}
                onKeyDown={handleKeyDown}
                placeholder="New category name…"
                maxLength={50}
                className="flex-1 bg-transparent text-sm text-foreground placeholder:text-muted-foreground outline-none min-w-0"
              />
              <div className="flex items-center gap-1.5 shrink-0">
                <motion.button
                  type="button"
                  onClick={handleCreate}
                  disabled={isSaving || !newName.trim()}
                  whileTap={{ scale: 0.95 }}
                  className="flex items-center gap-1 px-2.5 py-1 rounded-md bg-primary text-primary-foreground text-xs font-semibold disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer hover:bg-primary/90 transition-colors"
                >
                  {isSaving ? (
                    <Loader2 className="h-3 w-3 animate-spin" />
                  ) : (
                    <Check className="h-3 w-3" />
                  )}
                  Add
                </motion.button>
                <button
                  type="button"
                  onClick={() => {
                    setShowInput(false);
                    setNewName("");
                    setInputError("");
                  }}
                  className="px-2 py-1 rounded-md text-xs text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
                >
                  Cancel
                </button>
              </div>
            </div>

            {/* Intelligent Brand/Product Helper inside New Category Input */}
            {(() => {
              const brandPred = newName.trim().length >= 2 ? predictCategoryLocally(newName) : null;
              if (brandPred && brandPred.toLowerCase() !== newName.trim().toLowerCase()) {
                return (
                  <div className="flex items-center gap-2 mt-2 px-3 py-1.5 rounded-lg bg-amber-500/10 border border-amber-500/30 text-xs text-amber-200 animate-in fade-in duration-150">
                    <Sparkles className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                    <span className="truncate">
                      Recognized product/brand! Suggest category: <strong className="text-white font-semibold">{brandPred}</strong>
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        setNewName(brandPred);
                        setInputError("");
                      }}
                      className="px-2 py-0.5 rounded bg-amber-600 hover:bg-amber-500 text-white text-[11px] font-semibold transition cursor-pointer shrink-0 shadow-sm"
                    >
                      Use &quot;{brandPred}&quot;
                    </button>
                  </div>
                );
              }
              return null;
            })()}

            {inputError && (
              <p className="text-xs text-destructive font-medium mt-1.5 pl-1">
                {inputError}
              </p>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Selected summary line */}
      {selectedCat && !isLoading && (
        <p className="text-[10px] text-muted-foreground">
          Selected:{" "}
          <span className="text-foreground font-semibold">{selectedCat.name}</span>
        </p>
      )}

      {/* Field validation error */}
      {error && (
        <span className="text-xs text-destructive font-medium animate-in fade-in duration-200">
          {error}
        </span>
      )}
    </div>
  );
};
