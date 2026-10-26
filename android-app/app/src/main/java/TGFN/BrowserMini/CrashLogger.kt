package TGFN.BrowserMini

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val TAG = "TGFN.Crash"
    private const val FILE = "crash.log"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                log(app, thread, throwable)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun log(context: Context, thread: Thread, throwable: Throwable) {
        val text = buildReport(thread, throwable)
        Log.e(TAG, text)
        try {
            val file = File(context.filesDir, FILE)
            file.appendText(text + "\n\n")
            // Keep last ~200KB
            if (file.length() > 200_000) {
                val tail = file.readText().takeLast(120_000)
                file.writeText(tail)
            }
        } catch (_: Exception) {
        }
    }

    fun lastCrash(context: Context): String? {
        return try {
            val file = File(context.filesDir, FILE)
            if (!file.exists() || file.length() == 0L) null else file.readText().takeLast(8000)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return buildString {
            appendLine("========== TGFN CRASH ==========")
            appendLine("time: $time")
            appendLine("thread: ${thread.name} id=${thread.id}")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android: ${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
            appendLine("exception: ${throwable.javaClass.name}: ${throwable.message}")
            appendLine("cause: ${throwable.cause?.javaClass?.name}: ${throwable.cause?.message}")
            appendLine("--- stack ---")
            append(sw.toString())
            appendLine("================================")
        }
    }
}
