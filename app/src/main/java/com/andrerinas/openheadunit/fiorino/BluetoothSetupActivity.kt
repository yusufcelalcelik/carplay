package com.andrerinas.openheadunit.fiorino

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.Manifest
import android.app.Activity
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
import com.andrerinas.openheadunit.aap.VehicleTypePolicy
import com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.ExternalBtTransportPolicy
import com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.NativeAaHandshakeManager
import com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.hct.HctServiceObserver
import com.andrerinas.openheadunit.utils.BluetoothHelper
import com.andrerinas.openheadunit.utils.ToastUtils
import com.andrerinas.openheadunit.utils.bluetooth.BluetoothPeerDiscovery

/** The system owns pairing consent. Native AA owns reconnection after a saved phone is selected. */
class BluetoothSetupActivity : VehiclePreferencesActivity() {
    companion object {
        internal const val EXTRA_NATIVE_PHONE = "fiorino-native-phone"
    }

    private enum class BluetoothAction { START, DISCOVER }

    override val titleResource = R.string.fiorino_connection
    private var connectionMessage: String? = null
    private val pairingOwner = Any()
    private var pairingRequest: Job? = null
    private var legacyEnableRequest: Job? = null
    private var pendingBluetoothAction: BluetoothAction? = null
    private var pendingPhone: String? = null
    private val enableConsent = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val action = pendingBluetoothAction ?: return@registerForActivityResult
        val phone = pendingPhone
        pendingBluetoothAction = null
        pendingPhone = null
        lifecycleScope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                withTimeoutOrNull(4000) {
                    while (!androidRadioEnabled()) delay(150)
                }
            }
            if (androidRadioEnabled()) runBluetoothAction(action, phone)
            else reportBluetoothUnavailable(result.resultCode == Activity.RESULT_CANCELED)
        }
    }
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
        pendingBluetoothAction = savedInstanceState?.getString("pendingBluetoothAction")?.let(BluetoothAction::valueOf)
        pendingPhone = savedInstanceState?.getString("pendingPhone")
        updatePreferences()
        if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_NATIVE_PHONE)?.takeIf(BluetoothAdapter::checkBluetoothAddress)?.let { phone ->
                requestBluetooth(BluetoothAction.START, phone)
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pendingBluetoothAction", pendingBluetoothAction?.name)
        outState.putString("pendingPhone", pendingPhone)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() {
        super.onResume()
        updatePreferences()
    }
    private fun updatePreferences() {
        showPreferences(listOf(
            SettingItem.SettingEntry("usbBridge", R.string.fiorino_usb_bridge,
                "Aracın kablolu Android Auto girişine eski telefonu adaptör olarak bağla") {
                startActivity(Intent(this, com.andrerinas.openheadunit.bridge.UsbBridgeActivity::class.java))
            },
            SettingItem.SettingEntry("carPlayAddress", R.string.fiorino_carplay_address,
                "Otomatik okunamayan alıcı Bluetooth adresini bir kez kaydet") { configureCarPlayAddress() },
            SettingItem.CategoryHeader("native", R.string.fiorino_native_setup),
            SettingItem.SettingEntry("projectionPhone", R.string.fiorino_select_phone,
                "Android Auto veya CarPlay • hangi telefona bağlanmak istiyorsun?") {
                ProjectionPhonePicker.show(this)
            },
            SettingItem.InfoBanner("status", R.string.fiorino_native_info,
                connectionMessage ?: "Kayıtlı telefon ${settings.nativePreferredDeviceMac.ifBlank { "henüz seçilmedi" }}"),
            SettingItem.SettingEntry("nativeSetup", R.string.fiorino_enable_native,
                "Native AA, otomatik açılış ve arka planda yeniden bağlantı") {
                requestBluetooth(BluetoothAction.START)
            },
            SettingItem.SettingEntry("discoverable", R.string.fiorino_discoverable,
                "Alıcıyı açık tutarak 3 dakikalık Android Auto eşleştirme penceresi aç") {
                requestBluetooth(BluetoothAction.DISCOVER)
            },
            SettingItem.SettingEntry("receiverName", R.string.fiorino_receiver_name,
                "Fiorino Auto") { setReceiverName() },
            SettingItem.ToggleSettingEntry("dataOnlyBluetooth", R.string.fiorino_data_only_bluetooth,
                R.string.fiorino_data_only_bluetooth_help, settings.fiorinoDataOnlyBluetooth) {
                settings.fiorinoDataOnlyBluetooth = it
            },
            SettingItem.SettingEntry("selectPhone", R.string.fiorino_select_phone,
                settings.nativePreferredDeviceMac.ifBlank { "Henüz seçilmedi" }) { choosePhone() },
            SettingItem.SettingEntry("manageDevices", R.string.fiorino_select_phone,
                "Telefon kayıtlarını temizle / Bluetooth eşleşmelerini yönet") { ProjectionPhonePicker.manageSavedDevices(this) },
            SettingItem.InfoBanner("instructions", R.string.fiorino_native_info),
            SettingItem.SettingEntry("systemBluetooth", R.string.fiorino_system_bluetooth,
                "Ayrı OEM modülünde fabrika uygulamasından eşleştir") {
                try { startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
                catch (_: Exception) { message("Teybin fabrika Bluetooth uygulamasını aç.") }
            }
        ))
    }
    private fun androidRadioEnabled(): Boolean = runCatching {
        BluetoothHelper.getBluetoothAdapter(this)?.isEnabled == true
    }.getOrDefault(false)

    private fun requestBluetooth(action: BluetoothAction, phone: String? = null) {
        if (pendingBluetoothAction != null || legacyEnableRequest?.isActive == true ||
            !permitted() || App.provide(this).commManager.isConnected) return
        val moduleRoute = NativeAaHandshakeManager.wifiButtonRoute(this) == ExternalBtTransportPolicy.WifiButton.MODULE ||
            com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.esp32.Esp32RelaySession.hasSavedEsp32Relay(this)
        if (androidRadioEnabled() ||
            (action == BluetoothAction.START && moduleRoute)) {
            runBluetoothAction(action, phone)
            return
        }
        // The system consent activity on the tested HCT PX3 can stay on "Turning on"
        // indefinitely. Keep consent and the wait inside this app on that firmware.
        if (Build.VERSION.SDK_INT == 25 && Build.MODEL.contains("px3", ignoreCase = true) &&
            Build.BOARD.equals("rk30sdk", ignoreCase = true) &&
            Build.DISPLAY.contains("eng.hct.", ignoreCase = true)) {
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.fiorino_px3_bt_enable)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.fiorino_px3_bt_try) { _, _ -> tryPx3Bluetooth(action, phone) }
                .show()
            return
        }
        pendingBluetoothAction = action
        pendingPhone = phone
        try {
            enableConsent.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (_: android.content.ActivityNotFoundException) {
            pendingBluetoothAction = null
            pendingPhone = null
            reportBluetoothUnavailable()
        } catch (_: SecurityException) {
            pendingBluetoothAction = null
            pendingPhone = null
            reportBluetoothUnavailable()
        }
    }

    @Suppress("DEPRECATION") // Only the Android 7.1 HCT profile above reaches this path.
    private fun tryPx3Bluetooth(action: BluetoothAction, phone: String?) {
        if (legacyEnableRequest?.isActive == true || App.provide(this).commManager.isConnected) return
        legacyEnableRequest = lifecycleScope.launch {
            connectionMessage = getString(R.string.fiorino_px3_bt_checking)
            updatePreferences()
            val accepted = runCatching {
                val adapter = BluetoothHelper.getBluetoothAdapter(this@BluetoothSetupActivity)
                    ?: return@runCatching false
                when (adapter.state) {
                    BluetoothAdapter.STATE_ON, BluetoothAdapter.STATE_TURNING_ON -> true
                    BluetoothAdapter.STATE_OFF -> adapter.enable()
                    else -> false
                }
            }.getOrDefault(false)
            val enabled = accepted && withTimeoutOrNull(8000) {
                while (!androidRadioEnabled()) delay(150)
                true
            } == true
            if (App.provide(this@BluetoothSetupActivity).commManager.isConnected) return@launch
            if (enabled) runBluetoothAction(action, phone) else reportBluetoothUnavailable()
        }
    }

    private fun reportBluetoothUnavailable(canceled: Boolean = false) {
        NativeAaHandshakeManager.logBluetoothPreflight(this)
        val notice = getString(if (canceled) R.string.fiorino_android_bt_canceled else R.string.fiorino_android_bt_unavailable)
        val moduleStatus = if (HctServiceObserver.available(this)) R.string.fiorino_microntek_detected
            else R.string.fiorino_microntek_unavailable
        connectionMessage = "$notice\n${getString(moduleStatus)}"
        updatePreferences()
        message(notice)
    }

    private fun runBluetoothAction(action: BluetoothAction, phone: String?) {
        if (action == BluetoothAction.DISCOVER) {
            makeDiscoverable()
        } else {
            lifecycleScope.launch {
                settings.configureNativeReceiver(phone)
                connectionMessage = if (startReceiver(phone))
                    "Native kurulum kaydedildi. Telefonunu seçip ayarlardan çık."
                else "Ayarlar kaydedildi. Bağlantı başlatılamadı. İzinleri kontrol et."
                updatePreferences()
            }
        }
    }

    private fun setReceiverName() {
        if (!permitted()) return
        settings.vehicleDisplayName = "Fiorino Auto"
        settings.vehicleType = VehicleTypePolicy.CAR
        val renamed = runCatching {
            val adapter = BluetoothHelper.getBluetoothAdapter(this)
            adapter != null && adapter.isEnabled && (adapter.name == "Fiorino Auto" || adapter.setName("Fiorino Auto"))
        }.getOrDefault(false)
        message(getString(if (renamed) R.string.fiorino_receiver_name_set else R.string.fiorino_receiver_name_projection_only))
    }
    private fun configureCarPlayAddress() {
        val saved = getSharedPreferences("carplay-lab", MODE_PRIVATE)
        val input = android.widget.EditText(this).apply {
            hint = "AA:BB:CC:DD:EE:FF"; setSingleLine(true)
            setText(saved.getString("host-bt-mac", ""))
        }
        val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.fiorino_carplay_address)
            .setMessage("Alıcı olarak kullanılan teybin/Android cihazın kendi Bluetooth adresi. iPhone’un adresini girme. Çalışan CarPlay bağlantısını durdurduktan sonra değiştir.")
            .setView(input).setNegativeButton("Vazgeç", null).setPositiveButton("Kaydet", null).create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val mac = input.text.toString().trim().uppercase()
                if (!com.yusufcelik.carplaylab.ReceiverBluetoothAddress.isValid(mac)) {
                    input.error = "Geçerli alıcı Bluetooth adresi gerekli"; return@setOnClickListener
                }
                if (android.os.Build.VERSION.SDK_INT >= 24 && com.yusufcelik.carplaylab.CarPlaySessionService.isRunning()) {
                    input.error = "Önce CarPlay bağlantısını durdur"; return@setOnClickListener
                }
                saved.edit().putString("host-bt-mac", mac).apply()
                dialog.dismiss(); message("Alıcı adresi kaydedildi. Sonraki bağlantılarda otomatik kullanılacak.")
            }
        }
        dialog.show()
    }
    private suspend fun startReceiver(phone: String? = null): Boolean {
        if (App.provide(this).commManager.isConnected) return true
        if (!ProjectionPhonePicker.releaseCarPlay(this)) {
            message("Önceki bağlantı kapanıyor. Birazdan tekrar dene.")
            return false
        }
        ProjectionPhonePicker.selectAndroidAuto(this)
        val command = Intent(this, AapService::class.java)
        if (phone == null) command.action = AapService.ACTION_START_WIRELESS
        else command.setAction(AapService.ACTION_NATIVE_AA_POKE)
            .putExtra(AapService.EXTRA_MAC, phone).putExtra(AapService.EXTRA_EXPLICIT_PICK, true)
        return try {
            ContextCompat.startForegroundService(this, command)
            true
        } catch (_: Exception) {
            message("Bağlantı başlatılamadı. Arka plan izinlerini kontrol et.")
            false
        }
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
            if (App.provide(this).commManager.isConnected) {
                message("Eşleştirme penceresini açmak için mevcut Android Auto oturumunu kapat.")
                return
            }
            pairingRequest = lifecycleScope.launch {
                try {
                    settings.configureNativeReceiver()
                    if (!startReceiver()) return@launch
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
                    val peer = peers[index]
                    requestBluetooth(BluetoothAction.START, peer.address)
                }.show()
        } catch (_: Exception) { message("Eşleşen cihazlar okunamadı. Bluetooth izinlerini ve teybin modülünü kontrol et.") }
    }
    override fun onDestroy() {
        AapService.instance?.onNativePairingWindow(pairingOwner, false)
        super.onDestroy()
    }
    private fun message(text: String) = ToastUtils.showToast(this, text, Toast.LENGTH_LONG, force = true)
}
