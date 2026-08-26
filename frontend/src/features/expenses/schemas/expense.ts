import { z } from "zod";

export const expenseFormSchema = z.object({
  amount: z
    .union([z.string(), z.number()])
    .transform((val) => (typeof val === "string" ? parseFloat(val) : val))
    .refine((val) => !isNaN(val) && val > 0, {
      message: "Amount must be a positive number",
    }),
  category_id: z.string().uuid({ message: "Please select a valid category" }),
  payment_method_id: z.string().uuid({ message: "Please select a valid payment method" }),
  date: z.string().min(1, { message: "Please select a valid date" }),
  description: z.string().max(255, { message: "Description cannot exceed 255 characters" }).optional(),
});

export type ExpenseFormValues = z.infer<typeof expenseFormSchema>;
