package io.github.cpatcher.arch

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {

    private val logFile = File(Environment.getExternalStorageDirectory(), "cpatcher_debug.log")
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    fun d(tag: String, msg: String) {
        val line = "${dateFormat.format(Date())} [$tag] $msg"
        android.util.Log.d("Cpatcher", line)
        try { logFile.appendText("$line\n") } catch (_: Exception) { }
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        val line = "${dateFormat.format(Date())} [$tag] ERROR: $msg${tr?.let { " -> ${it.message}" } ?: ""}"
        android.util.Log.e("Cpatcher", line)
        try {
            logFile.appendText("$line\n")
            tr?.let { logFile.appendText("${it.stackTraceToString()}\n") }
        } catch (_: Exception) { }
    }

    fun clear() {
        try { logFile.writeText("") } catch (_: Exception) { }
    }

    fun getLogPath(): String = logFile.absolutePath
}
