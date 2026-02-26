package com.smartboiler.data.device

import android.content.Context
import android.util.Log
import com.smartboiler.data.remote.SmartThingsApiService
import com.smartboiler.data.remote.SmartThingsCommand
import com.smartboiler.data.remote.SmartThingsCommandRequest
import com.smartboiler.domain.device.DeviceType
import com.smartboiler.domain.device.SmartDevice
import com.smartboiler.domain.device.SmartSwitchController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real SmartThings implementation of [SmartSwitchController].
 *
 * Uses the SmartThings REST API v1 with a Personal Access Token (PAT)
 * to discover and control devices with the "switch" capability.
 */
@Singleton
class SmartThingsController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val api: SmartThingsApiService,
) : SmartSwitchController {

    companion object {
        private const val TAG = "SmartThings"
        private const val PREFS_NAME = "smart_switch_prefs"
        private const val KEY_SELECTED_DEVICE = "selected_device_id"
        private const val KEY_PAT_TOKEN = "smartthings_pat"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun bearerToken(): String {
        val pat = prefs.getString(KEY_PAT_TOKEN, "") ?: ""
        return "Bearer $pat"
    }

    /** Save the user's Personal Access Token. */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_PAT_TOKEN, token).apply()
        Log.d(TAG, "PAT saved (${token.take(8)}…)")
    }

    /** Get the saved PAT (for display purposes). */
    fun getToken(): String {
        return prefs.getString(KEY_PAT_TOKEN, "") ?: ""
    }

    override suspend fun discoverDevices(): List<SmartDevice> {
        return try {
            val response = api.listDevices(bearerToken())
            Log.d(TAG, "Discovered ${response.items.size} devices")

            response.items
                .filter { device ->
                    // Only show devices that have the "switch" capability
                    device.components?.any { component ->
                        component.capabilities.any { it.id == "switch" }
                    } == true
                }
                .map { device ->
                    val isOn = try {
                        val status = api.getDeviceStatus(bearerToken(), device.deviceId)
                        val switchValue = status.components
                            ?.get("main")
                            ?.get("switch")
                            ?.get("switch")
                            ?.value
                        switchValue == "on"
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get status for ${device.deviceId}", e)
                        false
                    }

                    SmartDevice(
                        id = device.deviceId,
                        name = device.label ?: device.name ?: "Unknown Device",
                        room = null, // roomId requires a separate API call
                        isOn = isOn,
                        type = DeviceType.SWITCH,
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discover devices", e)
            emptyList()
        }
    }

    override suspend fun selectDevice(deviceId: String) {
        Log.d(TAG, "Selected device: $deviceId")
        prefs.edit().putString(KEY_SELECTED_DEVICE, deviceId).apply()
    }

    override suspend fun getSelectedDevice(): SmartDevice? {
        val id = prefs.getString(KEY_SELECTED_DEVICE, null) ?: return null
        return try {
            val response = api.listDevices(bearerToken())
            val device = response.items.find { it.deviceId == id } ?: return null

            val status = api.getDeviceStatus(bearerToken(), id)
            val isOn = status.components
                ?.get("main")
                ?.get("switch")
                ?.get("switch")
                ?.value == "on"

            SmartDevice(
                id = device.deviceId,
                name = device.label ?: device.name ?: "Unknown Device",
                room = null,
                isOn = isOn,
                type = DeviceType.SWITCH,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get selected device", e)
            null
        }
    }

    override suspend fun turnOn(): Result<Unit> {
        val deviceId = prefs.getString(KEY_SELECTED_DEVICE, null)
            ?: return Result.failure(IllegalStateException("No device selected"))

        return try {
            val response = api.executeCommand(
                auth = bearerToken(),
                deviceId = deviceId,
                body = SmartThingsCommandRequest(
                    commands = listOf(
                        SmartThingsCommand(
                            capability = "switch",
                            command = "on",
                        )
                    )
                ),
            )
            Log.i(TAG, "🔥 Turned ON: $deviceId (status: ${response.results?.firstOrNull()?.status})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn on", e)
            Result.failure(e)
        }
    }

    override suspend fun turnOff(): Result<Unit> {
        val deviceId = prefs.getString(KEY_SELECTED_DEVICE, null)
            ?: return Result.failure(IllegalStateException("No device selected"))

        return try {
            val response = api.executeCommand(
                auth = bearerToken(),
                deviceId = deviceId,
                body = SmartThingsCommandRequest(
                    commands = listOf(
                        SmartThingsCommand(
                            capability = "switch",
                            command = "off",
                        )
                    )
                ),
            )
            Log.i(TAG, "❄️ Turned OFF: $deviceId (status: ${response.results?.firstOrNull()?.status})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off", e)
            Result.failure(e)
        }
    }

    override suspend fun isConnected(): Boolean {
        val hasToken = !getToken().isNullOrBlank()
        val hasDevice = prefs.getString(KEY_SELECTED_DEVICE, null) != null
        return hasToken && hasDevice
    }
}
