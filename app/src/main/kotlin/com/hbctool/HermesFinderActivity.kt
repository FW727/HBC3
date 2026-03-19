package com.hbctool

import android.net.Uri
import android.util.TypedValue
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.hbctool.model.SearchResult
import com.hbctool.ui.SearchResultAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HermesFinderActivity : BaseActivity() {

    // Predefined keywords common in React Native / Hermes bytecode
    private val predefinedKeywords = listOf(
        "LoadParam", "Ret", "Call", "NewObject", "PutById",
        "GetById", "JmpTrue", "JmpFalse", "LoadConstString",
        "CreateFunction", "Mov", "Add", "Sub", "StrictEq",
        "instanceof", "typeof", "debugger", "eval", "require",
        "AsyncFunction", "Promise", "fetch", "XMLHttpRequest",
        "token", "password", "secret", "api_key", "Authorization"
    )

    private val selectedPredefined = mutableSetOf<String>()
    private val customKeywords     = mutableListOf<String>()
    private val selectedFiles      = mutableListOf<Uri>()

    private lateinit var tvFilesCount:   TextView
    private lateinit var chipGroupPre:   ChipGroup
    private lateinit var chipGroupCust:  ChipGroup
    private lateinit var etCustom:       TextInputEditText
    private lateinit var btnAddCustom:   MaterialButton
    private lateinit var btnSearch:      MaterialButton
    private lateinit var tvResultCount:  TextView
    private lateinit var recycler:       RecyclerView
    private lateinit var progress:       LinearProgressIndicator
    private lateinit var tvStatus:       TextView

    private val pickFiles = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        selectedFiles.clear()
        selectedFiles.addAll(uris)
        tvFilesCount.text = getString(R.string.label_selected_files, uris.size)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hermes_finder)

        tvFilesCount   = findViewById(R.id.tvFilesCount)
        chipGroupPre   = findViewById(R.id.chipGroupPredefined)
        chipGroupCust  = findViewById(R.id.chipGroupCustom)
        etCustom       = findViewById(R.id.etCustomKeyword)
        btnAddCustom   = findViewById(R.id.btnAddKeyword)
        btnSearch      = findViewById(R.id.btnSearch)
        tvResultCount  = findViewById(R.id.tvResultCount)
        recycler       = findViewById(R.id.recyclerResults)
        progress       = findViewById(R.id.progressSearch)
        tvStatus       = findViewById(R.id.tvStatus)

        tvFilesCount.text = getString(R.string.no_files_selected)

        setupPredefinedChips()

        recycler.layoutManager = LinearLayoutManager(this)

        findViewById<MaterialButton>(R.id.btnBackFinder).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSelectHasmFiles).setOnClickListener {
            pickFiles.launch(arrayOf("*/*"))
        }

        btnAddCustom.setOnClickListener {
            val kw = etCustom.text?.toString()?.trim() ?: ""
            if (kw.isNotEmpty() && !customKeywords.contains(kw)) {
                customKeywords.add(kw)
                addCustomChip(kw)
                etCustom.setText("")
            }
        }

        btnSearch.setOnClickListener { doSearch() }
    }

    private fun setupPredefinedChips() {
        predefinedKeywords.forEach { kw ->
            val chip = Chip(this).apply {
                text = kw
                isCheckable = true
                isChecked = false
                setChipBackgroundColorResource(R.color.chip_bg_selector)
                val tv = TypedValue()
                context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true)
                setTextColor(tv.data)
                chipCornerRadius = 8f * resources.displayMetrics.density
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedPredefined.add(kw) else selectedPredefined.remove(kw)
                }
            }
            chipGroupPre.addView(chip)
        }
    }

    private fun addCustomChip(kw: String) {
        val chip = Chip(this).apply {
            text = kw
            isCloseIconVisible = true
            isCheckable = false
            setChipBackgroundColorResource(R.color.chip_custom_bg)
            setTextColor(0xFF00C8FF.toInt())
            chipCornerRadius = 8f * resources.displayMetrics.density
            setOnCloseIconClickListener {
                customKeywords.remove(kw)
                chipGroupCust.removeView(this)
            }
        }
        chipGroupCust.addView(chip)
    }

    private fun doSearch() {
        val keywords = selectedPredefined.toMutableList() + customKeywords
        if (selectedFiles.isEmpty()) {
            Snackbar.make(btnSearch, R.string.no_files_selected, Snackbar.LENGTH_SHORT).show()
            return
        }
        if (keywords.isEmpty()) {
            Snackbar.make(btnSearch, R.string.no_keywords_selected, Snackbar.LENGTH_SHORT).show()
            return
        }

        // UI: searching state
        progress.visibility = View.VISIBLE
        tvStatus.text = getString(R.string.searching)
        tvStatus.visibility = View.VISIBLE
        tvResultCount.visibility = View.GONE
        recycler.adapter = null
        btnSearch.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val results = mutableListOf<SearchResult>()
            selectedFiles.forEach { uri ->
                val name = getFileName(uri) ?: uri.lastPathSegment ?: "?"
                try {
                    contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.useLines { lines ->
                            lines.forEachIndexed { idx, line ->
                                keywords.forEach { kw ->
                                    if (line.contains(kw, ignoreCase = true)) {
                                        results.add(
                                            SearchResult(
                                                fileName    = name,
                                                lineNumber  = idx + 1,
                                                lineContent = line,
                                                keyword     = kw
                                            )
                                        )
                                    }
                                }
                            }
                        }
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                progress.visibility = View.GONE
                btnSearch.isEnabled = true
                if (results.isEmpty()) {
                    tvStatus.text = getString(R.string.no_results)
                    tvResultCount.visibility = View.GONE
                } else {
                    tvStatus.visibility = View.GONE
                    tvResultCount.text = getString(R.string.results_count, results.size)
                    tvResultCount.visibility = View.VISIBLE
                    recycler.adapter = SearchResultAdapter(results)
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = c.getString(idx)
            }
        }
        return name
    }
}
