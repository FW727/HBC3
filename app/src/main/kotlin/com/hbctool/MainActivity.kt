package com.hbctool

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.hbctool.ui.AnimatedBorderCardView
import com.hbctool.util.HbcSetup
import com.hbctool.util.LanguageManager
import com.hbctool.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Language toggle
        val btnLang = findViewById<MaterialButton>(R.id.btnLang)
        updateLangBtn(btnLang)
        btnLang.setOnClickListener { LanguageManager.toggle(this); recreate() }

        // Feature cards
        findViewById<AnimatedBorderCardView>(R.id.cardDisasm).setOnClickListener {
            startActivity(Intent(this, DisasmActivity::class.java))
        }
        findViewById<AnimatedBorderCardView>(R.id.cardAsm).setOnClickListener {
            startActivity(Intent(this, AsmActivity::class.java))
        }
        findViewById<AnimatedBorderCardView>(R.id.cardFinder).setOnClickListener {
            startActivity(Intent(this, HermesFinderActivity::class.java))
        }

        // Bottom nav
        findViewById<View>(R.id.btnSetup).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnGuide).setOnClickListener {
            startActivity(Intent(this, GuideActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // Termux live status
        val tvStatus = findViewById<TextView>(R.id.tvTermuxStatus)
        lifecycleScope.launch {
            val env = withContext(Dispatchers.IO) { HbcSetup.env(this@MainActivity) }
            tvStatus.text = when {
                env == null        -> getString(R.string.status_no_termux)
                !env.hasPython     -> getString(R.string.status_no_python)
                !env.hasHbctool    -> getString(R.string.status_no_hbctool)
                else               -> getString(R.string.status_ready)
            }
            tvStatus.setTextColor(
                if (env?.isReady == true) 0xFF00E57A.toInt() else 0xFFFFAA00.toInt()
            )
        }
    }

    private fun updateLangBtn(btn: MaterialButton) {
        btn.text = if (PrefsManager.getLanguage(this) == "en")
            getString(R.string.lang_btn_es) else getString(R.string.lang_btn_en)
    }
}
