package com.hbctool

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.hbctool.ui.AnimatedBorderCardView
import com.hbctool.util.HbcRunner
import com.hbctool.util.RunEvent
import kotlinx.coroutines.launch

class AsmActivity : BaseActivity() {

    private var hasmDirUri: Uri? = null
    private var outputHbcUri: Uri? = null

    private lateinit var tvHasmDir: MaterialTextView
    private lateinit var tvOutputHbc: MaterialTextView
    private lateinit var tvCommand: MaterialTextView
    private lateinit var btnRun: MaterialButton
    private lateinit var btnCopy: MaterialButton
    private lateinit var btnTermux: MaterialButton
    private lateinit var progress: LinearProgressIndicator
    private lateinit var tvProgress: TextView
    private lateinit var tvLog: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var logCard: View
    private lateinit var tvLogStatus: TextView
    private lateinit var cardHasm: AnimatedBorderCardView
    private lateinit var cardOut: AnimatedBorderCardView

    private val pickHasmDir = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        hasmDirUri = uri; tvHasmDir.text = uri.lastPathSegment ?: uri.toString()
        rebuildCommand(); refreshRunBtn()
    }

    private val pickOutputHbc = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri ?: return@registerForActivityResult
        outputHbcUri = uri
        tvOutputHbc.text = getFileName(uri) ?: uri.lastPathSegment ?: uri.toString()
        rebuildCommand(); refreshRunBtn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asm)

        tvHasmDir   = findViewById(R.id.tvHasmDir)
        tvOutputHbc = findViewById(R.id.tvOutputHbc)
        tvCommand   = findViewById(R.id.tvCommandAsm)
        btnRun      = findViewById(R.id.btnRunInAppAsm)
        btnCopy     = findViewById(R.id.btnCopyCommandAsm)
        btnTermux   = findViewById(R.id.btnRunTermuxAsm)
        progress    = findViewById(R.id.progressAsm)
        tvProgress  = findViewById(R.id.tvProgressLabelAsm)
        tvLog       = findViewById(R.id.tvLogAsm)
        logScroll   = findViewById(R.id.logScrollAsm)
        logCard     = findViewById(R.id.logCardAsm)
        tvLogStatus = findViewById(R.id.tvLogStatusAsm)
        cardHasm    = findViewById(R.id.cardSelectHasm)
        cardOut     = findViewById(R.id.cardSelectOutputHbc)

        tvHasmDir.text   = getString(R.string.no_dir_yet)
        tvOutputHbc.text = getString(R.string.no_file_yet)
        btnRun.isEnabled = false; btnCopy.isEnabled = false; btnTermux.isEnabled = false

        findViewById<MaterialButton>(R.id.btnBackAsm).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSelectHasmDir).setOnClickListener { pickHasmDir.launch(null) }
        findViewById<MaterialButton>(R.id.btnSelectOutputHbc).setOnClickListener { pickOutputHbc.launch("index.android.bundle") }

        btnRun.setOnClickListener { doRun() }

        btnCopy.setOnClickListener {
            val cmd = tvCommand.text.toString()
            if (cmd.isNotBlank()) {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("cmd", cmd))
                Snackbar.make(btnCopy, R.string.command_copied, Snackbar.LENGTH_SHORT).show()
            }
        }

        btnTermux.setOnClickListener {
            val cmd = tvCommand.text.toString()
            if (cmd.isBlank()) return@setOnClickListener
            try {
                startService(Intent().apply {
                    setClassName("com.termux", "com.termux.app.RunCommandService")
                    action = "com.termux.RUN_COMMAND"
                    putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                    putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", cmd))
                    putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
                })
            } catch (_: Exception) {
                Snackbar.make(btnTermux, R.string.termux_not_found, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun doRun() {
        val dirUri = hasmDirUri   ?: return
        val outUri = outputHbcUri ?: return

        tvLog.text = ""; tvLogStatus.visibility = View.GONE
        logCard.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE; tvProgress.visibility = View.VISIBLE
        setInputsEnabled(false)
        cardHasm.setLoading(true); cardOut.setLoading(true)

        val sb = StringBuilder()
        lifecycleScope.launch {
            HbcRunner.asm(this@AsmActivity, dirUri, outUri).collect { event ->
                when (event) {
                    is RunEvent.Progress -> { tvProgress.text = event.label; log(sb, "▶ ${event.label}") }
                    is RunEvent.Log      -> if (event.line.isNotBlank()) log(sb, event.line)
                    is RunEvent.Success  -> done(sb, true)
                    is RunEvent.Failure  -> { log(sb, "\n✗ ${event.reason}"); done(sb, false) }
                }
                tvLog.text = sb.toString()
                logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }

    private fun done(sb: StringBuilder, success: Boolean) {
        progress.visibility = View.GONE; tvProgress.visibility = View.GONE
        tvLogStatus.text = getString(if (success) R.string.run_success else R.string.run_error)
        tvLogStatus.setTextColor(if (success) 0xFF00E57A.toInt() else 0xFFFF4D4D.toInt())
        tvLogStatus.visibility = View.VISIBLE
        setInputsEnabled(true)
        cardHasm.setLoading(false); cardOut.setLoading(false)
    }

    private fun log(sb: StringBuilder, line: String) { if (sb.isNotEmpty()) sb.append('\n'); sb.append(line) }

    private fun setInputsEnabled(on: Boolean) {
        btnRun.isEnabled = on; btnCopy.isEnabled = on; btnTermux.isEnabled = on
        findViewById<MaterialButton>(R.id.btnSelectHasmDir).isEnabled = on
        findViewById<MaterialButton>(R.id.btnSelectOutputHbc).isEnabled = on
    }

    private fun refreshRunBtn() {
        val ready = hasmDirUri != null && outputHbcUri != null
        btnRun.isEnabled = ready; btnCopy.isEnabled = ready; btnTermux.isEnabled = ready
    }

    private fun rebuildCommand() {
        val dir = hasmDirUri ?: return; val out = outputHbcUri ?: return
        val dp = getRealPath(dir) ?: dir.path ?: return
        val op = getRealPath(out) ?: out.path ?: return
        tvCommand.text = "hbctool asm \"$dp\" \"$op\""
    }

    private fun getFileName(uri: Uri): String? {
        var n: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) { val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) n = c.getString(i) }
        }
        return n
    }

    private fun getRealPath(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        val docId = try {
            when {
                DocumentsContract.isDocumentUri(this, uri) -> DocumentsContract.getDocumentId(uri)
                DocumentsContract.isTreeUri(uri)           -> DocumentsContract.getTreeDocumentId(uri)
                else -> null
            }
        } catch (_: Exception) { null }
        docId?.let { id ->
            if (id.startsWith("primary:")) return "/storage/emulated/0/${id.removePrefix("primary:")}"
            val p = id.split(":"); if (p.size == 2) return "/storage/${p[0]}/${p[1]}"
        }
        return null
    }
}
