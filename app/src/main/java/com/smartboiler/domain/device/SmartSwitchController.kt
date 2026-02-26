package com.smartboiler.domain.device

/**
 * Abstraction for controlling the boiler's smart switch.
 *
 * Currently backed by a stub implementation. When the Google Home SDK AAR
 * is added to `app/libs/`, swap the DI binding to `GoogleHomeSmartSwitchController`.
 */
interface SmartSwitchController {

    /** Discover available on/off devices in the user's Google Home. */
    suspend fun discoverDevices(): List<SmartDevice>

    /** Persist the selected device ID for future control commands. */
    suspend fun selectDevice(deviceId: String)

    /** Get the currently selected device, or null if none selected. */
    suspend fun getSelectedDevice(): SmartDevice?

    /** Turn the selected device ON. */
    suspend fun turnOn(): Result<Unit>

    /** Turn the selected device OFF. */
    suspend fun turnOff(): Result<Unit>

    /** Check if we have a valid connection to the smart home platform. */
    suspend fun isConnected(): Boolean
}
