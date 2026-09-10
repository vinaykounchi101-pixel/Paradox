package com.paradox.finance.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: Double, currency: String = "INR"): String {
        val symbol = when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }

        val format = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val formattedAmount = format.format(amount)
        return "$symbol$formattedAmount"
    }

    fun getSymbol(currency: String = "INR"): String {
        return when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
    }
}
