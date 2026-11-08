package com.andrerinas.openheadunit.fiorino

import android.content.Context
import android.os.IBinder
import android.os.Parcel
import android.provider.Settings
import java.io.IOException

/** The Microntek logical knob and the amplifier use different scales. */
internal class MicrontekVolume private constructor(
    private val context: Context,
    private val service: IBinder,
    val maximum: Int,
) {
    data class Reading(val step: Int, val muted: Boolean)

    fun read(): Reading {
        val step = Settings.System.getInt(context.contentResolver, VOLUME, -1)
        if (step !in 0..maximum) throw IOException("Microntek volume position unavailable")
        val muted = query(service, MUTE)?.trim()?.toBooleanStrictOrNull()
            ?: throw IOException("Microntek mute status unavailable")
        return Reading(step, muted)
    }

    fun write(step: Int) {
        require(step in 0..maximum)
        val result = transact(service, SET_PARAMETER, "$VOLUME${amplifierLevel(step, maximum)}") { readInt() }
        if (result < 0) throw IOException("Microntek rejected the volume command ($result)")
    }

    companion object {
        private const val DESCRIPTOR = "android.microntek.ICarService"
        private const val VOLUME = "av_volume="
        private const val MUTE = "av_mute="
        private const val SET_PARAMETER = 17
        private const val GET_PARAMETER = 18

        // MTCD carservice contract: f1xpl/MtcdAutoVolume (ICarService and VolumeLevelManager).
        // Probe the actual service and readable scale before sending any volume command.
        fun open(context: Context): MicrontekVolume? {
            val service = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java)
                .invoke(null, "carservice") as? IBinder ?: return null
            if (service.interfaceDescriptor != DESCRIPTOR) return null
            val maximum = query(service, "cfg_maxvolume")?.trim()?.toIntOrNull()
                ?.takeIf { it in 1..100 } ?: return null
            return MicrontekVolume(context, service, maximum).also { it.read() }
        }

        internal fun amplifierLevel(step: Int, maximum: Int): Int {
            val percent = 100f * step / maximum
            return when {
                percent < 20f -> percent * 1.5f
                percent < 50f -> percent + 10f
                else -> percent * 0.8f + 20f
            }.toInt()
        }

        private fun query(service: IBinder, parameter: String): String? =
            transact(service, GET_PARAMETER, parameter) { readString() }

        private fun <T> transact(service: IBinder, code: Int, parameter: String, read: Parcel.() -> T): T {
            val request = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                request.writeInterfaceToken(DESCRIPTOR)
                request.writeString(parameter)
                if (!service.transact(code, request, reply, 0)) throw IOException("Microntek volume call unavailable")
                reply.readException()
                return reply.read()
            } finally {
                reply.recycle()
                request.recycle()
            }
        }
    }
}
