package com.paradox.finance.native_features

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import com.paradox.finance.data.model.ParseExpenseRequest
import com.paradox.finance.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                val sender = sms.originatingAddress ?: "Unknown"

                // Check if the SMS resembles a banking/UPI transaction
                if (isFinancialSms(body)) {
                    Log.d("ParadoxSmsReceiver", "Financial SMS detected from $sender: $body")

                    // Call backend to parse transaction
                    val apiService = ApiClient.getService(context)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val res = apiService.parseExpense(ParseExpenseRequest(body))
                            if (res.isSuccessful && res.body() != null) {
                                val parsed = res.body()!!
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        context,
                                        "⚡ Paradox: Detected ₹${parsed.amount ?: ""} spent at ${parsed.description ?: "Merchant"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ParadoxSmsReceiver", "Error parsing SMS", e)
                        }
                    }
                }
            }
        }
    }

    private fun isFinancialSms(body: String): Boolean {
        val lower = body.lowercase()
        val financialKeywords = listOf(
            "debited", "spent", "paid", "sent", "txn", "transaction",
            "vpa", "upi", "hdfc", "sbi", "icici", "axis", "kotak",
            "paytm", "gpay", "phonepe", "cred"
        )
        return financialKeywords.any { lower.contains(it) } &&
                (lower.contains("rs") || lower.contains("inr") || lower.contains("₹"))
    }
}
