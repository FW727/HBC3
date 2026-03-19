package com.hbctool.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {

    fun wrap(context: Context, lang: String): Context {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getCurrentLang(context: Context): String = PrefsManager.getLanguage(context)

    fun toggle(context: Context): String {
        val current = getCurrentLang(context)
        val next = if (current == "en") "es" else "en"
        PrefsManager.setLanguage(context, next)
        return next
    }
}
