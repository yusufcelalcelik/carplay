package com.andrerinas.openheadunit.utils

import java.io.File

/**
 * Finds a writable removable volume (a USB flash drive) on a head unit whose mount point is not
 * known in advance: Microntek/HCT units mount drives under names like /mnt/usb_storage/USB_DISK2
 * or /storage/usbotg, other units under /storage/XXXX-XXXX.
 */
object UsbVolumeFinder {
    val DEFAULT_ROOTS = listOf("/storage", "/mnt", "/mnt/usb_storage", "/mnt/media_rw")

    /** Internal storage and system mount points that are never a flash drive. */
    private val SKIP = setOf(
        "emulated", "self", "sdcard0", "sdcard", "shell", "secure", "asec", "obb", "runtime",
        "user", "knox-emulated", "container", "sdcard_internal", "usb_storage_root"
    )

    /** Writable candidate directories, drives named like USB first. Depth 2 below each root. */
    fun candidates(roots: List<File>, internal: Set<String> = emptySet()): List<File> {
        val seen = HashSet<String>()
        val found = ArrayList<File>()
        fun visit(dir: File, depth: Int) {
            val children = try { dir.listFiles() } catch (_: SecurityException) { null } ?: return
            for (child in children) {
                if (!child.isDirectory || child.name.lowercase() in SKIP || child.name.startsWith(".")) continue
                val canonical = try { child.canonicalPath } catch (_: Exception) { child.path }
                if (internal.any { canonical == it || canonical.startsWith("$it/") }) continue
                if (!seen.add(canonical)) continue
                if (isWritable(child)) found.add(child)
                if (depth < 2) visit(child, depth + 1)
            }
        }
        roots.forEach { visit(it, 1) }
        // /mnt/usb_storage is only the parent of the drive: keep the drive itself.
        val drives = found.filter { rank(it) == 0 }
        return found.filter { f -> drives.none { it != f && it.path.startsWith(f.path + "/") } }
            .sortedWith(compareBy({ rank(it) }, { it.path.count { c -> c == '/' } }))
    }

    private fun rank(dir: File): Int {
        val n = dir.path.lowercase()
        return when {
            "usb" in n || "udisk" in n -> 0
            else -> 1
        }
    }

    fun isWritable(dir: File): Boolean {
        if (!dir.canWrite()) return false
        return try {
            val probe = File(dir, ".fiorino_probe")
            probe.createNewFile() || probe.exists()
            probe.delete()
            true
        } catch (_: Exception) { false }
    }
}
