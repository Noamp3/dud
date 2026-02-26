package com.smartboiler.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.HeatingBaseline
import com.smartboiler.domain.repository.BoilerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val config: BoilerConfig? = null,
    val baselines: List<HeatingBaseline> = emptyList(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: BoilerRepository,
) : ViewModel() {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        viewModelScope.launch {
            val config = repository.getBoilerConfig()
            val baselines = repository.getBaselines()
            uiState = SettingsUiState(config = config, baselines = baselines, isLoading = false)
        }
    }

    fun updateBoilerSize(liters: Int) {
        uiState = uiState.copy(
            config = uiState.config?.copy(capacityLiters = liters),
            isSaved = false,
        )
    }

    fun updateHeatingPower(kw: Double) {
        uiState = uiState.copy(
            config = uiState.config?.copy(heatingPowerKw = kw),
            isSaved = false,
        )
    }

    fun updateTargetTemp(temp: Int) {
        uiState = uiState.copy(
            config = uiState.config?.copy(desiredTempCelsius = temp),
            isSaved = false,
        )
    }

    fun save() {
        val config = uiState.config ?: return
        viewModelScope.launch {
            repository.saveBoilerConfig(config)
            uiState = uiState.copy(isSaved = true)
        }
    }
}
