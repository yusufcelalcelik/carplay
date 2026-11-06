package com.andrerinas.openheadunit.bridge

import java.io.*
import org.junit.Assert.*
import org.junit.Test

class BridgeFramesTest {
    private fun frame(flags: Int, size: Int): ByteArray {
        val extra = if (flags and 3 == 1) 4 else 0
        return byteArrayOf(2, flags.toByte(), (size shr 8).toByte(), size.toByte()) +
            ByteArray(extra + size) { (it * 17).toByte() }
    }
    @Test fun `all fragment flag combinations survive split TCP reads without exposing partial USB frames`() {
        for (flags in 0..15) for (size in listOf(0, 1, 257, 65535)) {
            val expected = frame(flags, size)
            val input = object : ByteArrayInputStream(expected) {
                override fun read(b: ByteArray, off: Int, len: Int) = super.read(b, off, minOf(3, len))
            }
            val out = ByteArrayOutputStream()
            assertTrue(BridgeFrames.forwardFrame(input, out))
            assertArrayEquals(expected, out.toByteArray())
            assertFalse(BridgeFrames.forwardFrame(input, out))
        }
    }
    @Test fun `two coalesced TCP frames remain two USB writes`() {
        val a = frame(9, 300); val b = frame(11, 20)
        val input = ByteArrayInputStream(a + b)
        val writes = mutableListOf<ByteArray>()
        val output = object : OutputStream() {
            override fun write(b: Int) { fail("Unexpected partial write") }
            override fun write(b: ByteArray) { writes += b.copyOf() }
        }
        assertTrue(BridgeFrames.forwardFrame(input, output)); assertTrue(BridgeFrames.forwardFrame(input, output))
        assertEquals(2, writes.size); assertArrayEquals(a, writes[0]); assertArrayEquals(b, writes[1])
    }
    @Test fun `truncated header or body never reaches USB`() {
        val full = frame(13, 15)
        for (n in 1 until full.size) {
            val output = ByteArrayOutputStream()
            try { BridgeFrames.forwardFrame(ByteArrayInputStream(full.copyOf(n)), output); fail("Expected EOF") }
            catch (_: EOFException) { assertEquals(0, output.size()) }
        }
    }
    @Test fun `accessory identity refuses unrelated USB accessories`() {
        assertTrue(BridgeMode.accessorySupported("Android", "Android Auto"))
        assertTrue(BridgeMode.accessorySupported("Android", "Android Open Automotive Protocol"))
        assertFalse(BridgeMode.accessorySupported("Google", "DemoKit"))
        assertFalse(BridgeMode.accessorySupported(null, null))
    }
}
