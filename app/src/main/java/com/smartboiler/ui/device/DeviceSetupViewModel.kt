package com.smartboiler.ui.device

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.data.device.SmartThingsController
import com.smartboiler.domain.device.SmartDevice
import com.smartboiler.domain.device.SmartSwitchController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceSetupUiState(
    val devices: List<SmartDevice> = emptyList(),
    val selectedDeviceId: String? = null,
    val isDiscovering: Boolean = false,
    val isConnected: Boolean = false,
    val token: String = "",
    val hasToken: Boolean = false,
)

@HiltViewModel
class DeviceSetupViewModel @Inject constructor(
    private val controller: SmartSwitchController,
) : ViewModel() {

    var uiState by mutableStateOf(DeviceSetupUiState())
        private set

    private val smartThingsController get() = controller as? SmartThingsController

    init {
        viewModelScope.launch {
            val selected = controller.getSelectedDevice()
            val token = smartThingsController?.getToken() ?: ""
            uiState = uiState.copy(
                selectedDeviceId = selected?.id,
                isConnected = controller.isConnected(),
                token = token,
                hasToken = token.isNotBlank(),
            )
        }
    }

    fun updateToken(token: String) {
        uiState = uiState.copy(token = token)
    }

    fun saveToken() {
        val token = uiState.token.trim()
        if (token.isBlank()) return
        smartThingsController?.saveToken(token)
        uiState = uiState.copy(hasToken = true)
    }

    fun discoverDevices() {
        viewModelScope.launch {
            uiState = uiState.copy(isDiscovering = true)
            val devices = controller.discoverDevices()
            uiState = uiState.copy(devices = devices, isDiscovering = false)
        }
    }

    fun selectDevice(deviceId: String) {
        viewModelScope.launch {
            controller.selectDevice(deviceId)
            uiState = uiState.copy(
                selectedDeviceId = deviceId,
                isConnected = true,
            )
        }
    }

    fun testToggle() {
        viewModelScope.launch {
            val device = controller.getSelectedDevice() ?: return@launch
            if (device.isOn) controller.turnOff() else controller.turnOn()
            // Refresh device list to see updated state
            val devices = controller.discoverDevices()
            uiState = uiState.copy(devices = devices)
        }
    }
}
