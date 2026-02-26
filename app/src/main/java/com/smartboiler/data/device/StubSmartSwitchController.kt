package com.smartboiler.data.device

import android.content.Context
import android.util.Log
import com.smartboiler.domain.device.DeviceType
import com.smartboiler.domain.device.SmartDevice
import com.smartboiler.domain.device.SmartSwitchController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of [SmartSwitchController] for development.
 *
 * Returns mock devices and logs on/off commands. Replace with
 * `GoogleHomeSmartSwitchController` when the Google Home SDK is configured.
 */
@Singleton
class StubSmartSwitchController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SmartSwitchController {

    companion object {
        private const val TAG = "SmartSwitch"
        private const val PREFS_NAME = "smart_switch_prefs"
        private const val KEY_SELECTED_DEVICE = "selected_device_id"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val mockDevices = listOf(
        SmartDevice("mock-1", "Boiler Switch", "Utility Room", false, DeviceType.SWITCH),
        SmartDevice("mock-2", "Kitchen Plug", "Kitchen", true, DeviceType.PLUG),
        SmartDevice("mock-3", "Water Heater", "Bathroom", false, DeviceType.PLUG),
    )

    private var deviceStates = mutableMapOf<String, Boolean>().apply {
        mockDevices.forEach { put(it.id, it.isOn) }
    }

    override suspend fun discoverDevices(): List<SmartDevice> {
        Log.d(TAG, "Discovering devices (stub: returning ${mockDevices.size} mock devices)")
        return mockDevices.map { it.copy(isOn = deviceStates[it.id] ?: false) }
    }

    override suspend fun selectDevice(deviceId: String) {
        Log.d(TAG, "Selected device: $deviceId")
        prefs.edit().putString(KEY_SELECTED_DEVICE, deviceId).apply()
    }

    override suspend fun getSelectedDevice(): SmartDevice? {
        val id = prefs.getString(KEY_SELECTED_DEVICE, null) ?: return null
        return mockDevices.find { it.id == id }?.copy(isOn = deviceStates[id] ?: false)
    }

    override suspend fun turnOn(): Result<Unit> {
        val id = prefs.getString(KEY_SELECTED_DEVICE, null)
            ?: return Result.failure(IllegalStateException("No device selected"))
        Log.i(TAG, "🔥 Turning ON device: $id")
        deviceStates[id] = true
        return Result.success(Unit)
    }

    override suspend fun turnOff(): Result<Unit> {
        val id = prefs.getString(KEY_SELECTED_DEVICE, null)
            ?: return Result.failure(IllegalStateException("No device selected"))
        Log.i(TAG, "❄️ Turning OFF device: $id")
        deviceStates[id] = false
        return Result.success(Unit)
    }

    override suspend fun isConnected(): Boolean {
        return prefs.getString(KEY_SELECTED_DEVICE, null) != null
    }
}
