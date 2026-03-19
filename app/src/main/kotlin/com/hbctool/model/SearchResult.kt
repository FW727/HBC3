package com.hbctool.model

data class SearchResult(
    val fileName: String,
    val lineNumber: Int,
    val lineContent: String,
    val keyword: String
)
