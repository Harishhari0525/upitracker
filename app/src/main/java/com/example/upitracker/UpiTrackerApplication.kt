package com.example.upitracker

import android.app.Application
import com.example.upitracker.util.CryptoManager

class UpiTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize security immediately so it's ready for ViewModels and Workers
        try {
            CryptoManager.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}