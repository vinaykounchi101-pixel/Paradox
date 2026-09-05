"use client";

import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  MessageSquare,
  Sparkles,
  X,
  Send,
  Loader2,
  Bot,
  User as UserIcon,
  ChevronDown,
  RefreshCw,
  Zap,
} from "lucide-react";
import { aiApi, ChatMessage } from "@/lib/api/ai";
import { useCurrency } from "@/features/auth/context/CurrencyContext";

interface FinancialAssistantChatProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export const FinancialAssistantChat: React.FC<FinancialAssistantChatProps> = ({
  isOpen: controlledIsOpen,
  onClose: controlledOnClose,
}) => {
  const { currencySymbol, formatCurrency } = useCurrency();
  const [internalIsOpen, setInternalIsOpen] = useState(false);

  const isControlled = controlledIsOpen !== undefined;
  const isOpen = isControlled ? controlledIsOpen : internalIsOpen;
  const setIsOpen = (open: boolean) => {
    if (isControlled && controlledOnClose) {
      if (!open) controlledOnClose();
    } else {
      setInternalIsOpen(open);
    }
  };

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: "assistant",
      content:
        "👋 Hi! I'm your Paradox Financial Copilot. Grounded in your live transactions and budget velocity, ask me anything—like whether you can afford an upcoming purchase or where your money is going!",
    },
  ]);
  const [inputMessage, setInputMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [suggestedFollowups, setSuggestedFollowups] = useState<string[]>([
    `🛍️ Can I afford a ${currencySymbol}3,000 purchase?`,
    "📊 How much have I spent this month?",
    "⚡ What is my daily safe-to-spend limit?",
    "🔁 List my active subscriptions",
    "🛡️ Any unusual spending anomalies?",
  ]);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [isOpen, messages]);

  const handleSendMessage = async (textToSend?: string) => {
    const query = (textToSend || inputMessage).trim();
    if (!query || isLoading) return;

    // Remove emoji prefix if clicked from chips
    const cleanedQuery = query.replace(/^[\p{Emoji}\s]+/u, "").trim() || query;

    const userMsg: ChatMessage = { role: "user", content: query };
    const newHistory = [...messages, userMsg];
    setMessages(newHistory);
    setInputMessage("");
    setIsLoading(true);

    try {
      const res = await aiApi.chat({
        message: cleanedQuery,
        history: messages.slice(-6), // last 6 turns
      });

      const assistantMsg: ChatMessage = {
        role: "assistant",
        content: res.data.reply,
      };
      setMessages([...newHistory, assistantMsg]);
      if (res.data.suggested_followups && res.data.suggested_followups.length > 0) {
        setSuggestedFollowups(res.data.suggested_followups);
      }
    } catch {
      // Graceful contextual fallback when backend container is deploying/updating
      let fallbackReply = "⚠️ AI Copilot server is currently updating. In the meantime, you can check your live allowance in the Safe-to-Spend Speedometer or test purchase impacts with the 'Can I Afford This?' simulator!";

      const lower = cleanedQuery.toLowerCase();
      if (lower.includes("afford") || lower.includes("buy") || lower.includes("purchase")) {
        fallbackReply = `💡 Affordability Quick Check: Compare the purchase price against your remaining monthly allowance in the Safe-to-Spend card. If this purchase exceeds your daily buffer, consider using the 'Can I Afford This?' simulator on your dashboard to see the exact budget impact! 🛍️`;
      } else if (lower.includes("subscription") || lower.includes("recurring")) {
        fallbackReply = `🔁 Subscriptions Insight: You can audit all detected recurring burdens and annual totals anytime by clicking 'Audit' on the Subscriptions & Fixed Bills card! 📺`;
      } else if (lower.includes("spent") || lower.includes("spending") || lower.includes("how much")) {
        fallbackReply = `📊 Spending Insight: Your total spend and top category velocity are tracked live in the KPI cards and 3D Category chart on your dashboard! 📈`;
      } else if (lower.includes("anomal") || lower.includes("unusual")) {
        fallbackReply = `🛡️ Anomaly Check: Review the Anomaly Guard & Spend Forecast card on your dashboard to inspect statistical outliers in real time!`;
      }

      setMessages([
        ...newHistory,
        {
          role: "assistant",
          content: fallbackReply,
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      {/* Floating Trigger Button with 3D Pop (when uncontrolled) */}
      {!isControlled && (
        <motion.button
          type="button"
          whileHover={{ scale: 1.08, y: -3 }}
          whileTap={{ scale: 0.94 }}
          onClick={() => setIsOpen(!isOpen)}
          className="fixed bottom-5 right-5 z-40 flex items-center gap-2.5 rounded-full bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 px-4 py-3 text-sm font-bold text-white shadow-2xl transition-all cursor-pointer border border-white/20"
          style={{
            boxShadow: "0 10px 25px -5px rgba(99, 102, 241, 0.5), inset 0 1px 1px rgba(255, 255, 255, 0.3)",
          }}
          title="Open AI Financial Copilot Chat"
        >
          <Sparkles className="h-4 w-4 animate-spin text-amber-300" style={{ animationDuration: "5s" }} />
          <span>Ask Copilot 💬</span>
        </motion.button>
      )}

      {/* Slide-Over Drawer / Bottom Sheet */}
      <AnimatePresence>
        {isOpen && (
          <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-end sm:pr-6 sm:pb-6 pointer-events-none">
            {/* Backdrop on mobile */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsOpen(false)}
              className="fixed inset-0 bg-black/70 backdrop-blur-xs sm:hidden pointer-events-auto"
            />

            {/* Chat Box Container with 3D Depth */}
            <motion.div
              initial={{ opacity: 0, y: 40, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 40, scale: 0.95 }}
              transition={{ type: "spring", damping: 25, stiffness: 320 }}
              className="pointer-events-auto flex flex-col w-full sm:w-[420px] h-[86dvh] sm:h-[600px] rounded-t-3xl sm:rounded-3xl border border-indigo-500/35 bg-zinc-950/95 shadow-2xl backdrop-blur-xl overflow-hidden"
              style={{
                boxShadow: "0 25px 60px -15px rgba(0, 0, 0, 0.8), 0 0 40px rgba(99, 102, 241, 0.2)",
              }}
            >
              {/* Mobile Drag Indicator */}
              <div className="w-12 h-1 bg-zinc-800 rounded-full mx-auto my-2 sm:hidden shrink-0" />

              {/* Header */}
              <div className="flex items-center justify-between px-4 py-3 border-b border-zinc-800/80 bg-zinc-900/70">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500 text-white shadow-md shadow-indigo-500/20">
                    <Bot className="h-4 w-4" />
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <h3 className="text-sm font-bold text-zinc-100">Financial Copilot</h3>
                      <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                    </div>
                    <p className="text-[10px] text-zinc-400">Live intelligence & budget ground truth</p>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <motion.button
                    type="button"
                    whileHover={{ scale: 1.1, rotate: 180 }}
                    whileTap={{ scale: 0.9 }}
                    transition={{ duration: 0.2 }}
                    onClick={() => {
                      setMessages([
                        {
                          role: "assistant",
                          content:
                            "Conversation refreshed! What financial decision or budget question would you like to explore? 🚀",
                        },
                      ]);
                    }}
                    title="Clear conversation"
                    className="p-1.5 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 rounded-xl transition cursor-pointer"
                  >
                    <RefreshCw className="h-3.5 w-3.5" />
                  </motion.button>
                  <button
                    type="button"
                    onClick={() => setIsOpen(false)}
                    className="p-1.5 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 rounded-xl transition cursor-pointer"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              </div>

              {/* Messages Container */}
              <div className="flex-1 overflow-y-auto p-4 space-y-3.5 scrollbar-thin scrollbar-thumb-zinc-800">
                {messages.map((m, idx) => {
                  const isUser = m.role === "user";
                  return (
                    <motion.div
                      key={idx}
                      initial={{ opacity: 0, scale: 0.92, y: 8 }}
                      animate={{ opacity: 1, scale: 1, y: 0 }}
                      transition={{ duration: 0.2 }}
                      className={`flex gap-2.5 ${isUser ? "justify-end" : "justify-start"}`}
                    >
                      {!isUser && (
                        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-xl bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 text-xs shadow-xs">
                          <Bot className="h-3.5 w-3.5" />
                        </div>
                      )}

                      <div
                        className={`max-w-[85%] rounded-2xl px-3.5 py-2.5 text-xs leading-relaxed ${
                          isUser
                            ? "bg-gradient-to-r from-indigo-600 to-purple-600 text-white rounded-br-xs shadow-md"
                            : "bg-zinc-900/95 text-zinc-200 border border-zinc-800/90 rounded-bl-xs shadow-sm"
                        }`}
                      >
                        <div className="whitespace-pre-wrap break-words">{m.content}</div>
                      </div>

                      {isUser && (
                        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-xl bg-zinc-800 text-zinc-300 text-xs border border-zinc-700">
                          <UserIcon className="h-3.5 w-3.5" />
                        </div>
                      )}
                    </motion.div>
                  );
                })}

                {/* Animated Bouncing Typing Dots */}
                {isLoading && (
                  <motion.div
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex items-center gap-2 text-xs text-indigo-300 bg-indigo-950/40 border border-indigo-500/30 rounded-2xl px-3.5 py-2.5 w-fit shadow-xs"
                  >
                    <Bot className="h-3.5 w-3.5 text-indigo-400" />
                    <div className="flex items-center gap-1">
                      {[0, 1, 2].map((dot) => (
                        <motion.span
                          key={dot}
                          animate={{ y: [0, -5, 0] }}
                          transition={{
                            repeat: Infinity,
                            duration: 0.6,
                            delay: dot * 0.15,
                            ease: "easeInOut",
                          }}
                          className="h-1.5 w-1.5 rounded-full bg-indigo-400"
                        />
                      ))}
                    </div>
                    <span className="text-[11px] text-zinc-400 ml-1">Analyzing financial context...</span>
                  </motion.div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Quick Suggestion Chips with Emojis */}
              {suggestedFollowups.length > 0 && !isLoading && (
                <div className="px-3 py-2 border-t border-zinc-800/70 bg-zinc-950/80 flex items-center gap-1.5 overflow-x-auto scrollbar-none">
                  {suggestedFollowups.map((chip, i) => (
                    <motion.button
                      key={i}
                      type="button"
                      whileHover={{ scale: 1.03 }}
                      whileTap={{ scale: 0.97 }}
                      onClick={() => handleSendMessage(chip)}
                      className="shrink-0 text-[11px] px-2.5 py-1 rounded-full border border-indigo-500/25 bg-indigo-950/40 text-indigo-200 hover:bg-indigo-900/50 hover:border-indigo-400/40 transition cursor-pointer font-medium"
                    >
                      {chip}
                    </motion.button>
                  ))}
                </div>
              )}

              {/* Input Footer */}
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSendMessage();
                }}
                className="p-3 border-t border-zinc-800/80 bg-zinc-900/50 flex items-center gap-2"
              >
                <input
                  ref={inputRef}
                  type="text"
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  placeholder="Ask a financial question..."
                  disabled={isLoading}
                  className="flex-1 rounded-2xl border border-zinc-800 bg-zinc-950 px-4 py-2 text-xs text-zinc-100 placeholder-zinc-500 focus:outline-hidden focus:border-indigo-500 transition"
                />
                <motion.button
                  type="submit"
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  disabled={!inputMessage.trim() || isLoading}
                  className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 text-white hover:from-indigo-500 hover:to-purple-500 disabled:opacity-40 disabled:cursor-not-allowed transition cursor-pointer shrink-0 shadow-md"
                >
                  <Send className="h-3.5 w-3.5" />
                </motion.button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  );
};
