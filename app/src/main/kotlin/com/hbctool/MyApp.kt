package com.hbctool

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.hbctool.util.CrashHandler
import com.hbctool.util.LanguageManager
import com.hbctool.util.PrefsManager

class MyApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageManager.wrap(base, PrefsManager.getLanguage(base)))
    }

    override fun onCreate() {
        super.onCreate()
        // Follow system light/dark — must be called before any Activity is created
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        CrashHandler.install(this)
    }
}
