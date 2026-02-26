package com.smartboiler.domain.device

/**
 * Represents a smart home device discovered via Google Home.
 */
data class SmartDevice(
    val id: String,
    val name: String,
    val room: String?,
    val isOn: Boolean = false,
    val type: DeviceType = DeviceType.PLUG,
)

enum class DeviceType { SWITCH, PLUG }
