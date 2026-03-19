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

class DisasmActivity : BaseActivity() {

    private var hbcUri: Uri? = null
    private var outputDirUri: Uri? = null

    private lateinit var tvHbcPath: MaterialTextView
    private lateinit var tvOutputDir: MaterialTextView
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
    private lateinit var cardHbc: AnimatedBorderCardView
    private lateinit var cardOut: AnimatedBorderCardView

    private val pickHbc = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        hbcUri = uri
        tvHbcPath.text = getFileName(uri) ?: uri.lastPathSegment ?: uri.toString()
        rebuildCommand(); refreshRunBtn()
    }

    private val pickOutputDir = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        outputDirUri = uri
        tvOutputDir.text = uri.lastPathSegment ?: uri.toString()
        rebuildCommand(); refreshRunBtn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disasm)

        tvHbcPath   = findViewById(R.id.tvHbcPath)
        tvOutputDir = findViewById(R.id.tvOutputDir)
        tvCommand   = findViewById(R.id.tvCommand)
        btnRun      = findViewById(R.id.btnRunInApp)
        btnCopy     = findViewById(R.id.btnCopyCommand)
        btnTermux   = findViewById(R.id.btnRunTermux)
        progress    = findViewById(R.id.progressDisasm)
        tvProgress  = findViewById(R.id.tvProgressLabel)
        tvLog       = findViewById(R.id.tvLog)
        logScroll   = findViewById(R.id.logScroll)
        logCard     = findViewById(R.id.logCard)
        tvLogStatus = findViewById(R.id.tvLogStatus)
        cardHbc     = findViewById(R.id.cardSelectHbc)
        cardOut     = findViewById(R.id.cardSelectOutput)

        tvHbcPath.text   = getString(R.string.no_file_yet)
        tvOutputDir.text = getString(R.string.no_dir_yet)
        btnRun.isEnabled    = false
        btnCopy.isEnabled   = false
        btnTermux.isEnabled = false

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSelectHbc).setOnClickListener { pickHbc.launch(arrayOf("*/*")) }
        findViewById<MaterialButton>(R.id.btnSelectOutputDir).setOnClickListener { pickOutputDir.launch(null) }

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
        val uri    = hbcUri    ?: return
        val outUri = outputDirUri ?: return

        tvLog.text = ""; tvLogStatus.visibility = View.GONE
        logCard.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        tvProgress.visibility = View.VISIBLE
        setInputsEnabled(false)
        cardHbc.setLoading(true); cardOut.setLoading(true)

        val sb = StringBuilder()
        lifecycleScope.launch {
            HbcRunner.disasm(this@DisasmActivity, uri, outUri).collect { event ->
                when (event) {
                    is RunEvent.Progress -> { tvProgress.text = event.label; log(sb, "▶ ${event.label}") }
                    is RunEvent.Log      -> if (event.line.isNotBlank()) log(sb, event.line)
                    is RunEvent.Success  -> done(sb, success = true)
                    is RunEvent.Failure  -> { log(sb, "\n✗ ${event.reason}"); done(sb, success = false) }
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
        cardHbc.setLoading(false); cardOut.setLoading(false)
    }

    private fun log(sb: StringBuilder, line: String) {
        if (sb.isNotEmpty()) sb.append('\n'); sb.append(line)
    }

    private fun setInputsEnabled(on: Boolean) {
        btnRun.isEnabled = on; btnCopy.isEnabled = on; btnTermux.isEnabled = on
        findViewById<MaterialButton>(R.id.btnSelectHbc).isEnabled = on
        findViewById<MaterialButton>(R.id.btnSelectOutputDir).isEnabled = on
    }

    private fun refreshRunBtn() {
        val ready = hbcUri != null && outputDirUri != null
        btnRun.isEnabled = ready; btnCopy.isEnabled = ready; btnTermux.isEnabled = ready
    }

    private fun rebuildCommand() {
        val hbc = hbcUri ?: return
        val out = outputDirUri ?: return
        val hbcPath = getRealPath(hbc) ?: hbc.path ?: return
        val outPath = getRealPath(out) ?: out.path ?: return
        val name = getFileName(hbc)?.substringBeforeLast('.') ?: "output"
        tvCommand.text = "hbctool disasm \"$hbcPath\" \"$outPath/$name\""
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
