package com.paradox.finance.hardware.sms

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsExpense(
    val amount: Double?,
    val merchant: String?,
    val paymentMethod: String,
    val date: String,
    val reference: String? = null
)

object SmsTransactionParser {

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?i)(?:rs\.?|inr|debited\s+by|spent)\s*[:.]?\s*([0-9,]+(?:\.[0-9]{1,2})?)"""
    )
    
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?i)(?:at|to|vpa|info|trf\s+to)\s+([A-Za-z0-9\s._\-&]{3,30}?)(?:\s+on|\s+ref|\s+upi|\s+avl|\.|\z)"""),
        Pattern.compile("""(?i)(?:paid\s+to)\s+([A-Za-z0-9\s._\-&]{3,30})""")
    )

    fun parse(smsBody: String): ParsedSmsExpense? {
        val text = smsBody.trim()
        if (!isFinancialAlert(text)) return null

        // 1. Extract Amount
        var amount: Double? = null
        val amountMatcher = AMOUNT_PATTERN.matcher(text)
        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull()
        }

        // 2. Extract Merchant
        var merchant: String? = null
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && !isBlacklistedWord(candidate)) {
                    merchant = candidate
                    break
                }
            }
        }

        // 3. Payment Method detection
        val paymentMethod = when {
            text.contains("UPI", ignoreCase = true) || text.contains("VPA", ignoreCase = true) -> "UPI"
            text.contains("Credit Card", ignoreCase = true) || text.contains("Card", ignoreCase = true) -> "Credit Card"
            text.contains("Debit Card", ignoreCase = true) -> "Debit Card"
            text.contains("NetBanking", ignoreCase = true) -> "Net Banking"
            else -> "Cash"
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return ParsedSmsExpense(
            amount = amount,
            merchant = merchant ?: "Bank Transaction",
            paymentMethod = paymentMethod,
            date = todayDate
        )
    }

    private fun isFinancialAlert(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("debited") || lower.contains("spent") || lower.contains("paid") || lower.contains("sent")) &&
                (lower.contains("rs") || lower.contains("inr") || lower.contains("₹") || lower.contains("vpa") || lower.contains("acct"))
    }

    private fun isBlacklistedWord(word: String): Boolean {
        val lower = word.lowercase()
        return lower in setOf("your", "account", "bank", "card", "rs", "inr", "upi", "vpa", "ref", "balance")
    }
}
