package com.paradox.finance

import android.app.Application

class ParadoxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ParadoxApplication
            private set
    }
}
