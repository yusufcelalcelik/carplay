package com.andrerinas.openheadunit.fiorino

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
                "Telefondan Bluetooth aramasını açıp teybi seç") { makeDiscoverable() },
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
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180))
        } catch (_: Exception) { message("Görünürlük açılamadı. Teybin Bluetooth ayarlarını kullan.") }
    }
    private fun choosePhone() {
        if (!permitted()) return
        try {
            val peers = BluetoothPeerDiscovery.read(this).inventory.peers
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
    private fun message(text: String) = ToastUtils.showToast(this, text, Toast.LENGTH_LONG, force = true)
}
