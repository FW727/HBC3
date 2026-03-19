package com.hbctool.util

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/**
 * Advanced Termux detector.
 *
 * Termux can be installed in many ways:
 *  - Play Store (com.termux)
 *  - F-Droid (com.termux)
 *  - Termux:Boot, Termux:API forks
 *  - Samsung Good Lock, multi-user profiles
 *  - Custom package names (some modified APKs)
 *
 * This scanner tries every known method to locate the Termux binary directory.
 */
object TermuxFinder {

    // ── All known Termux package names ────────────────────────────────────
    private val TERMUX_PACKAGES = listOf(
        "com.termux",
        "com.termux.fdroid",
        "com.termux.debug",
        "com.termux.x11",         // Termux:X11
        "com.termux.api",         // Termux:API
    )

    // ── All possible base paths ────────────────────────────────────────────
    private val TERMUX_DATA_BASES = listOf(
        "/data/data/com.termux",
        "/data/user/0/com.termux",
        "/data/user_de/0/com.termux",
        "/data/data/com.termux.fdroid",
        "/data/user/0/com.termux.fdroid",
    )

    private val USR_SUBPATH = "files/usr"
    private val BIN_SUBPATH = "files/usr/bin"

    data class TermuxEnv(
        val packageName: String,
        val dataPath: String,
        val usrPath: String,
        val binPath: String,
        val pythonPath: String?,
        val pipPath: String?,
        val hbctoolPath: String?,
    ) {
        val hasPython   get() = pythonPath != null
        val hasHbctool  get() = hbctoolPath != null
        val isReady     get() = hasPython && hasHbctool
    }

    // ── Main scan ─────────────────────────────────────────────────────────

    /**
     * Scan all locations and return the best Termux environment found,
     * or null if Termux cannot be found.
     */
    fun findBest(ctx: Context): TermuxEnv? {
        val candidates = mutableListOf<TermuxEnv>()

        // Method 1: check all known data paths directly
        for (base in TERMUX_DATA_BASES) {
            val env = probeBase(base, packageNameFromPath(base)) ?: continue
            candidates += env
        }

        // Method 2: ask PackageManager for installed Termux variants
        for (pkg in TERMUX_PACKAGES) {
            if (!isPackageInstalled(ctx, pkg)) continue
            // Try common data paths for this package
            for (base in listOf(
                "/data/data/$pkg",
                "/data/user/0/$pkg",
                "/data/user_de/0/$pkg",
            )) {
                if (candidates.any { it.dataPath == base }) continue
                val env = probeBase(base, pkg) ?: continue
                candidates += env
            }
        }

        // Method 3: brute-force scan /data/data/ for any dir with files/usr/bin/python
        try {
            File("/data/data").listFiles()?.forEach { dir ->
                if (candidates.any { it.dataPath == dir.absolutePath }) return@forEach
                val env = probeBase(dir.absolutePath, dir.name) ?: return@forEach
                if (env.hasPython) candidates += env
            }
        } catch (_: Exception) {}

        // Method 4: check /data/user/0/ too
        try {
            File("/data/user/0").listFiles()?.forEach { dir ->
                if (candidates.any { it.dataPath == dir.absolutePath }) return@forEach
                val env = probeBase(dir.absolutePath, dir.name) ?: return@forEach
                if (env.hasPython) candidates += env
            }
        } catch (_: Exception) {}

        if (candidates.isEmpty()) return null

        // Score: prefer (has hbctool) > (has python) > (known package name)
        return candidates.maxByOrNull { env ->
            var score = 0
            if (env.hasHbctool) score += 100
            if (env.hasPython)  score += 50
            if (env.packageName.startsWith("com.termux")) score += 10
            score
        }
    }

    /** Quick check — returns true if ANY Python is found */
    fun hasPython(ctx: Context) = findBest(ctx)?.hasPython == true

    /** Quick check — returns true if hbctool is available */
    fun hasHbctool(ctx: Context) = findBest(ctx)?.hasHbctool == true

    // ── Probe a single base path ──────────────────────────────────────────

    private fun probeBase(base: String, pkg: String): TermuxEnv? {
        val usrDir = File(base, USR_SUBPATH)
        val binDir = File(base, BIN_SUBPATH)
        if (!binDir.exists()) return null   // Not a Termux prefix

        val python = findExecutable(binDir, listOf("python3", "python", "python3.11", "python3.10"))
        val pip    = findExecutable(binDir, listOf("pip3", "pip", "pip3.11"))
        val hbctool = findBinary(binDir, "hbctool")

        return TermuxEnv(
            packageName  = pkg,
            dataPath     = base,
            usrPath      = usrDir.absolutePath,
            binPath      = binDir.absolutePath,
            pythonPath   = python,
            pipPath      = pip,
            hbctoolPath  = hbctool,
        )
    }

    private fun findExecutable(binDir: File, names: List<String>): String? =
        names.map { File(binDir, it) }.firstOrNull { it.exists() && it.canExecute() }?.absolutePath

    private fun findBinary(binDir: File, name: String): String? {
        // Direct match
        File(binDir, name).takeIf { it.exists() }?.let { return it.absolutePath }
        // Search all bin files for a match by name
        try {
            binDir.listFiles()?.firstOrNull { it.name == name }?.let { return it.absolutePath }
        } catch (_: Exception) {}
        // Check usr/local/bin too
        val parentPath = binDir.parent ?: return null
        val localBin = File(parentPath, "local/bin")
        if (localBin.exists()) {
            File(localBin, name).takeIf { it.exists() }?.let { return it.absolutePath }
        }
        return null
    }

    private fun packageNameFromPath(path: String) =
        path.substringAfterLast("/").takeIf { it.contains(".") } ?: "com.termux"

    private fun isPackageInstalled(ctx: Context, pkg: String) = try {
        ctx.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    // ── Install hbctool via pip ───────────────────────────────────────────

    fun installHbctool(env: TermuxEnv, onLine: (String) -> Unit): Boolean {
        val pip    = env.pipPath
        val python = env.pythonPath ?: return false.also { onLine("✗ Python not found") }

        val args = if (pip != null)
            arrayOf(pip, "install", "--quiet", "hbctool")
        else
            arrayOf(python, "-m", "pip", "install", "--quiet", "hbctool")

        return try {
            val pb = ProcessBuilder(*args).apply {
                redirectErrorStream(true)
                environment()["PATH"] = "${env.binPath}:/system/bin:/system/xbin"
                environment()["HOME"] = env.dataPath
            }
            val proc = pb.start()
            proc.inputStream.bufferedReader().use { r ->
                r.forEachLine { onLine(it) }
            }
            val code = proc.waitFor()
            // Re-probe hbctool after install
            val ok = findBinary(File(env.binPath), "hbctool") != null
            ok.also { if (!ok) onLine("✗ pip exited $code, hbctool not found") }
        } catch (e: Exception) {
            onLine("✗ Error: ${e.message}"); false
        }
    }
}
