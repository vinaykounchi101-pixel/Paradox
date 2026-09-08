package com.paradox.finance

import android.app.Application
import com.paradox.finance.data.remote.ApiClient

class ParadoxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        ApiClient.initialize(this)
    }

    companion object {
        lateinit var instance: ParadoxApplication
            private set
    }
}

