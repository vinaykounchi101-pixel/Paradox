package com.paradox.finance.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.paradox.finance.core.Constants

class AuthPreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            Constants.PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveTokens(accessToken: String, refreshToken: String?) {
        prefs.edit().apply {
            putString(Constants.KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(Constants.KEY_REFRESH_TOKEN, it) }
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(Constants.KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(Constants.KEY_REFRESH_TOKEN, null)

    fun saveUser(email: String, fullName: String?, currency: String) {
        prefs.edit().apply {
            putString(Constants.KEY_USER_EMAIL, email)
            putString(Constants.KEY_USER_NAME, fullName ?: "")
            putString(Constants.KEY_CURRENCY, currency)
            apply()
        }
    }

    fun getUserEmail(): String? = prefs.getString(Constants.KEY_USER_EMAIL, null)

    fun getUserName(): String? = prefs.getString(Constants.KEY_USER_NAME, null)

    fun getCurrency(): String = prefs.getString(Constants.KEY_CURRENCY, "INR") ?: "INR"

    fun setCurrency(currency: String) {
        prefs.edit().putString(Constants.KEY_CURRENCY, currency).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(Constants.KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
