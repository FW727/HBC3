package com.hbctool

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.hbctool.util.LanguageManager
import com.hbctool.util.PrefsManager

abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(base: Context) {
        val lang = PrefsManager.getLanguage(base)
        super.attachBaseContext(LanguageManager.wrap(base, lang))
    }
}
