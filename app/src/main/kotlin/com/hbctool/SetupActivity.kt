package com.hbctool

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.hbctool.util.HbcSetup
import com.hbctool.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : BaseActivity() {

    private lateinit var tvPythonState:   TextView
    private lateinit var tvHbctoolState:  TextView
    private lateinit var tvSource:        TextView
    private lateinit var tvStatus:        TextView
    private lateinit var etCustomPath:    TextInputEditText
    private lateinit var btnApplyPath:    MaterialButton
    private lateinit var btnRescan:       MaterialButton
    private lateinit var btnInstall:      MaterialButton
    private lateinit var btnSkip:         MaterialButton
    private lateinit var btnContinue:     MaterialButton
    private lateinit var progress:        LinearProgressIndicator
    private lateinit var logScroll:       ScrollView
    private lateinit var logCard:         View
    private lateinit var tvLog:           TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        tvPythonState  = findViewById(R.id.tvPythonState)
        tvHbctoolState = findViewById(R.id.tvHbctoolState)
        tvSource       = findViewById(R.id.tvTermuxSource)
        tvStatus       = findViewById(R.id.tvSetupStatus)
        etCustomPath   = findViewById(R.id.etCustomTermuxPath)
        btnApplyPath   = findViewById(R.id.btnApplyPath)
        btnRescan      = findViewById(R.id.btnRescan)
        btnInstall     = findViewById(R.id.btnInstallHbctool)
        btnSkip        = findViewById(R.id.btnSkipSetup)
        btnContinue    = findViewById(R.id.btnContinueSetup)
        progress       = findViewById(R.id.progressSetup)
        logScroll      = findViewById(R.id.setupLogScroll)
        logCard        = findViewById(R.id.logCardSetup)
        tvLog          = findViewById(R.id.tvSetupLog)

        // Pre-fill with saved or default path
        val saved = PrefsManager.getTermuxPath(this)
        etCustomPath.setText(saved ?: "/data/data/com.termux/files/usr/bin")

        btnApplyPath.setOnClickListener { applyCustomPath() }
        btnRescan.setOnClickListener    { doScan() }
        btnInstall.setOnClickListener   { doInstall() }
        btnSkip.setOnClickListener      { goMain() }
        btnContinue.setOnClickListener  { PrefsManager.markSetupDone(this); goMain() }

        etCustomPath.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { applyCustomPath(); true } else false
        }

        doScan()
    }

    private fun doScan() {
        lifecycleScope.launch {
            val env = withContext(Dispatchers.IO) {
                HbcSetup.invalidate()
                HbcSetup.env(this@SetupActivity)
            }
            refreshState(env)
        }
    }

    private fun applyCustomPath() {
        val path = etCustomPath.text?.toString()?.trim() ?: return
        if (path.isEmpty()) { Snackbar.make(btnApplyPath, "Enter a path first", Snackbar.LENGTH_SHORT).show(); return }
        // Hide keyboard
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(etCustomPath.windowToken, 0)

        lifecycleScope.launch {
            val env = withContext(Dispatchers.IO) {
                val probed = HbcSetup.probeDir(path, "manual")
                if (probed != null) {
                    PrefsManager.setTermuxPath(this@SetupActivity, path)
                    HbcSetup.invalidate()
                    HbcSetup.env(this@SetupActivity)
                } else null
            }
            if (env == null) {
                Snackbar.make(btnApplyPath, "Path not found or empty: $path", Snackbar.LENGTH_LONG).show()
            } else {
                refreshState(env)
                Snackbar.make(btnApplyPath, "Path saved!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshState(env: HbcSetup.Env?) {
        val hasPy  = env?.hasPython  == true
        val hasHbt = env?.hasHbctool == true

        tvPythonState.text = when {
            env == null -> "&#10007; Termux not found"
            hasPy       -> "&#10003; Python: ${env.pythonPath?.substringAfterLast("/")}"
            else        -> "&#10007; Python not found in Termux"
        }
        tvPythonState.setTextColor(if (hasPy) 0xFF00E57A.toInt() else 0xFFFF4D4D.toInt())

        tvHbctoolState.text = if (hasHbt)
            "&#10003; hbctool ready" else "&#9711; hbctool not installed"
        tvHbctoolState.setTextColor(if (hasHbt) 0xFF00E57A.toInt() else 0xFFFFAA00.toInt())

        tvSource.text = env?.let { "Source: ${it.source}" } ?: ""
        tvSource.visibility = if (env != null) View.VISIBLE else View.GONE

        btnInstall.isEnabled = hasPy && !hasHbt
        btnInstall.text = getString(when {
            env == null -> R.string.setup_need_python
            !hasPy      -> R.string.setup_need_python
            hasHbt      -> R.string.setup_already_installed
            else        -> R.string.setup_install_btn
        })
        btnContinue.visibility = if (hasHbt) View.VISIBLE else View.GONE
        tvStatus.text = getString(when {
            hasHbt -> R.string.setup_ready
            hasPy  -> R.string.setup_tap_install
            else   -> R.string.setup_need_python_hint
        })
    }

    private fun doInstall() {
        progress.visibility  = View.VISIBLE
        logCard.visibility   = View.VISIBLE
        btnInstall.isEnabled = false
        btnSkip.isEnabled    = false
        tvLog.text = ""

        val sb = StringBuilder()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                HbcSetup.installHbctool(this@SetupActivity) { line ->
                    sb.appendLine(line)
                    runOnUiThread {
                        tvLog.text = sb.toString()
                        logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                }
            }
            progress.visibility  = View.GONE
            btnSkip.isEnabled    = true
            val env = withContext(Dispatchers.IO) { HbcSetup.env(this@SetupActivity) }
            refreshState(env)
            sb.appendLine(if (ok) "&#10003; Done!" else "&#10007; Failed. Check path and try again.")
            tvLog.text = sb.toString()
            if (ok) PrefsManager.markSetupDone(this@SetupActivity)
            else btnInstall.isEnabled = env?.hasPython == true
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}
