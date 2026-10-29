package app.lumio.player.crash

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Process
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

/** Plain-Views crash screen (no Compose) running in its own process so it works even when the app is broken. */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = CrashReporter.readCrash(this)
        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0B0B14.toInt())
            setPadding(pad, pad * 3, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "Lumio crashed"
            setTextColor(0xFFFFFFFF.toInt()); setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f); setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Tap Copy or Share and send this report to fix it. Nothing is sent automatically."
            setTextColor(0xFFB8B8CC.toInt()); setPadding(0, pad / 2, 0, pad)
        })
        val scroll = ScrollView(this)
        scroll.addView(TextView(this).apply {
            text = report
            setTextIsSelectable(true)
            setTextColor(0xFFCFCFE0.toInt()); setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f); typeface = Typeface.MONOSPACE
        })
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        fun button(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label; isAllCaps = false; setOnClickListener { onClick() }
        }
        val row1 = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, pad, 0, 0) }
        row1.addView(button("Copy") {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Lumio crash", report))
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        })
        row1.addView(button("Share") {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, report) }, "Share crash report"))
        })
        row1.addView(button("Restart") { restartApp(false) })
        root.addView(row1)
        root.addView(button("Reset app data & restart (keeps your music files)") { restartApp(true) })
        setContentView(root)
    }

    private fun restartApp(reset: Boolean) {
        if (reset) {
            try {
                deleteDatabase("lumio.db")
                File(filesDir, "datastore").deleteRecursively()
                File(filesDir, "last_crash.txt").delete()
            } catch (_: Throwable) {}
        }
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(it)
        }
        finishAndRemoveTask()
        Process.killProcess(Process.myPid())
    }
}
