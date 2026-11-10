package com.andrerinas.openheadunit.utils

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UsbVolumeFinderTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun `finds a usb disk two levels down and ranks it first`() {
        val root = tmp.newFolder("mnt")
        File(root, "usb_storage/USB_DISK2").mkdirs()
        File(root, "other").mkdirs()
        File(root, "emulated").mkdirs()
        val found = UsbVolumeFinder.candidates(listOf(root))
        assertEquals("USB_DISK2", found.first().name)
        assertTrue(found.none { it.name == "emulated" })
    }

    @Test fun `skips internal storage and an empty root`() {
        val root = tmp.newFolder("storage")
        val internal = File(root, "XXXX-1111").also { it.mkdirs() }
        assertTrue(UsbVolumeFinder.candidates(listOf(root), setOf(internal.canonicalPath)).isEmpty())
        assertTrue(UsbVolumeFinder.candidates(listOf(File(root, "missing"))).isEmpty())
    }

    @Test fun `writer drains the ring to a file and survives repeat reads`() {
        LogRing.acquire()
        try {
            LogRing.offer(4, "one"); LogRing.offer(4, "two")
            val (seq, lines) = LogRing.since(0)
            assertEquals(2, lines.size)
            LogRing.offer(4, "three")
            assertEquals(1, LogRing.since(seq).second.size)
        } finally { LogRing.release() }
    }
}
