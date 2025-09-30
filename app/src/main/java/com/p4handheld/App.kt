package com.p4handheld

import android.app.Application

class App : Application() {

    // Companion object to hold the singleton instance
    companion object {
        // Volatile ensures that the instance is visible to all threads
        @Volatile
        private var instance: App? = null

        fun getInstance(): App =
            instance ?: synchronized(this) {
                instance ?: throw IllegalStateException("Application is not created yet!")
            }
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize the instance variable when the application is created
        instance = this
    }
}
