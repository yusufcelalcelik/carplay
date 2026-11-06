package com.andrerinas.openheadunit.bridge

import com.andrerinas.openheadunit.aap.AapMessageFraming
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream

/** Preserve complete AAP wire frames at the USB boundary. Never decrypt or decode media. */
object BridgeFrames {
    fun forwardFrame(input: InputStream, output: OutputStream): Boolean {
        val first = input.read()
        if (first < 0) return false
        val data = DataInputStream(input)
        val header = byteArrayOf(first.toByte(), data.readByte(), data.readByte(), data.readByte())
        val length = ((header[2].toInt() and 255) shl 8) or (header[3].toInt() and 255)
        val extra = if (AapMessageFraming.carriesTotalLength(header[1].toInt() and 255)) 4 else 0
        val frame = ByteArray(4 + extra + length)
        header.copyInto(frame)
        data.readFully(frame, 4, extra + length)
        output.write(frame)
        return true
    }
}
