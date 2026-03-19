package com.hbctool.util

import android.content.Context

object PrefsManager {
    private const val PREFS = "hbc_prefs"
    private const val KEY_LANG         = "language"
    private const val KEY_TERMUX_PATH  = "termux_bin_path"
    private const val KEY_SETUP_DONE   = "setup_done_v2"

    fun getLanguage(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "en") ?: "en"

    fun setLanguage(ctx: Context, lang: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()

    /** Custom Termux bin path set by user, overrides auto-detection */
    fun getTermuxPath(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TERMUX_PATH, null)

    fun setTermuxPath(ctx: Context, path: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TERMUX_PATH, path).apply()

    fun clearTermuxPath(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_TERMUX_PATH).apply()

    fun isSetupDone(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SETUP_DONE, false)

    fun markSetupDone(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SETUP_DONE, true).apply()
}
