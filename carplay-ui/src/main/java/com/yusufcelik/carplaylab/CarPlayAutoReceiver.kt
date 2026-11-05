package com.yusufcelik.carplaylab

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Build
import android.os.UserManager

/** Boot and known-phone events can arm only the user's already configured CarPlay receiver. */
class CarPlayAutoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < 24 || !context.getSystemService(UserManager::class.java).isUserUnlocked) return
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            @Suppress("DEPRECATION")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
            val selected = context.getSharedPreferences("carplay-lab", Context.MODE_PRIVATE).getString("iphone", null)
            if (runCatching { device.address != selected }.getOrDefault(true)) return
        } else if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) != BluetoothAdapter.STATE_ON) return
        } else if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_USER_UNLOCKED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        CarPlaySessionService.autoStart(context)
    }
}
