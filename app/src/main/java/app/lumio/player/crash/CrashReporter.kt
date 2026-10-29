package app.lumio.player.crash

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Catches ANY crash, saves a full report (stack trace + last logcat lines) and shows it on a crash screen
 * (separate process) with Copy / Share buttons. Non-fatal background errors are appended to errors.log.
 */
object CrashReporter {
    private const val CRASH_FILE = "last_crash.txt"
    private const val ERROR_LOG = "errors.log"

    fun isCrashProcess(): Boolean =
        Build.VERSION.SDK_INT >= 28 && (Application.getProcessName() ?: "").endsWith(":crash")

    fun install(app: Application) {
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                File(app.filesDir, CRASH_FILE).writeText(buildReport(app, thread, error))
                app.startActivity(
                    Intent(app, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                Thread.sleep(400)
            } catch (_: Throwable) {
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    /** Background errors that must not kill the app. */
    fun logNonFatal(tag: String, e: Throwable) {
        Log.e(tag, "non-fatal", e)
        try {
            val ctx = appContext ?: return
            val f = File(ctx.filesDir, ERROR_LOG)
            if (f.exists() && f.length() > 100_000) f.delete()
            f.appendText("[${stamp()}] $tag\n${Log.getStackTraceString(e)}\n\n")
        } catch (_: Throwable) {
        }
    }

    @Volatile private var appContext: Context? = null
    fun init(context: Context) { appContext = context.applicationContext }

    fun readCrash(ctx: Context): String {
        val crash = File(ctx.filesDir, CRASH_FILE).takeIf { it.exists() }?.readText() ?: "No crash report found."
        val errors = File(ctx.filesDir, ERROR_LOG).takeIf { it.exists() }?.readText()?.takeLast(20_000)
        return if (errors.isNullOrBlank()) crash else crash + "\n\n===== NON-FATAL ERRORS =====\n" + errors
    }

    private fun stamp() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    private fun buildReport(ctx: Context, thread: Thread, e: Throwable): String {
        val ver = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (_: Throwable) { "?" }
        val logcat = try {
            Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "-t", "250"))
                .inputStream.bufferedReader().readText().takeLast(30_000)
        } catch (_: Throwable) { "(logcat unavailable)" }
        return buildString {
            appendLine("LUMIO CRASH REPORT  ${stamp()}")
            appendLine("version=$ver  android=${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT})  device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("thread=${thread.name}")
            appendLine()
            appendLine(Log.getStackTraceString(e))
            appendLine("===== LAST LOGCAT LINES =====")
            appendLine(logcat)
        }
    }
}
