package com.yusufcelik.carplaylab

import java.io.Closeable
import org.junit.Assert.*
import org.junit.Test

class CarPlayDiscoveryRecordsTest {
    private class Record(private val fail: Boolean = false) : Closeable {
        var closes = 0
        override fun close() { closes++; if (fail) error("synthetic close failure") }
    }
    @Test fun `discovery registers accessory identifiers rather than iPhone identifiers and closes once`() {
        val opened = mutableListOf<Record>()
        val uuids = mutableListOf<String>()
        val records = CarPlayDiscoveryRecords.open { _, uuid ->
            uuids += uuid.toString(); Record().also(opened::add)
        }
        assertEquals(listOf("00000000-deca-fade-deca-deafdecacaff", "ec884348-cd41-40a2-9727-575d50bf1fd3"), uuids)
        records.close(); records.close()
        assertTrue(opened.all { it.closes == 1 })
    }
    @Test fun `partial registration cannot leave an orphan iAP2 record`() {
        val first = Record()
        var calls = 0
        assertThrows(IllegalStateException::class.java) {
            CarPlayDiscoveryRecords.open { _, _ -> if (calls++ == 0) first else error("synthetic refusal") }
        }
        assertEquals(1, first.closes)
    }
    @Test fun `a failing record close cannot leak the other record`() {
        val first = Record(); val second = Record(true)
        var calls = 0
        val group = CarPlayDiscoveryRecords.open { _, _ -> if (calls++ == 0) first else second }
        group.close()
        assertEquals(1, first.closes); assertEquals(1, second.closes)
    }
}
