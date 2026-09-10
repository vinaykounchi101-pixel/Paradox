package com.paradox.finance

import android.app.Application
import com.paradox.finance.data.api.ApiClient
import com.paradox.finance.data.api.TokenManager
import com.paradox.finance.data.local.AppDatabase

class ParadoxApplication : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        tokenManager = TokenManager(this)
        database = AppDatabase.getDatabase(this)
        ApiClient.initialize(tokenManager)
    }

    companion object {
        lateinit var instance: ParadoxApplication
            private set
    }
}
