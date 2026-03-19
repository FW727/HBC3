package com.hbctool

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class CrashActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TRACE = "crash_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "No info available."
        val report = buildString {
            appendLine("── HBC to Bytecode ────────────────")
            appendLine("Version : ${appVersion()}")
            appendLine("Android : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device  : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("ABI     : ${Build.SUPPORTED_ABIS.firstOrNull()}")
            appendLine("───────────────────────────────────")
            append(trace)
        }

        val root = findViewById<View>(android.R.id.content)
        findViewById<TextView>(R.id.tvCrashTrace).text = report

        findViewById<MaterialButton>(R.id.btnCopyCrash).setOnClickListener {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("crash", report))
            Snackbar.make(root, R.string.crash_copied, Snackbar.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnReportTelegram).setOnClickListener {
            val msg = Uri.encode(report.take(500) + if (report.length > 500) "\n…" else "")
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/Fireway727?text=$msg")))
            } catch (_: Exception) {
                Snackbar.make(root, R.string.telegram_not_found, Snackbar.LENGTH_SHORT).show()
            }
        }

        findViewById<MaterialButton>(R.id.btnRestartApp).setOnClickListener {
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(this)
            }
            finish()
        }
    }

    private fun appVersion() = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrDefault("?")
}
