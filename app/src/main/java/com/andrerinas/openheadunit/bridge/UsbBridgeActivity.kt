package com.andrerinas.openheadunit.bridge

import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.andrerinas.openheadunit.R
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.fiorino.VehiclePreferencesActivity
import com.andrerinas.openheadunit.fiorino.ProjectionPhonePicker
import com.andrerinas.openheadunit.main.settings.SettingItem
import com.andrerinas.openheadunit.utils.BluetoothHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*

/** User-visible USB consent entry, also opened by an Android Auto accessory attach. */
class UsbBridgeActivity : VehiclePreferencesActivity() {
    override val titleResource = R.string.fiorino_usb_bridge
    private val prefs by lazy { getSharedPreferences("usb-bridge", 0) }
    private val usb by lazy { getSystemService(Context.USB_SERVICE) as UsbManager }
    private val handler = Handler(Looper.getMainLooper())
    private var registered = false
    private var starting = false
    private val refresh = object : Runnable { override fun run() { render(); handler.postDelayed(this, 1000) } }
    private val permission = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == "$packageName.BRIDGE_USB_PERMISSION") {
                if (i.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) beginSaved()
                else android.widget.Toast.makeText(c, "USB izni verilmedi", 1).show()
            }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        ContextCompat.registerReceiver(this, permission, IntentFilter("$packageName.BRIDGE_USB_PERMISSION"), ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
        if (intent.action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED && BridgeMode.enabled(this)) {
            if (!prefs.getBoolean("ask-each-time", true) && prefs.contains("phone")) beginSaved()
            else handler.post { choosePhone() }
        }
    }
    override fun onResume() { super.onResume(); refresh.run() }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
    override fun onDestroy() { if (registered) unregisterReceiver(permission); super.onDestroy() }
    private fun render() {
        showPreferences(listOf(
            SettingItem.InfoBanner("bridgeIntro", R.string.fiorino_usb_bridge,
                "Eski Android telefonu aracın kablolu Android Auto USB girişine tak. Sürücü telefonu bu cihaza kablosuz bağlanır. Bu mod iPhone → fabrika CarPlay köprüsü sağlamaz; mevcut Android teyp CarPlay alıcısı ayrı çalışır."),
            SettingItem.InfoBanner("bridgeState", R.string.fiorino_native_info, UsbBridgeService.status),
            SettingItem.SettingEntry("bridgePhone", R.string.fiorino_bridge_choose, "Sürücü telefonunu seç / değiştir") { choosePhone() },
            SettingItem.SettingEntry("bridgeStart", R.string.fiorino_bridge_start, "Son seçilen Android telefonu bağla") { beginSaved() },
            SettingItem.SettingEntry("bridgeAsk", R.string.fiorino_bridge_auto,
                if (prefs.getBoolean("ask-each-time", true)) "USB takılınca telefon seç • açık" else "USB takılınca son telefonu bağla") {
                prefs.edit().putBoolean("ask-each-time", !prefs.getBoolean("ask-each-time", true)).apply(); render()
            },
            SettingItem.SettingEntry("bridgePair", R.string.fiorino_bridge_pair, "Yeni sürücü telefonunu bir kez eşleştir") {
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            },
            SettingItem.SettingEntry("bridgeStop", R.string.fiorino_bridge_stop, "Köprüyü durdur ve normal alıcıya dön") {
                UsbBridgeService.stop(this)
                ProjectionPhonePicker.selectAndroidAuto(this)
                render()
            },
            SettingItem.InfoBanner("bridgeLimits", R.string.fiorino_native_info,
                "Önizleme: Ford Kuga üzerinde henüz doğrulanmadı. Geçişten sonra teyp yeni oturumu açmazsa USB kablosunu çıkarıp tak. Telefonun kendi Android Auto uygulaması USB’yi alıyorsa USB uygulama seçicisinden Fiorino Auto’yu seç. Araç yılı/SYNC sürümü ve eski telefon modeli uyumluluk için gereklidir.")
        ))
    }
    private fun choosePhone() {
        if (!permissions()) return
        val adapter = BluetoothHelper.getBluetoothAdapter(this)
        val phones = adapter?.bondedDevices.orEmpty().filter { device ->
            !com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.DriverCandidatePolicy.isCarPlayPhone(device.uuids?.map { it.uuid.toString() })
        }.sortedBy { it.name ?: it.address }
        if (phones.isEmpty()) {
            MaterialAlertDialogBuilder(this).setMessage("Önce sürücü Android telefonunu bu eski telefonla Bluetooth üzerinden eşleştir.")
                .setPositiveButton("Bluetooth ayarları") { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
                .setNegativeButton("Kapat", null).show(); return
        }
        MaterialAlertDialogBuilder(this).setTitle("Hangi Android telefona bağlanılsın?")
            .setItems(phones.map { it.name ?: "Android telefon" }.toTypedArray()) { _, index ->
                if (UsbBridgeService.running) {
                    MaterialAlertDialogBuilder(this).setTitle("Telefon değiştirilsin mi?")
                        .setMessage("Mevcut oturum kapanacak. Araç USB bağlantısını kendiliğinden yenilemezse kabloyu çıkarıp takman gerekecek.")
                        .setPositiveButton("Değiştir") { _, _ -> saveAndStart(phones[index].address) }.setNegativeButton("Vazgeç", null).show()
                } else saveAndStart(phones[index].address)
            }.setNegativeButton("Vazgeç", null).show()
    }
    private fun saveAndStart(mac: String) { prefs.edit().putString("phone", mac).apply(); beginSaved() }
    private fun permissions(): Boolean {
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 31) needed += listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE)
        if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.NEARBY_WIFI_DEVICES
        val missing = needed.filter { ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) { requestPermissions(missing.toTypedArray(), 150); return false }
        return true
    }
    private fun beginSaved() {
        if (starting || !permissions()) return
        if (!packageManager.hasSystemFeature("android.hardware.wifi.direct") ||
            !packageManager.hasSystemFeature("android.hardware.usb.accessory") ||
            BluetoothHelper.getBluetoothAdapter(this)?.isEnabled != true) {
            MaterialAlertDialogBuilder(this).setMessage("USB aksesuar ve Wi-Fi Direct destekleyen bir Android telefon gerekli. Wi-Fi, Bluetooth ve konumu aç.")
                .setPositiveButton("Tamam", null).show(); return
        }
        if (!prefs.contains("phone")) { choosePhone(); return }
        val accessory = usb.accessoryList?.firstOrNull { BridgeMode.accessorySupported(it.manufacturer, it.model) }
        if (accessory == null) {
            MaterialAlertDialogBuilder(this).setMessage("Aracın Android Auto destekli USB girişine veri kablosuyla bağla. Şu an Android Auto USB aksesuarı görülmüyor.")
                .setPositiveButton("Tamam", null).show(); return
        }
        if (!usb.hasPermission(accessory)) {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            usb.requestPermission(accessory, PendingIntent.getBroadcast(this, 150, Intent("$packageName.BRIDGE_USB_PERMISSION").setPackage(packageName), flags))
            return
        }
        starting = true
        lifecycleScope.launch {
            try {
                if (!ProjectionPhonePicker.releaseCarPlay(this@UsbBridgeActivity, includeBridge = false)) return@launch
                // Prevent normal receiver boot/Bluetooth triggers from competing for this radio.
                BridgeMode.select(this@UsbBridgeActivity)
                AapService.instance?.prepareForCarPlay()
                stopService(Intent(this@UsbBridgeActivity, AapService::class.java))
                val released = withTimeoutOrNull(6000) { while (AapService.instance != null) delay(100); true } == true
                if (!released) return@launch
                ContextCompat.startForegroundService(this@UsbBridgeActivity, Intent(this@UsbBridgeActivity, UsbBridgeService::class.java)
                    .putExtra("phone", prefs.getString("phone", null)))
            } finally { starting = false; render() }
        }
    }
}
