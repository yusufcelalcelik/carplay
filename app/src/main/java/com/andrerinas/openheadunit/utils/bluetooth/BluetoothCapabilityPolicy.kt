package com.andrerinas.openheadunit.utils.bluetooth

/**
 * What this unit's Android Bluetooth can actually do, decided from what was observed on it rather
 * than from its model name.
 *
 * Many head units (Microntek/HCT MTCD-MTCE, FYT and others) keep the phone link on a separate
 * serial module that the MCU drives. Android still hands out a BluetoothAdapter object there, but
 * it never reaches STATE_ON, or it turns on and cannot publish an RFCOMM record. Wireless Android
 * Auto needs that record, so each of these has a different honest answer for the user.
 */
object BluetoothCapabilityPolicy {

    enum class Verdict {
        /** Nothing has been tried on this unit yet. */
        UNKNOWN,
        /** An Android Auto listener has opened here. */
        WORKING,
        /** Android reports no Bluetooth adapter at all. */
        NO_ADAPTER,
        /** The adapter accepts the request and never turns on: no controller behind it. */
        NEVER_TURNS_ON,
        /** The radio is on but refuses to publish a server record. */
        NO_RFCOMM_SERVER
    }

    enum class Advice {
        NONE,
        /** The manufacturer's own Bluetooth service is reachable: try that route. */
        MODULE_ROUTE,
        /** No usable radio in the unit: an old phone on USB can carry the wireless side. */
        USB_BRIDGE
    }

    /**
     * @param adapterPresent whether Android returned an adapter
     * @param enableAccepted what enable() answered, or null when it was not asked
     * @param reachedOn whether STATE_ON was seen within the wait, or null when not waited for
     * @param listenOpened whether a server socket opened, or null when not tried
     * @param listenFailures consecutive failed attempts to open one with the radio on
     */
    fun classify(
        adapterPresent: Boolean,
        enableAccepted: Boolean? = null,
        reachedOn: Boolean? = null,
        listenOpened: Boolean? = null,
        listenFailures: Int = 0,
        maxListenFailures: Int = 4
    ): Verdict = when {
        !adapterPresent -> Verdict.NO_ADAPTER
        listenOpened == true -> Verdict.WORKING
        reachedOn == false -> Verdict.NEVER_TURNS_ON
        enableAccepted == false && reachedOn != true -> Verdict.NEVER_TURNS_ON
        listenFailures >= maxListenFailures -> Verdict.NO_RFCOMM_SERVER
        else -> Verdict.UNKNOWN
    }

    /** A later success always wins; a failure never downgrades a unit that has worked to unknown. */
    fun merge(previous: Verdict, observed: Verdict): Verdict =
        if (observed == Verdict.UNKNOWN) previous else observed

    fun advice(verdict: Verdict, moduleServiceAvailable: Boolean): Advice = when (verdict) {
        Verdict.UNKNOWN, Verdict.WORKING -> Advice.NONE
        else -> if (moduleServiceAvailable) Advice.MODULE_ROUTE else Advice.USB_BRIDGE
    }

    /**
     * Whether the app asks and switches the radio on itself instead of opening the system consent.
     *
     * The system activity is the right default. It is skipped only where it has already failed on
     * this unit, or on the firmware family known to leave it on "Turning on" for good. From
     * Android 13 an app may no longer switch the radio, so the system activity is all there is.
     */
    fun enablesInApp(sdkInt: Int, knownHangingFirmware: Boolean, systemConsentFailedBefore: Boolean): Boolean =
        sdkInt < 33 && (knownHangingFirmware || systemConsentFailedBefore)

    fun parse(stored: String?): Verdict =
        Verdict.values().firstOrNull { it.name == stored } ?: Verdict.UNKNOWN
}
