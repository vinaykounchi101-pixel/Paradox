"use client";

import React, { useState, useEffect, useRef } from "react";
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
import { aiApi, AnalyzeSentimentResponse } from "@/lib/api/ai";
import { useCurrency } from "@/features/auth/context/CurrencyContext";
import {
  Sparkles,
  Check,
  Loader2,
  ArrowRight,
  Camera,
  Mic,
  MicOff,
  MessageSquare,
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Plus,
} from "lucide-react";

interface ExpenseFormDialogProps {
  isOpen: boolean;
  onClose: () => void;
  expense?: ExpenseRead | null;
}

const PREDICTIVE_CATEGORIES: Record<string, string[]> = {
  "Alcohol": [
    "brandy", "whiskey", "whisky", "beer", "wine", "vodka", "rum", "gin", "tequila",
    "liquor", "alcohol", "bar", "pub", "cocktail", "scotch", "champagne", "bourbon",
    "brewery", "theka", "daaru", "drinks", "corona", "budweiser", "bira", "kingfisher",
    "bacardi", "old monk", "smirnoff", "absolut", "jack daniels", "johnnie walker"
  ],
  "Gaming": [
    "ps5", "playstation", "playstation 5", "ps4", "ps3", "xbox", "xbox series",
    "nintendo", "switch", "gaming", "steam", "game", "games", "epic games", "gta",
    "fifa", "valorant", "console", "controller", "pc gaming", "roblox", "minecraft",
    "steam deck", "bgmi", "pubg", "game pass", "dualsense", "dualshock"
  ],
  "Food & Dining": [
    "swiggy", "zomato", "restaurant", "cafe", "coffee", "tea", "chai", "lunch", "dinner",
    "breakfast", "snack", "burger", "pizza", "starbucks", "biryani", "bakery", "mcdonald",
    "kfc", "dominos", "subway", "eats", "dosa", "idli", "shawarma"
  ],
  "Groceries": [
    "blinkit", "zepto", "instamart", "grocery", "groceries", "supermarket", "milk",
    "bread", "eggs", "vegetables", "fruits", "veggies", "provisions", "ration", "mart"
  ],
  "Transportation": [
    "uber", "ola", "auto", "cab", "rickshaw", "metro", "bus", "train", "flight",
    "petrol", "diesel", "fuel", "cng", "parking", "fastag", "toll", "rapido"
  ],
  "Bills & Utilities": [
    "electricity", "water", "gas", "wifi", "internet", "broadband", "mobile recharge",
    "phone bill", "cylinder", "dth", "power", "airtel", "jio", "vi", "piped gas"
  ],
  "Healthcare": [
    "doctor", "hospital", "clinic", "medicine", "medicines", "pharmacy", "medical",
    "apollo", "pharmeasy", "dentist", "tablets", "syrup", "1mg", "health checkup"
  ],
  "Shopping": [
    "clothes", "shoes", "amazon", "flipkart", "myntra", "shopping", "mall", "zara",
    "h&m", "electronics", "gadget", "headphones", "laptop", "ajio", "meesho"
  ],
  "Subscriptions": [
    "netflix", "spotify", "prime", "youtube", "hotstar", "subscription", "patreon",
    "apple music", "disney", "chatgpt"
  ],
  "Fitness": [
    "gym", "fitness", "yoga", "workout", "protein", "whey", "creatine", "cult", "crossfit"
  ],
  "Pet Care": [
    "dog", "cat", "pet", "pets", "puppy", "kitten", "pedigree", "veterinary", "vet", "whiskas", "royal canin"
  ],
  "Education": [
    "tuition", "course", "udemy", "coursera", "school", "college", "fees", "books"
  ],
  "Housing": [
    "rent", "brokerage", "maintenance", "maid", "repair", "plumber", "electrician"
  ],
  "Investments": [
    "stocks", "sip", "mutual fund", "crypto", "shares", "gold", "zerodha", "groww"
  ],
};

// Local dictionary prediction for instant 0ms category auto-detection
const predictCategoryLocally = (desc: string): string | null => {
  const clean = desc.toLowerCase().trim();
  if (clean.length < 2) return null;
  // Match single words and compound terms
  const words = clean.split(/[\s,._\-/]+/);

  for (const [catName, keywords] of Object.entries(PREDICTIVE_CATEGORIES)) {
    for (const kw of keywords) {
      if (clean === kw || words.includes(kw) || (kw.length >= 4 && clean.includes(kw))) {
        return catName;
      }
    }
  }
  return null;
};

// Helper to intelligently resolve categories including common synonyms and aliases
const matchCategory = (
  targetName?: string | null,
  availableCats: CategoryRead[] = []
): CategoryRead | undefined => {
  if (!targetName || availableCats.length === 0) return undefined;
  const target = targetName.toLowerCase().trim();

  // 1. Exact match
  let found = availableCats.find((c) => c.name.toLowerCase() === target);
  if (found) return found;

  // 2. Gaming specific match (preserve distinct Gaming category; do not coerce into Entertainment)
  if (
    target === "gaming" ||
    target === "games" ||
    target.includes("playstation") ||
    target.includes("xbox") ||
    target.includes("nintendo") ||
    target.includes("steam")
  ) {
    found = availableCats.find((c) => c.name.toLowerCase().includes("gaming") || c.name.toLowerCase().includes("game"));
    if (found) return found;
  }

  // 3. Alcohol / Liquor specific match (preserve distinct Alcohol category; do not coerce into Food)
  if (
    target === "alcohol" ||
    target === "liquor" ||
    target === "drinks" ||
    target === "bar" ||
    target === "pub" ||
    target.includes("brandy") ||
    target.includes("whiskey") ||
    target.includes("beer") ||
    target.includes("wine") ||
    target.includes("vodka")
  ) {
    found = availableCats.find(
      (c) =>
        c.name.toLowerCase().includes("alcohol") ||
        c.name.toLowerCase().includes("liquor") ||
        c.name.toLowerCase().includes("drinks") ||
        c.name.toLowerCase().includes("beverage")
    );
    if (found) return found;
  }

  // 4. Known Semantic / Synonym Aliases
  if (
    target.includes("food") ||
    target.includes("dining") ||
    target.includes("restaurant") ||
    target.includes("cafe") ||
    target.includes("snack") ||
    target.includes("lunch") ||
    target.includes("dinner") ||
    target.includes("breakfast") ||
    target.includes("pizza") ||
    target.includes("burger") ||
    target.includes("zomato") ||
    target.includes("swiggy")
  ) {
    found = availableCats.find(
      (c) => c.name.toLowerCase().includes("food") || c.name.toLowerCase().includes("dining")
    );
    if (found) return found;
  }

  if (
    target.includes("grocer") ||
    target.includes("supermarket") ||
    target.includes("vegetable") ||
    target.includes("fruit") ||
    target.includes("milk") ||
    target.includes("blinkit") ||
    target.includes("zepto") ||
    target.includes("instamart")
  ) {
    found = availableCats.find((c) => c.name.toLowerCase().includes("grocer"));
    if (found) return found;
    found = availableCats.find((c) => c.name.toLowerCase().includes("food"));
    if (found) return found;
  }

  if (
    target.includes("utilit") ||
    target.includes("bill") ||
    target.includes("electric") ||
    target.includes("water") ||
    target.includes("gas") ||
    target.includes("wifi") ||
    target.includes("internet") ||
    target.includes("broadband") ||
    target.includes("mobile") ||
    target.includes("recharge")
  ) {
    found = availableCats.find(
      (c) => c.name.toLowerCase().includes("utilit") || c.name.toLowerCase().includes("bill")
    );
    if (found) return found;
  }

  if (
    target.includes("transport") ||
    target.includes("travel") ||
    target.includes("cab") ||
    target.includes("taxi") ||
    target.includes("uber") ||
    target.includes("ola") ||
    target.includes("fuel") ||
    target.includes("petrol") ||
    target.includes("diesel") ||
    target.includes("metro") ||
    target.includes("auto") ||
    target.includes("bus")
  ) {
    found = availableCats.find((c) => c.name.toLowerCase().includes("transport"));
    if (found) return found;
  }

  if (
    target.includes("health") ||
    target.includes("medic") ||
    target.includes("doctor") ||
    target.includes("clinic") ||
    target.includes("pharmacy") ||
    target.includes("hospital")
  ) {
    found = availableCats.find(
      (c) => c.name.toLowerCase().includes("health") || c.name.toLowerCase().includes("medic")
    );
    if (found) return found;
  }

  if (
    target.includes("shop") ||
    target.includes("cloth") ||
    target.includes("mall") ||
    target.includes("amazon") ||
    target.includes("flipkart") ||
    target.includes("myntra")
  ) {
    found = availableCats.find((c) => c.name.toLowerCase().includes("shop"));
    if (found) return found;
  }

  if (
    target.includes("entertain") ||
    target.includes("movie") ||
    target.includes("cinema") ||
    target.includes("netflix") ||
    target.includes("spotify")
  ) {
    found = availableCats.find((c) => c.name.toLowerCase().includes("entertain"));
    if (found) return found;
  }

  if (
    target.includes("educat") ||
    target.includes("school") ||
    target.includes("course") ||
    target.includes("book") ||
    target.includes("tuition")
  ) {
    found = availableCats.find((c) => c.name.toLowerCase().includes("educat"));
    if (found) return found;
  }

  if (
    target.includes("other") ||
    target.includes("uncategorized") ||
    target.includes("misc")
  ) {
    found = availableCats.find(
      (c) => c.name.toLowerCase().includes("uncategorized") || c.name.toLowerCase().includes("other")
    );
    if (found) return found;
  }

  // 5. Substring match fallback
  found = availableCats.find(
    (c) => c.name.toLowerCase().includes(target) || target.includes(c.name.toLowerCase())
  );
  return found;
};

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
  const [isRecurring, setIsRecurring] = useState(false);
  const [recurringFrequency, setRecurringFrequency] = useState("monthly");
  const [isScanningReceipt, setIsScanningReceipt] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { currencySymbol, formatCurrency } = useCurrency();

  const [errors, setErrors] = useState<Partial<Record<keyof ExpenseFormValues, string>>>({});

  // AI Feature States
  const [aiInputText, setAiInputText] = useState("");
  const [isParsingAi, setIsParsingAi] = useState(false);
  const [aiStatusMessage, setAiStatusMessage] = useState<string | null>(null);

  const [suggestedCategory, setSuggestedCategory] = useState<CategoryRead | null>(null);
  const [suggestedNewCategory, setSuggestedNewCategory] = useState<string | null>(null);
  const [isAddingNewCategory, setIsAddingNewCategory] = useState(false);
  const [suggestedReason, setSuggestedReason] = useState<string | null>(null);
  const [isCategorizingAi, setIsCategorizingAi] = useState(false);

  // Voice Speech Recognition State
  const [isListening, setIsListening] = useState(false);
  const recognitionRef = useRef<any>(null);

  // SMS Parser State
  const [showSmsBox, setShowSmsBox] = useState(false);
  const [smsInputText, setSmsInputText] = useState("");
  const [isParsingSms, setIsParsingSms] = useState(false);

  // Duplicate Detection State
  const [duplicateWarning, setDuplicateWarning] = useState<string | null>(null);

  // Emotional Sentiment & Buyer's Remorse Reflection State
  const [sentimentResult, setSentimentResult] = useState<AnalyzeSentimentResponse | null>(null);
  const [showSentimentTip, setShowSentimentTip] = useState(true);

  // Track dialog open transition to avoid clearing fields during background re-renders
  const prevIsOpenRef = useRef(false);

  useEffect(() => {
    if (isOpen && !prevIsOpenRef.current) {
      // Newly opened dialog: initialize clean form state
      setLocalCategories(categories);
      setAiInputText("");
      setAiStatusMessage(null);
      setSuggestedCategory(null);
      setSuggestedNewCategory(null);
      setIsAddingNewCategory(false);
      setSuggestedReason(null);
      setSentimentResult(null);
      setShowSentimentTip(true);

      if (expense) {
        setAmount(String(expense.amount));
        const validCat = categories.find((c) => c.id === expense.category_id);
        const defaultCat = categories.find((c) => c.is_default) || categories[0];
        setCategoryId(validCat ? validCat.id : defaultCat?.id || "");

        const validPm = paymentMethods.find((p) => p.id === expense.payment_method_id);
        const defaultPm = paymentMethods.find((p) => p.is_default) || paymentMethods[0];
        setPaymentMethodId(validPm ? validPm.id : defaultPm?.id || "");

        setDate(expense.date ? expense.date.split("T")[0] : "");
        setDescription(expense.description || "");
        setIsRecurring(Boolean(expense.is_recurring));
        setRecurringFrequency(expense.recurring_frequency || "monthly");
      } else {
        setAmount("");
        const defaultCat = categories.find((c) => c.is_default) || categories[0];
        const defaultPm = paymentMethods.find((p) => p.is_default) || paymentMethods[0];
        setCategoryId(defaultCat?.id || "");
        setPaymentMethodId(defaultPm?.id || "");
        setDate(new Date().toISOString().split("T")[0]);
        setDescription("");
        setIsRecurring(false);
        setRecurringFrequency("monthly");
      }
      setErrors({});
    } else if (isOpen && categories.length > 0) {
      // Merge newly fetched categories into localCategories without duplicates
      setLocalCategories((prev) => {
        const seen = new Set<string>();
        const merged: CategoryRead[] = [];
        for (const cat of [...categories, ...prev]) {
          const lower = cat.name.toLowerCase().trim();
          if (!seen.has(cat.id) && !seen.has(lower)) {
            seen.add(cat.id);
            seen.add(lower);
            merged.push(cat);
          }
        }
        return merged;
      });
      if (!categoryId) {
        const defaultCat = categories.find((c) => c.is_default) || categories[0];
        if (defaultCat) setCategoryId(defaultCat.id);
      }
      if (!paymentMethodId) {
        const defaultPm = paymentMethods.find((p) => p.is_default) || paymentMethods[0];
        if (defaultPm) setPaymentMethodId(defaultPm.id);
      }
    }
    prevIsOpenRef.current = isOpen;
  }, [isOpen, expense, categories, paymentMethods, categoryId, paymentMethodId, localCategories.length]);

  // Real-time category recommendation based on description (Instant Local + Debounced AI)
  useEffect(() => {
    if (!description || description.trim().length < 2 || isEditMode) {
      setSuggestedCategory(null);
      setSuggestedNewCategory(null);
      setSuggestedReason(null);
      return;
    }

    const activeCats = localCategories.length > 0 ? localCategories : categories;

    // Step 1: Instant 0ms local dictionary prediction
    const localPred = predictCategoryLocally(description);
    if (localPred) {
      const matched = matchCategory(localPred, activeCats);
      if (matched) {
        if (matched.id !== categoryId) {
          setSuggestedCategory(matched);
          setSuggestedReason(`Matches "${description.trim()}"`);
          setSuggestedNewCategory(null);
        } else {
          setSuggestedCategory(null);
          setSuggestedNewCategory(null);
          setSuggestedReason(null);
        }
      } else {
        // Suggested category does not exist in user's category list yet
        setSuggestedCategory(null);
        setSuggestedNewCategory(localPred);
        setSuggestedReason(`Matches "${description.trim()}"`);
      }
    }

    // Step 2: Debounced AI fallback for natural language phrases or non-dictionary terms
    const timer = setTimeout(async () => {
      try {
        setIsCategorizingAi(true);
        const res = await aiApi.categorize({
          description,
          available_categories: activeCats.map((c) => c.name),
        });
        if (res.data?.category_name) {
          const matched = matchCategory(res.data.category_name, activeCats);
          if (matched) {
            if (matched.id !== categoryId) {
              setSuggestedCategory(matched);
              setSuggestedReason(res.data.reasoning || `AI matched "${res.data.category_name}"`);
            } else {
              setSuggestedCategory(null);
              setSuggestedReason(null);
            }
            setSuggestedNewCategory(null);
          } else {
            // Category suggested by AI does NOT exist in user's category list!
            setSuggestedCategory(null);
            setSuggestedNewCategory(res.data.category_name);
            setSuggestedReason(res.data.reasoning || `AI suggests new category`);
          }
        }
      } catch {
        // Fail quietly on background suggestion
      } finally {
        setIsCategorizingAi(false);
      }
    }, 400);

    return () => clearTimeout(timer);
  }, [description, localCategories, categories, categoryId, isEditMode]);

  // Debounced duplicate detection guard
  useEffect(() => {
    if (isEditMode || !amount || isNaN(Number(amount)) || Number(amount) <= 0 || !date) {
      setDuplicateWarning(null);
      return;
    }

    const timer = setTimeout(async () => {
      try {
        const res = await aiApi.checkDuplicate({
          amount: Number(amount),
          date,
          description: description || undefined,
          window_days: 2,
        });
        if (res.data?.has_duplicate) {
          setDuplicateWarning(res.data.message || "Potential duplicate transaction detected.");
        } else {
          setDuplicateWarning(null);
        }
      } catch {
        setDuplicateWarning(null);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [amount, date, description, isEditMode]);

  // Debounced Emotional Sentiment & Buyer's Remorse Analysis
  useEffect(() => {
    if (!description || description.trim().length < 4 || isEditMode) {
      setSentimentResult(null);
      return;
    }

    const timer = setTimeout(async () => {
      try {
        const res = await aiApi.analyzeSentiment({
          text: description,
          amount: amount && !isNaN(Number(amount)) ? Number(amount) : undefined,
        });
        if (res.data) {
          setSentimentResult(res.data);
          setShowSentimentTip(true);
        }
      } catch {
        // Silent fallback
      }
    }, 600);

    return () => clearTimeout(timer);
  }, [description, amount, isEditMode]);

  // Helper to map and set payment method
  const matchAndSetPaymentMethod = (pmTargetRaw?: string | null) => {
    if (!pmTargetRaw) return;
    const pmTarget = pmTargetRaw.toLowerCase().trim();
    let matchedPm = paymentMethods.find((p) => p.name.toLowerCase() === pmTarget);
    if (!matchedPm) {
      if (
        pmTarget === "upi" ||
        pmTarget.includes("pay") ||
        pmTarget.includes("wallet") ||
        pmTarget.includes("phonepe") ||
        pmTarget.includes("gpay") ||
        pmTarget.includes("paytm")
      ) {
        matchedPm = paymentMethods.find((p) => {
          const n = p.name.toLowerCase();
          return n.includes("digital wallet") || n.includes("wallet") || n.includes("upi") || n.includes("pay");
        });
      } else if (
        pmTarget.includes("bank") ||
        pmTarget.includes("net") ||
        pmTarget.includes("transfer") ||
        pmTarget.includes("neft") ||
        pmTarget.includes("imps")
      ) {
        matchedPm = paymentMethods.find((p) => {
          const n = p.name.toLowerCase();
          return n.includes("bank") || n.includes("transfer") || n.includes("net");
        });
      } else if (pmTarget.includes("credit")) {
        matchedPm = paymentMethods.find((p) => p.name.toLowerCase().includes("credit"));
      } else if (pmTarget.includes("debit") || pmTarget.includes("atm")) {
        matchedPm = paymentMethods.find((p) => p.name.toLowerCase().includes("debit"));
      } else if (pmTarget.includes("card")) {
        matchedPm = paymentMethods.find((p) => p.name.toLowerCase().includes("card"));
      } else if (pmTarget.includes("cash")) {
        matchedPm = paymentMethods.find((p) => p.name.toLowerCase().includes("cash"));
      }
    }
    if (!matchedPm) {
      matchedPm = paymentMethods.find(
        (p) => p.name.toLowerCase().includes(pmTarget) || pmTarget.includes(p.name.toLowerCase())
      );
    }
    if (matchedPm) {
      setPaymentMethodId(matchedPm.id);
    }
  };

  // Handle Natural Language Quick AI Parsing
  const executeAiParse = async (textToParse: string) => {
    if (!textToParse.trim()) return;
    try {
      setIsParsingAi(true);
      setAiStatusMessage(null);
      const activeCats = localCategories.length > 0 ? localCategories : categories;
      const res = await aiApi.parseExpense({
        text: textToParse,
        available_categories: activeCats.map((c) => c.name),
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
          const matchedCat = matchCategory(data.category_name, activeCats);
          if (matchedCat) {
            setCategoryId(matchedCat.id);
            setSuggestedNewCategory(null);
          } else {
            setSuggestedNewCategory(data.category_name);
          }
        }
        if (data.payment_method_name) {
          matchAndSetPaymentMethod(data.payment_method_name);
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

  const handleAiParse = () => executeAiParse(aiInputText);

  // Web Speech API Voice Recognition Toggle
  const toggleVoiceInput = () => {
    const SpeechRecognitionAPI =
      typeof window !== "undefined"
        ? (window as unknown as { SpeechRecognition?: any; webkitSpeechRecognition?: any }).SpeechRecognition ||
          (window as unknown as { SpeechRecognition?: any; webkitSpeechRecognition?: any }).webkitSpeechRecognition
        : null;

    if (!SpeechRecognitionAPI) {
      toastError("Voice input is not supported in this browser. Please try Chrome, Edge, or Safari.");
      return;
    }

    if (isListening) {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
      setIsListening(false);
      return;
    }

    try {
      const recognition = new SpeechRecognitionAPI();
      recognition.lang = "en-IN";
      recognition.interimResults = false;
      recognition.maxAlternatives = 1;

      recognition.onstart = () => {
        setIsListening(true);
        setAiStatusMessage("Listening... Speak now");
      };

      recognition.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        setAiInputText(transcript);
        setIsListening(false);
        setAiStatusMessage(`Heard: "${transcript}"`);
        executeAiParse(transcript);
      };

      recognition.onerror = (event: any) => {
        setIsListening(false);
        if (event.error !== "no-speech") {
          toastError(`Voice error: ${event.error}`);
        }
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognition;
      recognition.start();
    } catch {
      setIsListening(false);
      toastError("Could not access microphone.");
    }
  };

  // Indian Bank / UPI SMS Parsing
  const handleSmsParse = async () => {
    if (!smsInputText.trim()) return;
    try {
      setIsParsingSms(true);
      const activeCats = localCategories.length > 0 ? localCategories : categories;
      const res = await aiApi.parseSms({
        text: smsInputText,
        available_categories: activeCats.map((c) => c.name),
        available_payment_methods: paymentMethods.map((p) => p.name),
      });

      const data = res.data;
      if (data) {
        if (data.amount) setAmount(String(data.amount));
        if (data.date) setDate(data.date);
        if (data.merchant) setDescription(data.merchant);
        if (data.category_name) {
          const matchedCat = matchCategory(data.category_name, activeCats);
          if (matchedCat) {
            setCategoryId(matchedCat.id);
            setSuggestedNewCategory(null);
          } else {
            setSuggestedNewCategory(data.category_name);
          }
        }
        if (data.payment_method_name) {
          matchAndSetPaymentMethod(data.payment_method_name);
        }
        setShowSmsBox(false);
        setSmsInputText("");
        setAiStatusMessage(`SMS Parsed: ${data.merchant || "Transaction"} (${data.amount ? formatCurrency(data.amount) : ""})`);
        success(`SMS parsed successfully (${data.provider_used})!`);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to parse SMS";
      toastError(msg);
    } finally {
      setIsParsingSms(false);
    }
  };

// Client-side image downscaler to guarantee fast uploads (< 200KB) and zero timeout
const resizeReceiptImage = (file: File, maxDim = 1280): Promise<File> => {
  return new Promise((resolve) => {
    if (!file.type.startsWith("image/")) return resolve(file);
    const img = new Image();
    const objectUrl = URL.createObjectURL(file);
    img.onload = () => {
      URL.revokeObjectURL(objectUrl);
      let { width, height } = img;
      if (width <= maxDim && height <= maxDim) {
        return resolve(file);
      }
      if (width > height) {
        height = Math.round((height * maxDim) / width);
        width = maxDim;
      } else {
        width = Math.round((width * maxDim) / height);
        height = maxDim;
      }
      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d");
      if (!ctx) return resolve(file);
      ctx.drawImage(img, 0, 0, width, height);
      canvas.toBlob(
        (blob) => {
          if (!blob) return resolve(file);
          resolve(new File([blob], file.name.replace(/\.[^/.]+$/, ".jpg"), { type: "image/jpeg" }));
        },
        "image/jpeg",
        0.85
      );
    };
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(file);
    };
    img.src = objectUrl;
  });
};

  const handleReceiptFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      setIsScanningReceipt(true);
      setAiStatusMessage("Scanning receipt with Gemini Vision...");
      const optimizedFile = await resizeReceiptImage(file);
      const res = await aiApi.scanReceipt(optimizedFile);
      const data = (res as any)?.data || res;
      if (data) {
        const validAmount =
          data.amount !== undefined &&
          data.amount !== null &&
          !isNaN(Number(data.amount)) &&
          Number(data.amount) > 0;

        if (validAmount) {
          setAmount(String(data.amount));
        }
        if (data.date) {
          setDate(data.date);
        }
        if (data.description) {
          setDescription(data.description);
          setAiInputText(data.description);
        }
        const activeCats = localCategories.length > 0 ? localCategories : categories;
        if (data.category_name) {
          const matched = matchCategory(data.category_name, activeCats);
          if (matched) {
            setCategoryId(matched.id);
            setSuggestedNewCategory(null);
          } else {
            setSuggestedNewCategory(data.category_name);
          }
        }
        if (data.payment_method_name) {
          matchAndSetPaymentMethod(data.payment_method_name);
        }

        const providerLabel = (data.provider_used || "Gemini").toUpperCase();
        if (validAmount) {
          setAiStatusMessage(`Auto-filled: ${data.description || "Bill"} (${formatCurrency(data.amount)})`);
          success(`Bill scanned & auto-filled with AI (${providerLabel})!`);
        } else {
          setAiStatusMessage(`Scanned: ${data.description || "Bill"}. Enter amount.`);
          success(`Bill scanned with AI (${providerLabel})! Please enter verified amount.`);
        }
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to scan receipt image";
      toastError(msg);
      setAiStatusMessage(null);
    } finally {
      setIsScanningReceipt(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  // Called by CategoryPicker when user creates a new category inline
  const handleCreateCategory = async (name: string): Promise<CategoryRead> => {
    const response = await createCategory({ name });
    const created: CategoryRead = response.data;
    setLocalCategories((prev) => {
      const lower = created.name.toLowerCase().trim();
      if (prev.some((c) => c.id === created.id || c.name.toLowerCase().trim() === lower)) {
        return prev;
      }
      return [...prev, created];
    });
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
            is_recurring: isRecurring,
            recurring_frequency: isRecurring ? recurringFrequency : undefined,
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
          is_recurring: isRecurring,
          recurring_frequency: isRecurring ? recurringFrequency : undefined,
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
          <div className="w-full max-w-full overflow-hidden rounded-xl border border-indigo-500/30 bg-gradient-to-r from-indigo-500/10 via-purple-500/5 to-transparent p-2.5 sm:p-3 space-y-2.5">
            <div className="flex items-center justify-between gap-2 min-w-0">
              <span className="text-xs font-semibold uppercase tracking-wider text-indigo-400 flex items-center gap-1.5 shrink-0">
                <Sparkles className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                AI Quick Add
              </span>
              {aiStatusMessage && (
                <span className="text-[11px] text-emerald-400 flex items-center gap-1 min-w-0 max-w-[170px] sm:max-w-xs text-right">
                  <Check className="w-3 h-3 shrink-0" />
                  <span className="truncate">{aiStatusMessage}</span>
                </span>
              )}
            </div>

            {/* Row 1: Full-width Quick Add Input with embedded Voice button + Auto-Fill submit button */}
            <div className="flex items-center gap-1.5 sm:gap-2 w-full min-w-0">
              <div className="relative flex-1 min-w-0">
                <input
                  type="text"
                  placeholder="e.g. 'Paid 350 for Zomato via UPI' or 'Petrol 500'"
                  value={aiInputText}
                  onChange={(e) => setAiInputText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      handleAiParse();
                    }
                  }}
                  className="w-full min-w-0 bg-zinc-900/90 border border-zinc-800 rounded-lg pl-2.5 sm:pl-3 pr-8 py-2 text-xs text-zinc-200 placeholder:text-zinc-500 focus:outline-none focus:border-indigo-500 transition"
                />
                <button
                  type="button"
                  onClick={toggleVoiceInput}
                  disabled={isParsingAi}
                  className={`absolute right-1 top-1/2 -translate-y-1/2 p-1 rounded-md transition cursor-pointer ${
                    isListening
                      ? "bg-rose-500/20 text-rose-300 border border-rose-500/50 animate-pulse"
                      : "text-zinc-400 hover:text-white hover:bg-zinc-800/80"
                  }`}
                  title={isListening ? "Listening... Click to stop" : "Speak your expense (Voice Quick-Add)"}
                >
                  {isListening ? (
                    <MicOff className="w-3.5 h-3.5 text-rose-400" />
                  ) : (
                    <Mic className="w-3.5 h-3.5" />
                  )}
                </button>
              </div>

              <button
                type="button"
                onClick={handleAiParse}
                disabled={isParsingAi || !aiInputText.trim()}
                className="px-2.5 sm:px-3 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition flex items-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer shrink-0 shadow-sm"
              >
                {isParsingAi ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Sparkles className="w-3.5 h-3.5" />
                )}
                <span>Auto-Fill</span>
              </button>
            </div>

            {/* Row 2: Secondary Quick Add Tools with strict 2-column grid to guarantee zero overflow */}
            <div className="grid grid-cols-2 gap-1.5 sm:gap-2 w-full min-w-0 pt-0.5">
              {/* Hidden file input for receipt photo upload */}
              <input
                type="file"
                ref={fileInputRef}
                accept="image/*"
                className="hidden"
                onChange={handleReceiptFileChange}
              />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isScanningReceipt || isParsingAi}
                className="w-full min-w-0 justify-center px-2 py-1.5 rounded-lg bg-indigo-500/15 hover:bg-indigo-500/25 text-indigo-300 hover:text-white text-xs font-medium transition flex items-center gap-1.5 border border-indigo-500/35 cursor-pointer disabled:opacity-50 truncate"
                title="Scan receipt, bill, or invoice image using Gemini Vision OCR"
              >
                {isScanningReceipt ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-400 shrink-0" />
                ) : (
                  <Camera className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                )}
                <span className="truncate">{isScanningReceipt ? "Scanning..." : "Scan Bill"}</span>
              </button>

              {/* Bank SMS Paste Toggle */}
              <button
                type="button"
                onClick={() => setShowSmsBox(!showSmsBox)}
                className={`w-full min-w-0 justify-center px-2 py-1.5 rounded-lg border text-xs font-medium transition flex items-center gap-1.5 cursor-pointer truncate ${
                  showSmsBox
                    ? "bg-purple-500/30 text-purple-200 border-purple-500/50"
                    : "bg-zinc-800/80 hover:bg-zinc-800 text-zinc-300 border-zinc-700"
                }`}
                title="Paste Indian Banking or UPI SMS alert"
              >
                <MessageSquare className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                <span className="truncate">{showSmsBox ? "Hide SMS" : "Paste SMS"}</span>
              </button>
            </div>

            {/* Voice listening status indicator */}
            {isListening && (
              <div className="flex items-center gap-1.5 text-[11px] text-rose-300 animate-pulse pt-0.5 min-w-0 truncate">
                <span className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-ping shrink-0" />
                <span className="truncate">Listening to your voice... Speak your expense</span>
              </div>
            )}

            {/* Collapsible SMS Paste Box */}
            {showSmsBox && (
              <div className="pt-2 border-t border-zinc-800/80 space-y-2 animate-in fade-in duration-200 w-full min-w-0">
                <div className="flex items-center justify-between text-[11px] text-zinc-400 gap-2 min-w-0">
                  <span className="truncate">Paste bank or UPI SMS (HDFC, SBI, ICICI, etc.):</span>
                  <button
                    type="button"
                    onClick={() => setShowSmsBox(false)}
                    className="text-zinc-500 hover:text-zinc-300 text-[10px] shrink-0"
                  >
                    Cancel
                  </button>
                </div>
                <textarea
                  rows={2}
                  placeholder="e.g. 'HDFC Bank: Rs 850.00 debited from a/c **1234 on 02-Sep-26 to ZOMATO. UPI Ref 324156'"
                  value={smsInputText}
                  onChange={(e) => setSmsInputText(e.target.value)}
                  className="w-full min-w-0 bg-zinc-900 border border-zinc-800 rounded-lg p-2 text-xs text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-purple-500 transition resize-none"
                />
                <div className="flex justify-end w-full min-w-0">
                  <button
                    type="button"
                    onClick={handleSmsParse}
                    disabled={isParsingSms || !smsInputText.trim()}
                    className="px-3 py-1.5 rounded-md bg-purple-600 hover:bg-purple-500 text-white text-[11px] font-medium transition flex items-center gap-1.5 disabled:opacity-50 cursor-pointer shadow-sm"
                  >
                    {isParsingSms ? <Loader2 className="w-3 h-3 animate-spin" /> : <Sparkles className="w-3 h-3" />}
                    Parse &amp; Auto-Fill
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Duplicate Transaction Warning Banner */}
        {duplicateWarning && (
          <div className="rounded-lg border border-amber-500/40 bg-amber-500/10 p-3 text-xs text-amber-200 flex items-start gap-2.5 animate-in fade-in duration-200">
            <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <div className="flex-1 space-y-1">
              <span className="font-semibold text-amber-300">Potential Duplicate Detected:</span>
              <p className="text-[11px] text-amber-200/90 leading-relaxed">{duplicateWarning}</p>
            </div>
            <button
              type="button"
              onClick={() => setDuplicateWarning(null)}
              className="text-amber-400/80 hover:text-amber-300 text-[11px] px-1.5 py-0.5 rounded border border-amber-500/30 hover:bg-amber-500/20 transition cursor-pointer"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Amount field */}
        <Input
          label={`Amount (${currencySymbol})`}
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
          categories={localCategories.length > 0 ? localCategories : categories}
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
          {/* Suggested existing category */}
          {suggestedCategory && suggestedCategory.id !== categoryId && (
            <div className="mt-1.5 flex items-center justify-between rounded-lg bg-indigo-500/10 border border-indigo-500/25 px-2.5 sm:px-3 py-1.5 text-xs text-indigo-300 animate-in fade-in duration-200 w-full min-w-0 gap-2">
              <div className="flex items-center gap-1.5 min-w-0 truncate">
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
                className="px-2.5 py-1 rounded bg-indigo-600 hover:bg-indigo-500 text-[11px] text-white font-medium cursor-pointer transition shrink-0 shadow-sm"
              >
                Apply
              </button>
            </div>
          )}

          {/* Suggested new category that does not exist in user's category list */}
          {suggestedNewCategory && (
            <div className="mt-2 flex items-center justify-between rounded-xl bg-gradient-to-r from-amber-500/15 via-orange-500/10 to-transparent border border-amber-500/40 p-2.5 sm:px-3 text-xs text-amber-200 animate-in fade-in duration-200 w-full min-w-0 gap-2 shadow-sm">
              <div className="flex items-center gap-2 min-w-0 truncate">
                <Sparkles className="w-4 h-4 text-amber-400 shrink-0 animate-pulse" />
                <span className="truncate">
                  Suggested Category:{" "}
                  <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-amber-500/25 text-amber-200 font-semibold border border-amber-500/40 ml-1">
                    {suggestedNewCategory}
                  </span>
                  {suggestedReason && (
                    <span className="text-zinc-400 text-[11px] ml-1.5 hidden sm:inline">
                      ({suggestedReason})
                    </span>
                  )}
                </span>
              </div>
              <div className="flex items-center gap-1.5 shrink-0">
                <button
                  type="button"
                  disabled={isAddingNewCategory}
                  onClick={async () => {
                    try {
                      setIsAddingNewCategory(true);
                      const created = await handleCreateCategory(suggestedNewCategory);
                      setCategoryId(created.id);
                      setSuggestedNewCategory(null);
                      setSuggestedCategory(null);
                      success(`Category "${created.name}" created and selected!`);
                    } catch {
                      toastError(`Failed to create category "${suggestedNewCategory}"`);
                    } finally {
                      setIsAddingNewCategory(false);
                    }
                  }}
                  className="px-3 py-1.5 rounded-lg bg-gradient-to-r from-amber-600 to-orange-600 hover:from-amber-500 hover:to-orange-500 text-xs text-white font-semibold cursor-pointer transition flex items-center gap-1.5 shadow-md hover:shadow-amber-500/20 disabled:opacity-50"
                  title={`Create category "${suggestedNewCategory}" and select it`}
                >
                  {isAddingNewCategory ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <Plus className="w-3.5 h-3.5" />
                  )}
                  <span>Add &amp; Select</span>
                </button>
                <button
                  type="button"
                  onClick={() => setSuggestedNewCategory(null)}
                  className="p-1 text-amber-400/60 hover:text-amber-200 text-xs rounded transition cursor-pointer"
                  title="Dismiss suggestion"
                >
                  ✕
                </button>
              </div>
            </div>
          )}

          {/* Mindful Spending & Buyer's Remorse Reflection Banner */}
          {sentimentResult && showSentimentTip && (
            <div
              className={`mt-2 p-2.5 rounded-xl border text-xs animate-in fade-in duration-200 ${
                sentimentResult.sentiment === "remorse" || sentimentResult.sentiment === "stress"
                  ? "bg-rose-500/10 border-rose-500/30 text-rose-200"
                  : "bg-indigo-500/10 border-indigo-500/25 text-indigo-200"
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 font-semibold">
                  <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Mindful Check:</span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-zinc-900/80 border border-zinc-700 text-zinc-300">
                    {sentimentResult.spending_tag}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setShowSentimentTip(false)}
                  className="text-zinc-400 hover:text-zinc-200 text-xs px-1 rounded transition cursor-pointer"
                  title="Dismiss reflection prompt"
                >
                  ✕
                </button>
              </div>
              <p className="mt-1 text-[11px] text-zinc-300 leading-relaxed">
                {sentimentResult.reflection}
              </p>
            </div>
          )}
        </div>

        {/* Recurring Expense / Subscription Toggle */}
        <div className="rounded-xl bg-zinc-900/60 border border-border p-3 space-y-2">
          <div className="flex items-center justify-between">
            <label htmlFor="recurring-toggle" className="text-xs font-semibold text-foreground cursor-pointer select-none">
              🔁 Recurring Subscription / Fixed Bill?
            </label>
            <input
              type="checkbox"
              id="recurring-toggle"
              checked={isRecurring}
              onChange={(e) => setIsRecurring(e.target.checked)}
              className="h-4 w-4 rounded border-border text-primary focus:ring-primary cursor-pointer accent-indigo-600"
            />
          </div>
          {isRecurring && (
            <div className="flex items-center gap-2 pt-1 animate-in fade-in">
              <span className="text-[11px] text-muted-foreground">Frequency:</span>
              {(["monthly", "weekly", "yearly"] as const).map((freq) => (
                <button
                  key={freq}
                  type="button"
                  onClick={() => setRecurringFrequency(freq)}
                  className={`px-2.5 py-0.5 rounded-full text-xs font-medium capitalize border transition-all cursor-pointer ${
                    recurringFrequency === freq
                      ? "bg-primary text-primary-foreground border-primary shadow-sm"
                      : "bg-zinc-800/80 text-muted-foreground border-border hover:text-foreground"
                  }`}
                >
                  {freq}
                </button>
              ))}
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
