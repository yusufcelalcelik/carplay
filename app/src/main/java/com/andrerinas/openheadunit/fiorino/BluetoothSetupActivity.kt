package com.andrerinas.openheadunit.fiorino

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.Manifest
import com.andrerinas.openheadunit.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.main.settings.SettingItem
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.utils.BluetoothHelper
import com.andrerinas.openheadunit.utils.ToastUtils
import com.andrerinas.openheadunit.utils.bluetooth.BluetoothPeerDiscovery

/** The system owns pairing consent. Native AA owns reconnection after a saved phone is selected. */
class BluetoothSetupActivity : VehiclePreferencesActivity() {
    override val titleResource = R.string.fiorino_connection
    private var connectionMessage: String? = null
    private val pairingOwner = Any()
    private var pairingRequest: Job? = null
    private val discoveryConsent = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val visible = runCatching {
            BluetoothHelper.getBluetoothAdapter(this)?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
        }.getOrDefault(false)
        if (result.resultCode > 0 || visible) {
            AapService.instance?.onNativePairingWindow(pairingOwner, true)
        } else {
            AapService.instance?.onNativePairingWindow(pairingOwner, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePreferences()
    }
    override fun onResume() {
        super.onResume()
        updatePreferences()
    }
    private fun updatePreferences() {
        showPreferences(listOf(
            SettingItem.CategoryHeader("native", R.string.fiorino_native_setup),
            SettingItem.SettingEntry("projectionPhone", R.string.fiorino_select_phone,
                "Android Auto veya CarPlay • hangi telefona bağlanmak istiyorsun?") {
                ProjectionPhonePicker.show(this)
            },
            SettingItem.InfoBanner("status", R.string.fiorino_native_info,
                connectionMessage ?: "Kayıtlı telefon ${settings.nativePreferredDeviceMac.ifBlank { "henüz seçilmedi" }}"),
            SettingItem.SettingEntry("nativeSetup", R.string.fiorino_enable_native,
                "Native AA, otomatik açılış ve arka planda yeniden bağlantı") {
                if (permitted()) {
                    settings.configureNativeReceiver()
                    connectionMessage = if (startReceiver())
                        "Native kurulum kaydedildi. Telefonunu seçip ayarlardan çık."
                    else "Ayarlar kaydedildi. Bağlantı başlatılamadı. İzinleri kontrol et."
                    updatePreferences()
                }
            },
            SettingItem.SettingEntry("discoverable", R.string.fiorino_discoverable,
                "Alıcıyı açık tutarak 3 dakikalık Android Auto eşleştirme penceresi aç") { makeDiscoverable() },
            SettingItem.SettingEntry("selectPhone", R.string.fiorino_select_phone,
                settings.nativePreferredDeviceMac.ifBlank { "Henüz seçilmedi" }) { choosePhone() },
            SettingItem.InfoBanner("instructions", R.string.fiorino_native_info),
            SettingItem.SettingEntry("systemBluetooth", R.string.fiorino_system_bluetooth,
                "Ayrı OEM modülünde fabrika uygulamasından eşleştir") {
                try { startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
                catch (_: Exception) { message("Teybin fabrika Bluetooth uygulamasını aç.") }
            }
        ))
    }
    private fun startReceiver(): Boolean = try {
        ProjectionPhonePicker.selectAndroidAuto(this)
        ContextCompat.startForegroundService(this, Intent(this, AapService::class.java).setAction(AapService.ACTION_START_WIRELESS))
        true
    } catch (_: Exception) {
        message("Bağlantı başlatılamadı. Bluetooth ve otomatik başlatma izinlerini kontrol et.")
        false
    }
    private fun permitted(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val permissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        if (permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) return true
        requestPermissions(permissions, 41)
        message("İzni verdikten sonra düğmeye tekrar dokun.")
        return false
    }
    private fun makeDiscoverable() {
        if (!permitted()) return
        try {
            if (BluetoothHelper.getBluetoothAdapter(this) == null) {
                message("Android Bluetooth modülüne erişemiyor. Teybin fabrika Bluetooth uygulamasından eşleştir.")
                return
            }
            if (pairingRequest?.isActive == true) return
            settings.configureNativeReceiver()
            if (!startReceiver()) return
            pairingRequest = lifecycleScope.launch {
                try {
                    var waited = 0
                    while (AapService.instance == null && waited < 5000) { delay(100); waited += 100 }
                    val service = AapService.instance
                    if (service == null) { message("Alıcı hazırlanamadı. Tekrar dene."); return@launch }
                    service.onNativePairingWindow(pairingOwner, true)
                    connectionMessage = "Android Auto alıcısı eşleştirme için hazırlanıyor. Görünürlüğü onayla; telefonda Android Auto → Kablosuz araç bağla ekranını aç. Araç listesinde görünme bu cihazda henüz doğrulanmadı."
                    updatePreferences()
                    try {
                        discoveryConsent.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180))
                    } catch (_: Exception) {
                        service.onNativePairingWindow(pairingOwner, false)
                        message("Görünürlük açılamadı. Teybin Bluetooth modülünü kontrol et.")
                    }
                } finally { pairingRequest = null }
            }
        } catch (_: Exception) { message("Görünürlük açılamadı. Teybin Bluetooth ayarlarını kullan.") }
    }
    private fun choosePhone() {
        if (!permitted()) return
        try {
            val snapshot = BluetoothPeerDiscovery.read(this)
            val devices = snapshot.androidDevices.associateBy { it.address }
            val peers = snapshot.inventory.peers.filter { peer ->
                val device = devices[peer.address]
                device == null || BluetoothHelper.classifyDevice(device,
                    com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.DriverCandidatePolicy.Pin.NONE).reason !=
                    com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.DriverCandidatePolicy.Reason.IOS_CARPLAY
            }
            if (peers.isEmpty()) { message("Önce telefonunu teyple eşleştir, sonra tekrar dene."); return }
            MaterialAlertDialogBuilder(this).setTitle("Android Auto telefonu")
                .setItems(peers.map { "${it.name ?: "Telefon"}\n${it.address}" }.toTypedArray()) { _, index ->
                    val settings = App.provide(this).settings
                    val peer = peers[index]
                    settings.configureNativeReceiver(peer.address)
                    connectionMessage = if (startReceiver())
                        "${peer.name ?: peer.address} kaydedildi. Bağlantıyı başlatmak için ayarlardan çık."
                    else "Telefon kaydedildi. Bağlantıyı başlatmak için izinleri kontrol et."
                    updatePreferences()
                }.show()
        } catch (_: Exception) { message("Eşleşen cihazlar okunamadı. Bluetooth izinlerini ve teybin modülünü kontrol et.") }
    }
    override fun onDestroy() {
        AapService.instance?.onNativePairingWindow(pairingOwner, false)
        super.onDestroy()
    }
    private fun message(text: String) = ToastUtils.showToast(this, text, Toast.LENGTH_LONG, force = true)
}
