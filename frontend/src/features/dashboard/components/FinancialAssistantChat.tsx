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
  const { currencySymbol } = useCurrency();
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
        "👋 Hi! I'm your Paradox Financial Copilot. Grounded in your live spending and budgets, ask me anything—like whether you can afford an upcoming purchase or where your money is going!",
    },
  ]);
  const [inputMessage, setInputMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [suggestedFollowups, setSuggestedFollowups] = useState<string[]>([
    `Can I afford ${currencySymbol}2,500 purchase today?`,
    "How much have I spent this month?",
    "What is my biggest expense category?",
    "What is my daily safe-to-spend limit?",
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

    const userMsg: ChatMessage = { role: "user", content: query };
    const newHistory = [...messages, userMsg];
    setMessages(newHistory);
    setInputMessage("");
    setIsLoading(true);

    try {
      const res = await aiApi.chat({
        message: query,
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
      setMessages([
        ...newHistory,
        {
          role: "assistant",
          content:
            "⚠️ I had trouble connecting to the financial intelligence service. Please check your network connection and try again.",
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      {/* Floating Trigger Button (when uncontrolled) */}
      {!isControlled && (
        <motion.button
          type="button"
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={() => setIsOpen(!isOpen)}
          className="fixed bottom-5 right-5 z-40 flex items-center gap-2 rounded-full bg-gradient-to-r from-indigo-600 to-purple-600 px-4 py-3 text-sm font-semibold text-white shadow-xl shadow-indigo-500/25 hover:from-indigo-500 hover:to-purple-500 transition-all cursor-pointer border border-indigo-400/30"
          title="Open AI Financial Copilot Chat"
        >
          <Sparkles className="h-4 w-4 animate-pulse text-amber-300" />
          <span>Ask Copilot</span>
        </motion.button>
      )}

      {/* Slide-Over Drawer / Modal */}
      <AnimatePresence>
        {isOpen && (
          <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-end sm:justify-end sm:pr-6 sm:pb-6 pointer-events-none">
            {/* Backdrop on mobile */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsOpen(false)}
              className="fixed inset-0 bg-black/60 backdrop-blur-xs sm:hidden pointer-events-auto"
            />

            {/* Chat Box Container */}
            <motion.div
              initial={{ opacity: 0, y: 30, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 30, scale: 0.96 }}
              transition={{ duration: 0.2, ease: "easeOut" }}
              className="pointer-events-auto flex flex-col w-full sm:w-[420px] h-[85vh] sm:h-[580px] rounded-t-2xl sm:rounded-2xl border border-indigo-500/30 bg-zinc-950/95 shadow-2xl shadow-indigo-950/40 backdrop-blur-xl overflow-hidden"
            >
              {/* Header */}
              <div className="flex items-center justify-between px-4 py-3 border-b border-zinc-800/80 bg-zinc-900/60">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-tr from-indigo-500 to-purple-500 text-white shadow-sm">
                    <Bot className="h-4 w-4" />
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <h3 className="text-sm font-bold text-zinc-100">Financial Copilot</h3>
                      <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                    </div>
                    <p className="text-[10px] text-zinc-400">Grounded in your live financial data</p>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => {
                      setMessages([
                        {
                          role: "assistant",
                          content:
                            "Refreshed! What financial decision or budget question would you like to explore?",
                        },
                      ]);
                    }}
                    title="Clear conversation"
                    className="p-1.5 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 rounded-lg transition cursor-pointer"
                  >
                    <RefreshCw className="h-3.5 w-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsOpen(false)}
                    className="p-1.5 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 rounded-lg transition cursor-pointer"
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
                      initial={{ opacity: 0, y: 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      className={`flex gap-2.5 ${isUser ? "justify-end" : "justify-start"}`}
                    >
                      {!isUser && (
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 text-xs">
                          <Bot className="h-3.5 w-3.5" />
                        </div>
                      )}

                      <div
                        className={`max-w-[85%] rounded-2xl px-3.5 py-2.5 text-xs leading-relaxed ${
                          isUser
                            ? "bg-indigo-600 text-white rounded-br-xs shadow-md"
                            : "bg-zinc-900/90 text-zinc-200 border border-zinc-800 rounded-bl-xs shadow-sm"
                        }`}
                      >
                        {/* Message content rendering */}
                        <div className="whitespace-pre-wrap break-words">{m.content}</div>
                      </div>

                      {isUser && (
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-zinc-800 text-zinc-300 text-xs">
                          <UserIcon className="h-3.5 w-3.5" />
                        </div>
                      )}
                    </motion.div>
                  );
                })}

                {isLoading && (
                  <motion.div
                    initial={{ opacity: 0, y: 4 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex items-center gap-2 text-xs text-indigo-300 bg-indigo-950/30 border border-indigo-500/20 rounded-xl px-3 py-2 w-fit"
                  >
                    <Loader2 className="h-3.5 w-3.5 animate-spin text-indigo-400" />
                    <span>Analyzing live financial metrics...</span>
                  </motion.div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Quick Suggestion Chips */}
              {suggestedFollowups.length > 0 && !isLoading && (
                <div className="px-3 py-1.5 border-t border-zinc-800/60 bg-zinc-950/60 flex items-center gap-1.5 overflow-x-auto scrollbar-none">
                  {suggestedFollowups.slice(0, 3).map((chip, i) => (
                    <button
                      key={i}
                      type="button"
                      onClick={() => handleSendMessage(chip)}
                      className="shrink-0 text-[11px] px-2.5 py-1 rounded-full border border-indigo-500/25 bg-indigo-950/40 text-indigo-300 hover:bg-indigo-900/50 hover:border-indigo-400/40 transition cursor-pointer"
                    >
                      {chip}
                    </button>
                  ))}
                </div>
              )}

              {/* Input Footer */}
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleSendMessage();
                }}
                className="p-3 border-t border-zinc-800/80 bg-zinc-900/40 flex items-center gap-2"
              >
                <input
                  ref={inputRef}
                  type="text"
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  placeholder="Ask financial question..."
                  disabled={isLoading}
                  className="flex-1 rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2 text-xs text-zinc-100 placeholder-zinc-500 focus:outline-hidden focus:border-indigo-500 transition"
                />
                <button
                  type="submit"
                  disabled={!inputMessage.trim() || isLoading}
                  className="flex h-8 w-8 items-center justify-center rounded-xl bg-indigo-600 text-white hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed transition cursor-pointer shrink-0 shadow-sm"
                >
                  <Send className="h-3.5 w-3.5" />
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  );
};
