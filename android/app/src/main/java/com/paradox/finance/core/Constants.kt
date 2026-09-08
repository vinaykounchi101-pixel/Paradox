package com.paradox.finance.core

object Constants {
    // Production Render Backend URL (Default)
    const val DEFAULT_BASE_URL = "https://paradox-2t3x.onrender.com/"
    
    // Localhost fallback (when developing on local network or emulator)
    const val LOCAL_EMULATOR_URL = "http://10.0.2.2:8000/"
    
    // Preference Keys
    const val PREFS_NAME = "paradox_secure_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_NAME = "user_name"
    const val KEY_CURRENCY = "selected_currency"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    // Supported Currencies
    val SUPPORTED_CURRENCIES = listOf("INR", "USD", "EUR", "GBP")
    val CURRENCY_SYMBOLS = mapOf(
        "INR" to "₹",
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£"
    )
}
