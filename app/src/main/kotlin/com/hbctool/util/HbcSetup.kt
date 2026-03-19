package com.hbctool.util

import android.content.Context
import java.io.File

/**
 * Single source of truth for Termux environment.
 * Priority: user-saved path → auto-detection.
 */
object HbcSetup {

    data class Env(
        val binPath:    String,
        val pythonPath: String?,
        val pipPath:    String?,
        val hbctoolPath: String?,
        val source: String        // "saved" or "auto"
    ) {
        val hasPython  get() = pythonPath  != null
        val hasHbctool get() = hbctoolPath != null
        val isReady    get() = hasPython && hasHbctool
    }

    private var cached: Env? = null

    fun invalidate() { cached = null }

    fun env(ctx: Context): Env? {
        return cached ?: buildEnv(ctx).also { cached = it }
    }

    private fun buildEnv(ctx: Context): Env? {
        // 1. User-saved path takes priority
        val saved = PrefsManager.getTermuxPath(ctx)
        if (saved != null) {
            val env = probeDir(saved, "saved")
            if (env != null) return env
            // Saved path no longer valid — clear it
            PrefsManager.clearTermuxPath(ctx)
        }
        // 2. Auto-detect
        return TermuxFinder.findBest(ctx)?.let { tfEnv ->
            Env(
                binPath     = tfEnv.binPath,
                pythonPath  = tfEnv.pythonPath,
                pipPath     = tfEnv.pipPath,
                hbctoolPath = tfEnv.hbctoolPath,
                source      = "auto (${tfEnv.packageName})"
            )
        }
    }

    /** Build Env from a user-supplied bin path string */
    fun probeDir(binPath: String, source: String = "manual"): Env? {
        val bin = File(binPath)
        if (!bin.exists()) return null
        val python   = listOf("python3", "python").map { File(bin, it) }.firstOrNull { it.canExecute() }?.absolutePath
        val pip      = listOf("pip3", "pip").map    { File(bin, it) }.firstOrNull { it.canExecute() }?.absolutePath
        val hbctool  = File(bin, "hbctool").takeIf  { it.exists() }?.absolutePath
        // Must at least have a directory with something
        return if (bin.list()?.isNotEmpty() == true)
            Env(binPath, python, pip, hbctool, source)
        else null
    }

    fun installHbctool(ctx: Context, onLine: (String) -> Unit): Boolean {
        val e = env(ctx) ?: run { onLine("Termux not found."); return false }
        val pip    = e.pipPath
        val python = e.pythonPath ?: run { onLine("Python not found."); return false }

        val args = if (pip != null)
            arrayOf(pip, "install", "--quiet", "hbctool")
        else
            arrayOf(python, "-m", "pip", "install", "--quiet", "hbctool")

        return try {
            val pb = ProcessBuilder(*args).apply {
                redirectErrorStream(true)
                environment().apply {
                    put("PATH", "${e.binPath}:/system/bin:/system/xbin")
                    put("HOME", File(e.binPath).parent?.let { File(it).parent } ?: "/")
                }
            }
            val proc = pb.start()
            proc.inputStream.bufferedReader().use { r -> r.forEachLine { onLine(it) } }
            val code = proc.waitFor()
            invalidate()
            val ok = env(ctx)?.hasHbctool == true
            if (!ok) onLine("pip exit $code — hbctool not found after install")
            ok
        } catch (ex: Exception) { onLine("Error: ${ex.message}"); false }
    }
}
