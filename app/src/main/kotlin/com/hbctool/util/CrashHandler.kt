package com.hbctool.util

import android.content.Context
import android.content.Intent
import com.hbctool.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler : Thread.UncaughtExceptionHandler {
    private var default: Thread.UncaughtExceptionHandler? = null
    private lateinit var ctx: Context

    fun install(context: Context) {
        ctx = context.applicationContext
        default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            ctx.startActivity(Intent(ctx, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_TRACE, sw.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        } catch (_: Exception) {
            default?.uncaughtException(t, e)
        }
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
