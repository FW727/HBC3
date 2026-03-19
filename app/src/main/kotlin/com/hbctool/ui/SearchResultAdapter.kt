package com.hbctool.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.hbctool.R
import com.hbctool.model.SearchResult

class SearchResultAdapter(
    private val items: List<SearchResult>
) : RecyclerView.Adapter<SearchResultAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvFile:    TextView = view.findViewById(R.id.tvResultFile)
        val tvLine:    TextView = view.findViewById(R.id.tvResultLine)
        val tvContent: TextView = view.findViewById(R.id.tvResultContent)
        val card:      MaterialCardView = view as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.tvFile.text = r.fileName
        holder.tvLine.text = holder.itemView.context.getString(R.string.label_line, r.lineNumber)

        // Highlight keyword in the line
        val content = r.lineContent.trim()
        val spanned = SpannableString(content)
        val lower = content.lowercase()
        val kwLower = r.keyword.lowercase()
        var idx = lower.indexOf(kwLower)
        while (idx >= 0) {
            spanned.setSpan(
                ForegroundColorSpan(0xFF00C8FF.toInt()),
                idx, idx + r.keyword.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spanned.setSpan(
                StyleSpan(Typeface.BOLD),
                idx, idx + r.keyword.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            idx = lower.indexOf(kwLower, idx + 1)
        }
        holder.tvContent.text = spanned

        // Long click → copy line to clipboard
        holder.card.setOnLongClickListener {
            val cm = it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("hasm_line", content))
            Snackbar.make(it, "Copied!", Snackbar.LENGTH_SHORT).show()
            true
        }
    }
}
