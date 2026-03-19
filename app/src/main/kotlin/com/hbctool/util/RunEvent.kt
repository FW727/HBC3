package com.hbctool.util

sealed class RunEvent {
    data class Log(val line: String)          : RunEvent()
    data class Progress(val label: String)    : RunEvent()
    object Success                             : RunEvent()
    data class Failure(val reason: String)    : RunEvent()
}
