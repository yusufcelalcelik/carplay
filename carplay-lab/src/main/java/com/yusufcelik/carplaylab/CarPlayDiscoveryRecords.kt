package com.yusufcelik.carplaylab

import java.io.Closeable
import java.util.UUID

/** Discovery-only records. They provide no authentication or projection transport. */
internal class CarPlayDiscoveryRecords<T : Closeable> private constructor(val records: List<T>) : Closeable {
    private var closed = false
    @Synchronized override fun close() {
        if (closed) return
        closed = true
        records.asReversed().forEach { runCatching { it.close() } }
    }
    companion object {
        // Accessory-side identifiers, distinct from the iPhone-side UUIDs used for outbound bootstrap.
        // Primary implementation reference: catplay-labs/catplay bluetooth/catplay_bt/src/iap2.rs
        val ACCESSORY_IAP2 = UUID.fromString("00000000-deca-fade-deca-deafdecacaff")
        val ACCESSORY_CARPLAY = UUID.fromString("ec884348-cd41-40a2-9727-575d50bf1fd3")
        fun <T : Closeable> open(register: (String, UUID) -> T): CarPlayDiscoveryRecords<T> {
            val iap = register("Fiorino iAP2 discovery test", ACCESSORY_IAP2)
            try {
                return CarPlayDiscoveryRecords(listOf(iap,
                    register("Fiorino CarPlay discovery test", ACCESSORY_CARPLAY)))
            } catch (error: Exception) {
                runCatching { iap.close() }
                throw error
            }
        }
    }
}
