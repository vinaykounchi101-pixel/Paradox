package com.paradox.finance.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsResult(
    val amount: Double?,
    val merchant: String?,
    val paymentMethod: String,
    val date: String
)

object SmsParserHelper {

    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr|spent|debited|paid)\\s*(?:rs\\.?|inr)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)")
    private val MERCHANT_PATTERN = Pattern.compile("(?i)(?:at|to|info\\/|vpa)\\s+([a-zA-Z0-9\\s\\.\\-_@]+?)(?:\\s+(?:on|using|via|ref|upi|bal|avbl)|\\.|$)")

    fun parseIndianSms(smsText: String): ParsedSmsResult {
        var amount: Double? = null
        val amountMatcher = AMOUNT_PATTERN.matcher(smsText)
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1)?.toDoubleOrNull()
        }

        var merchant: String? = null
        val merchantMatcher = MERCHANT_PATTERN.matcher(smsText)
        if (merchantMatcher.find()) {
            merchant = merchantMatcher.group(1)?.trim()
        }

        val paymentMethod = when {
            smsText.contains("upi", ignoreCase = true) || smsText.contains("gpay", ignoreCase = true) || smsText.contains("phonepe", ignoreCase = true) -> "UPI"
            smsText.contains("credit card", ignoreCase = true) || smsText.contains("card ending", ignoreCase = true) -> "Credit Card"
            smsText.contains("debit", ignoreCase = true) -> "Debit Card"
            smsText.contains("netbanking", ignoreCase = true) -> "NetBanking"
            else -> "UPI"
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return ParsedSmsResult(
            amount = amount,
            merchant = merchant,
            paymentMethod = paymentMethod,
            date = currentDate
        )
    }
}
