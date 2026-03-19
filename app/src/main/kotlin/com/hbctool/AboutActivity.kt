package com.hbctool

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        findViewById<MaterialButton>(R.id.btnBackAbout).setOnClickListener { finish() }

        // Version
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrDefault("1.0")
        findViewById<TextView>(R.id.tvAboutVersion).text =
            getString(R.string.about_version, versionName)

        // Telegram
        findViewById<MaterialButton>(R.id.btnAboutTelegram).setOnClickListener {
            openUrl("https://t.me/Fireway727")
        }

        // GitHub — user fills in their own link
        findViewById<MaterialButton>(R.id.btnAboutGithub).setOnClickListener {
            openUrl(getString(R.string.about_github_url))
        }

        // Guide
        findViewById<MaterialCardView>(R.id.cardAboutGuide).setOnClickListener {
            startActivity(Intent(this, GuideActivity::class.java))
        }
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}
