package com.andrerinas.openheadunit.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Appends the live app log to a file on a USB flash drive, found on its own. Waits for a drive to
 * be plugged in, keeps going if one is swapped, and flushes every second so pulling the drive
 * loses at most that. Capture itself costs one volatile read per log line while this is off.
 */
object UsbLogWriter {
    private const val DIR_NAME = "FiorinoLogs"
    private const val MAX_FILE_BYTES = 10L * 1024 * 1024
    private const val KEEP_FILES = 5

    @Volatile var status: String = "Kapalı"
        private set
    @Volatile private var thread: Thread? = null

    val running: Boolean get() = thread != null

    @Synchronized
    fun start(context: Context) {
        if (thread != null) return
        val app = context.applicationContext
        LogRing.acquire()
        status = "Flash bellek bekleniyor"
        thread = Thread({ loop(app) }, "UsbLogWriter").apply { isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        val t = thread ?: return
        thread = null
        t.interrupt()
        try { t.join(2000) } catch (_: InterruptedException) {}
        LogRing.release()
        status = "Kapalı"
    }

    private fun loop(context: Context) {
        var writer: BufferedWriter? = null
        var written = 0L
        var seq = 0L
        var sinceProbe = 5
        try {
            while (!Thread.currentThread().isInterrupted) {
                if (writer == null && ++sinceProbe >= 5) {
                    sinceProbe = 0
                    val dir = findDirectory(context)
                    if (dir != null) {
                        try {
                            writer = open(dir)
                            written = 0L
                            seq = 0L
                            status = "Yazıyor: ${dir.path}"
                        } catch (e: Exception) { status = "Flash açılamadı: ${e.message}" }
                    }
                }
                val w = writer
                if (w != null) {
                    try {
                        val (last, lines) = LogRing.since(seq)
                        for (line in lines) { w.write(line); w.write("\n"); written += line.length + 1 }
                        seq = last
                        w.flush()
                        if (written > MAX_FILE_BYTES) { w.close(); writer = null; sinceProbe = 5 }
                    } catch (e: Exception) {
                        // The drive was pulled or filled up: wait for the next one.
                        try { w.close() } catch (_: Exception) {}
                        writer = null
                        status = "Flash çıkarıldı veya dolu, yenisi bekleniyor"
                    }
                }
                Thread.sleep(1000)
            }
        } catch (_: InterruptedException) {
        } finally {
            try { writer?.flush(); writer?.close() } catch (_: Exception) {}
        }
    }

    private fun open(dir: File): BufferedWriter {
        trim(dir)
        val name = "fiorino_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".log"
        val w = BufferedWriter(OutputStreamWriter(FileOutputStream(File(dir, name), true), Charsets.UTF_8))
        w.write("# ${Build.MANUFACTURER} ${Build.MODEL} Android ${Build.VERSION.RELEASE} ${Build.DISPLAY}\n")
        w.flush()
        return w
    }

    private fun trim(dir: File) {
        dir.listFiles { f -> f.name.startsWith("fiorino_") && f.name.endsWith(".log") }
            ?.sortedBy { it.lastModified() }?.dropLast(KEEP_FILES - 1)?.forEach { it.delete() }
    }

    private fun findDirectory(context: Context): File? {
        val internal = HashSet<String>()
        try { internal.add(Environment.getExternalStorageDirectory().canonicalPath) } catch (_: Exception) {}
        // App-specific folders on removable volumes need no storage permission. [0] is internal.
        val appDirs = ContextCompat.getExternalFilesDirs(context, null).drop(1).filterNotNull()
            .filter { UsbVolumeFinder.isWritable(it) }
        val root = appDirs.firstOrNull()
            ?: UsbVolumeFinder.candidates(UsbVolumeFinder.DEFAULT_ROOTS.map(::File), internal).firstOrNull()
            ?: return null
        val dir = File(root, DIR_NAME)
        return if (dir.isDirectory || dir.mkdirs()) dir else null
    }
}
