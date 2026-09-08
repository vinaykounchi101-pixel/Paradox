package com.paradox.finance.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "paradox_auth_prefs")

class TokenManager(private val context: Context) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_CURRENCY = stringPreferencesKey("user_currency")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACCESS_TOKEN]
    }

    val userEmailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL]
    }

    val userCurrencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_CURRENCY] ?: "INR"
    }

    suspend fun saveAuthSession(token: String, email: String, name: String? = null, currency: String = "INR") {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = token
            preferences[KEY_USER_EMAIL] = email
            if (name != null) preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_CURRENCY] = currency
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_USER_EMAIL)
            preferences.remove(KEY_USER_NAME)
        }
    }
}
