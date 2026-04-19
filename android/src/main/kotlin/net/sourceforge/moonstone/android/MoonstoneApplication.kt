package net.sourceforge.moonstone.android

import android.app.Application

/**
 * Application class for Moonstone Android app.
 * Provides app-level initialization and configuration.
 */
class MoonstoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MoonstoneApplication
            private set
    }
}
