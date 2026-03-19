package com.hbctool.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object HbcRunner {

    fun disasm(ctx: Context, hbcUri: Uri, outDirUri: Uri): Flow<RunEvent> = flow {

        // ── 1. Find Termux env ────────────────────────────────────────────
        emit(RunEvent.Progress("Scanning for Termux…"))
        val env = HbcSetup.env(ctx)
        if (env == null) {
            emit(RunEvent.Failure("Termux not found.\n\nInstall Termux from F-Droid and run:\n  pkg install python")); return@flow
        }
        emit(RunEvent.Log("▸ Termux: ${env.binPath}"))

        val python  = env.pythonPath  ?: run { emit(RunEvent.Failure("Python not found in Termux.\n\nIn Termux: pkg install python")); return@flow }
        val hbctool = env.hbctoolPath ?: run { emit(RunEvent.Failure("hbctool not installed.\n\nGo to ⚙ Setup → Install hbctool.")); return@flow }

        // ── 2. Copy input to cache ────────────────────────────────────────
        emit(RunEvent.Progress("Copying bundle to cache…"))
        val inputFile = copyUriToCache(ctx, hbcUri)
            ?: run { emit(RunEvent.Failure("Cannot read input file.")); return@flow }
        emit(RunEvent.Log("▸ ${inputFile.name}  (${inputFile.length() / 1024} KB)"))

        // ── 3. Output dir in cache ────────────────────────────────────────
        val outDir = File(ctx.cacheDir, "hasm_out_${System.currentTimeMillis()}").also {
            it.deleteRecursively(); it.mkdirs()
        }

        // ── 4. Run hbctool disasm ─────────────────────────────────────────
        emit(RunEvent.Progress("Disassembling bytecode…"))
        val exitCode = runProcess(env, python, hbctool, "disasm",
            inputFile.absolutePath, outDir.absolutePath
        ) { emit(RunEvent.Log(it)) }

        if (exitCode != 0) {
            emit(RunEvent.Failure("hbctool exited with code $exitCode"))
            cleanup(inputFile, outDir); return@flow
        }

        // ── 5. Count & save ───────────────────────────────────────────────
        val files = outDir.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) {
            emit(RunEvent.Failure("No output generated.\nFile may not be a valid Hermes bundle."))
            cleanup(inputFile, outDir); return@flow
        }
        emit(RunEvent.Progress("Saving ${files.size} file(s)…"))
        val saved = copyDirToTree(ctx, outDir, outDirUri, inputFile.nameWithoutExtension)

        emit(RunEvent.Log(""))
        emit(RunEvent.Log("✓ $saved file(s) saved successfully"))
        emit(RunEvent.Success)
        cleanup(inputFile, outDir)

    }.flowOn(Dispatchers.IO)

    fun asm(ctx: Context, hasmDirUri: Uri, outFileUri: Uri): Flow<RunEvent> = flow {

        emit(RunEvent.Progress("Scanning for Termux…"))
        val env = HbcSetup.env(ctx)
        if (env == null) {
            emit(RunEvent.Failure("Termux not found.\n\nInstall Termux from F-Droid and run:\n  pkg install python")); return@flow
        }
        emit(RunEvent.Log("▸ Termux: ${env.binPath}"))

        val python  = env.pythonPath  ?: run { emit(RunEvent.Failure("Python not found.\n\nIn Termux: pkg install python")); return@flow }
        val hbctool = env.hbctoolPath ?: run { emit(RunEvent.Failure("hbctool not installed.\n\nGo to ⚙ Setup → Install hbctool.")); return@flow }

        emit(RunEvent.Progress("Copying .hasm files to cache…"))
        val inDir = File(ctx.cacheDir, "hasm_in_${System.currentTimeMillis()}").also {
            it.deleteRecursively(); it.mkdirs()
        }
        val copied = copyTreeToDir(ctx, hasmDirUri, inDir)
        if (copied == 0) { emit(RunEvent.Failure("No files found in selected folder.")); return@flow }
        emit(RunEvent.Log("Copied $copied file(s) ✓"))

        val outHbc = File(ctx.cacheDir, "asm_${System.currentTimeMillis()}.hbc").also { it.delete() }

        emit(RunEvent.Progress("Assembling bytecode…"))
        val exitCode = runProcess(env, python, hbctool, "asm",
            inDir.absolutePath, outHbc.absolutePath
        ) { emit(RunEvent.Log(it)) }

        if (exitCode != 0) {
            emit(RunEvent.Failure("hbctool exited with code $exitCode"))
            cleanup(inDir, outHbc); return@flow
        }
        if (!outHbc.exists() || outHbc.length() == 0L) {
            emit(RunEvent.Failure("Output file was not created.")); cleanup(inDir, outHbc); return@flow
        }

        emit(RunEvent.Progress("Writing output file…"))
        ctx.contentResolver.openOutputStream(outFileUri)?.use { outHbc.inputStream().copyTo(it) }
            ?: run { emit(RunEvent.Failure("Cannot write to output file.")); cleanup(inDir, outHbc); return@flow }

        emit(RunEvent.Log(""))
        emit(RunEvent.Log("✓ ${outHbc.length() / 1024} KB assembled successfully"))
        emit(RunEvent.Success)
        cleanup(inDir, outHbc)

    }.flowOn(Dispatchers.IO)

    // ── Process runner ────────────────────────────────────────────────────

    private suspend fun runProcess(
        env: HbcSetup.Env,
        vararg cmd: String,
        onLine: suspend (String) -> Unit
    ): Int {
        val pb = ProcessBuilder(*cmd).apply {
            redirectErrorStream(true)
            environment().apply {
                put("PATH", "${env.binPath}:/system/bin:/system/xbin")
                put("HOME", File(env.binPath).parentFile?.parentFile?.absolutePath ?: "/")
                put("PREFIX", "${File(env.binPath).parent ?: env.binPath}")
            }
        }
        return try {
            val proc = pb.start()
            proc.inputStream.bufferedReader().use { r ->
                r.forEachLine { line ->
                    if (line.isNotBlank()) kotlinx.coroutines.runBlocking { onLine(line) }
                }
            }
            proc.waitFor()
        } catch (e: Exception) { onLine("Error: ${e.message}"); -1 }
    }

    // ── File helpers ──────────────────────────────────────────────────────

    private fun copyUriToCache(ctx: Context, uri: Uri): File? = try {
        val name = getFileName(ctx, uri) ?: "input.bundle"
        val dest = File(ctx.cacheDir, name)
        ctx.contentResolver.openInputStream(uri)?.use { it.copyTo(dest.outputStream()) }
        dest.takeIf { it.exists() && it.length() > 0 }
    } catch (_: Exception) { null }

    private fun copyTreeToDir(ctx: Context, treeUri: Uri, dest: File): Int {
        var n = 0
        val tree = DocumentFile.fromTreeUri(ctx, treeUri) ?: return 0
        fun rec(doc: DocumentFile, local: File) {
            doc.listFiles().forEach { c ->
                if (c.isDirectory) rec(c, File(local, c.name ?: "d").also { it.mkdirs() })
                else {
                    ctx.contentResolver.openInputStream(c.uri)
                        ?.use { it.copyTo(File(local, c.name ?: "f").outputStream()) }
                    n++
                }
            }
        }
        rec(tree, dest); return n
    }

    private fun copyDirToTree(ctx: Context, src: File, treeUri: Uri, folderName: String): Int {
        val tree = DocumentFile.fromTreeUri(ctx, treeUri) ?: return 0
        val sub = tree.findFile(folderName)?.takeIf { it.isDirectory }
            ?: tree.createDirectory(folderName) ?: return 0
        var n = 0
        fun write(dir: File, docDir: DocumentFile) {
            dir.listFiles()?.forEach { f ->
                if (f.isDirectory) {
                    val d = docDir.findFile(f.name)?.takeIf { it.isDirectory }
                        ?: docDir.createDirectory(f.name) ?: return@forEach
                    write(f, d)
                } else {
                    docDir.findFile(f.name)?.delete()
                    val doc = docDir.createFile("application/octet-stream", f.name) ?: return@forEach
                    ctx.contentResolver.openOutputStream(doc.uri)?.use { f.inputStream().copyTo(it) }
                    n++
                }
            }
        }
        write(src, sub); return n
    }

    private fun getFileName(ctx: Context, uri: Uri): String? {
        var n: String? = null
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) { val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) n = c.getString(i) }
        }
        return n
    }

    private fun cleanup(vararg f: File) = f.forEach { if (it.isDirectory) it.deleteRecursively() else it.delete() }
}
