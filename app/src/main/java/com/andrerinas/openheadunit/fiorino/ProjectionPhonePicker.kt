package com.andrerinas.openheadunit.fiorino

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.DriverCandidatePolicy
import com.andrerinas.openheadunit.utils.ToastUtils
import com.andrerinas.openheadunit.utils.BluetoothHelper
import com.andrerinas.openheadunit.utils.bluetooth.BluetoothPeerDiscovery
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** A protocol choice belongs to a phone address, never to its display name. */
object ProjectionPhonePicker {
    private fun prefs(context: Context) = context.getSharedPreferences("projection-phones", Context.MODE_PRIVATE)
    fun carPlaySelected(context: Context) = prefs(context).getString("selected-protocol", null) == "CARPLAY"
    fun selectAndroidAuto(context: Context) { prefs(context).edit().putString("selected-protocol", "ANDROID_AUTO").apply() }
    private var switching = false

    fun show(activity: FragmentActivity, legacy: (() -> Unit)? = null) {
        if (switching) return
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 71)
            message(activity, "Bluetooth iznini verip bağlantı düğmesine tekrar dokun.")
            return
        }
        val snapshot = BluetoothPeerDiscovery.read(activity)
        val devices = snapshot.androidDevices.associateBy { it.address }
        val peers = snapshot.inventory.peers.filter { peer ->
            val device = devices[peer.address] ?: return@filter true
            val result = BluetoothHelper.classifyDevice(device, DriverCandidatePolicy.Pin.NONE)
            result.verdict != DriverCandidatePolicy.Verdict.NOT_A_PHONE || result.reason == DriverCandidatePolicy.Reason.IOS_CARPLAY
        }
        val builder = MaterialAlertDialogBuilder(activity).setTitle("Hangi telefona bağlanmak istiyorsun?")
        if (peers.isEmpty()) builder.setMessage("Kayıtlı telefon yok. İlk eşleştirme için telefon türünü seç.")
        else builder.setItems(peers.map { peer ->
            val ios = DriverCandidatePolicy.isCarPlayPhone(devices[peer.address]?.uuids?.map { it.uuid.toString() })
            val protocol = if (ios) "CarPlay" else when (prefs(activity).getString("protocol.${peer.address}", null)) {
                "ANDROID_AUTO" -> "Android Auto"; "CARPLAY" -> "CarPlay"; else -> "Telefon türünü seç"
            }
            "${peer.name ?: "Telefon"} • $protocol"
        }.toTypedArray()) { _, index ->
            val peer = peers[index]
            val ios = DriverCandidatePolicy.isCarPlayPhone(devices[peer.address]?.uuids?.map { it.uuid.toString() })
            val stored = prefs(activity).getString("protocol.${peer.address}", null)
            if (ios || stored == "CARPLAY") connect(activity, peer.address, true)
            else if (stored == "ANDROID_AUTO") connect(activity, peer.address, false)
            else MaterialAlertDialogBuilder(activity).setTitle("${peer.name ?: "Telefon"} hangi tür?")
                .setItems(arrayOf("Android • Android Auto", "iPhone • CarPlay")) { _, choice ->
                    connect(activity, peer.address, choice == 1)
                }.show()
        }
        builder.setPositiveButton("Yeni telefon eşleştir") { _, _ ->
            MaterialAlertDialogBuilder(activity).setTitle("Telefon türü")
                .setItems(arrayOf("Android Auto", "CarPlay • deneysel")) { _, choice ->
                    if (choice == 0) {
                        prefs(activity).edit().putString("selected-protocol", "ANDROID_AUTO").apply()
                        activity.startActivity(Intent(activity, BluetoothSetupActivity::class.java))
                    } else launchCarPlay(activity, null, discover = true)
                }.show()
        }.setNegativeButton("Vazgeç", null)
        legacy?.let { callback -> builder.setNeutralButton("Diğer bağlantı yöntemleri") {
            _, _ -> prefs(activity).edit().putString("selected-protocol", "ANDROID_AUTO").apply(); callback()
        } }
        builder.show()
    }

    private fun connect(activity: FragmentActivity, mac: String, carPlay: Boolean) {
        if (carPlay && Build.VERSION.SDK_INT < 24) { message(activity, "CarPlay için Android 7.0 veya üzeri gerekli."); return }
        val protocol = if (carPlay) "CARPLAY" else "ANDROID_AUTO"
        prefs(activity).edit().putString("protocol.$mac", protocol).putString("selected-protocol", protocol).apply()
        if (carPlay) launchCarPlay(activity, mac, false)
        else {
            App.provide(activity).settings.configureNativeReceiver(mac)
            ContextCompat.startForegroundService(activity, Intent(activity, AapService::class.java)
                .setAction(AapService.ACTION_NATIVE_AA_POKE).putExtra(AapService.EXTRA_MAC, mac))
        }
    }

    private fun launchCarPlay(activity: FragmentActivity, mac: String?, discover: Boolean) {
        if (Build.VERSION.SDK_INT < 24) { message(activity, "CarPlay için Android 7.0 veya üzeri gerekli."); return }
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.NEARBY_WIFI_DEVICES
        val missing = needed.filter { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            activity.requestPermissions(missing.toTypedArray(), 72)
            message(activity, "Ağ iznini verip CarPlay telefonunu tekrar seç.")
            return
        }
        prefs(activity).edit().putString("selected-protocol", "CARPLAY").apply()
        switching = true
        activity.lifecycleScope.launch {
            try {
                // Stop reconnect before touching the old interface. Do not run two receivers on P2P.
                AapService.instance?.prepareForCarPlay()
                withContext(Dispatchers.IO) { App.provide(activity).commManager.disconnectForLinkLoss(3000) }
                val ready = awaitWifiRelease(activity)
                if (!ready) { message(activity, "Android Auto ağı kapanmadı. Bağlantıyı durdurup tekrar dene."); return@launch }
                activity.startActivity(Intent().setClassName(activity, "com.yusufcelik.carplaylab.CarPlayReceiverActivity")
                    .putExtra("selectedPhoneMac", mac).putExtra("startReceiver", !discover).putExtra("discover", discover))
            } catch (_: Exception) { message(activity, "CarPlay açılamadı. Bağlantı izinlerini kontrol et.") }
            finally { switching = false }
        }
    }

    private suspend fun awaitWifiRelease(context: Context): Boolean {
        val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager ?: return true
        val channel = manager.initialize(context, context.mainLooper, null) ?: return false
        return try {
            withTimeoutOrNull(6000) {
                while (true) {
                    val absent = suspendCancellableCoroutine<Boolean> { continuation ->
                        manager.requestGroupInfo(channel) { group -> if (continuation.isActive) continuation.resume(group == null) }
                    }
                    if (absent) break
                    delay(150)
                }
                true
            } ?: false
        } finally { if (Build.VERSION.SDK_INT >= 27) channel.close() }
    }
    private fun message(context: Context, text: String) = ToastUtils.showToast(context, text, android.widget.Toast.LENGTH_LONG, force = true)
}
