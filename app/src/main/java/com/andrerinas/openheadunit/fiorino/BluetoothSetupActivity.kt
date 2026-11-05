package com.andrerinas.openheadunit.fiorino

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import com.andrerinas.openheadunit.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.utils.BluetoothHelper
import com.andrerinas.openheadunit.utils.Settings
import com.andrerinas.openheadunit.utils.ToastUtils
import com.andrerinas.openheadunit.utils.bluetooth.BluetoothPeerDiscovery

/** The system owns pairing consent. Native AA owns reconnection after a saved phone is selected. */
class BluetoothSetupActivity : AppCompatActivity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = VehicleSettingsLayout(this, "Kablosuz bağlantı", "İlk eşleştirme ve otomatik yeniden bağlantı")
        val column = screen.section("Eşleştir", "Telefon eşleştirme", "Bluetooth ile başlat, Wi-Fi ile devam et.", R.drawable.ic_bluetooth_white_24dp)
        status = screen.label("").apply { setPadding(0, 0, 0, screen.dp(12)); column.addView(this) }
        fun button(label: String, primary: Boolean = false, action: () -> Unit) {
            column.addView(screen.action(label, primary, action), LinearLayout.LayoutParams(-1, -2))
        }
        button("Telefonun görebilmesi için görünür yap", primary = true) { makeDiscoverable() }
        column.addView(screen.label("").apply {
            text = "Telefondan Bluetooth aramasını aç ve teybin adını seç. Eşleştirmeyi iki cihazda onayla. Sonra aşağıdan telefonunu kaydet. Görüntü ve Android Auto sesi Wi-Fi üzerinden taşınır.\n\nTelefonun Bluetooth kaydında Telefon aramaları açık, Medya sesi kapalı olsun. Böylece görüşme bağlantısı korunur ve Bluetooth medya ses eşitlemesi devreye girmez."
            setPadding(0, screen.dp(14), 0, screen.dp(14))
        })
        button("Eşleşen telefonumu seç ve arka plan bağlantısını başlat") { choosePhone() }
        button("Teybin Bluetooth ayarları") {
            try { startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
            catch (_: Exception) { message("Teybin fabrika Bluetooth uygulamasını aç.") }
        }
        button("Geri dön") { finish() }
    }
    override fun onResume() {
        super.onResume()
        val settings = App.provide(this).settings
        status.text = "Kayıtlı telefon ${settings.nativePreferredDeviceMac.ifBlank { "henüz seçilmedi" }}\nBluetooth görünürlüğü için sistem onayı gerekir. Ayrı OEM modüllerinde eşleştirme teybin fabrika Bluetooth uygulamasından yapılabilir."
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
                    settings.nativePreferredDeviceMac = peer.address
                    settings.nativePokeBtMacs = setOf(peer.address)
                    settings.nativePokeAllPairedDevices = false
                    settings.autoStartBluetoothDeviceMacs = setOf(peer.address)
                    Settings.syncAutoStartBtMacsToDeviceStorage(this, setOf(peer.address))
                    settings.autoStartOnBoot = true
                    settings.autoStartOnScreenOn = true
                    settings.wifiConnectionMode = com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode.NATIVE
                    ContextCompat.startForegroundService(this, Intent(this, AapService::class.java).setAction(AapService.ACTION_START_WIRELESS))
                    status.text = "${peer.name ?: peer.address} kaydedildi. Kablosuz bağlantı arka planda dinleniyor."
                }.show()
        } catch (_: Exception) { message("Eşleşen cihazlar okunamadı. Bluetooth izinlerini ve teybin modülünü kontrol et.") }
    }
    private fun message(text: String) = ToastUtils.showToast(this, text, Toast.LENGTH_LONG, force = true)
}
