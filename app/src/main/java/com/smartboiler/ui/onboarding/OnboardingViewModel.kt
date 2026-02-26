package com.smartboiler.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.DayType
import com.smartboiler.domain.model.HeatingBaseline
import com.smartboiler.domain.usecase.CheckOnboardingCompleteUseCase
import com.smartboiler.domain.usecase.CompleteOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    // Step 2: Boiler Setup
    val capacityLiters: Int = 150,
    val heatingPowerKw: Double = 3.0,
    val desiredTempCelsius: Int = 40,

    // Step 3: Location
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cityName: String = "",
    val locationDetected: Boolean = false,
    val locationLoading: Boolean = false,
    val locationError: String? = null,

    // Step 4: Baselines
    val sunnyMinutes: Int = 0,
    val partlyCloudyMinutes: Int = 30,
    val cloudyMinutes: Int = 90,

    // Overall
    val isSaving: Boolean = false,
    val onboardingComplete: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val checkOnboardingCompleteUseCase: CheckOnboardingCompleteUseCase,
) : ViewModel() {

    var uiState by mutableStateOf(OnboardingUiState())
        private set

    /** Check if user has already completed onboarding (for start destination). */
    suspend fun checkOnboardingStatus(): Boolean = checkOnboardingCompleteUseCase()

    // --- Boiler Setup ---

    fun updateCapacity(liters: Int) {
        uiState = uiState.copy(capacityLiters = liters.coerceIn(30, 500))
    }

    fun updateHeatingPower(kw: Double) {
        uiState = uiState.copy(heatingPowerKw = kw.coerceIn(1.0, 10.0))
    }

    fun updateDesiredTemp(temp: Int) {
        uiState = uiState.copy(desiredTempCelsius = temp.coerceIn(30, 60))
    }

    // --- Location ---

    fun updateLocation(lat: Double, lng: Double, city: String) {
        uiState = uiState.copy(
            latitude = lat,
            longitude = lng,
            cityName = city,
            locationDetected = true,
            locationLoading = false,
            locationError = null,
        )
    }

    fun setLocationLoading(loading: Boolean) {
        uiState = uiState.copy(locationLoading = loading)
    }

    fun setLocationError(error: String?) {
        uiState = uiState.copy(locationError = error, locationLoading = false)
    }

    fun updateCityManually(city: String) {
        uiState = uiState.copy(cityName = city)
    }

    // --- Baselines ---

    fun updateSunnyMinutes(minutes: Int) {
        uiState = uiState.copy(sunnyMinutes = minutes.coerceIn(0, 240))
    }

    fun updatePartlyCloudyMinutes(minutes: Int) {
        uiState = uiState.copy(partlyCloudyMinutes = minutes.coerceIn(0, 240))
    }

    fun updateCloudyMinutes(minutes: Int) {
        uiState = uiState.copy(cloudyMinutes = minutes.coerceIn(0, 240))
    }

    // --- Save ---

    fun completeOnboarding() {
        val state = uiState
        uiState = uiState.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val config = BoilerConfig(
                    capacityLiters = state.capacityLiters,
                    heatingPowerKw = state.heatingPowerKw,
                    desiredTempCelsius = state.desiredTempCelsius,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    cityName = state.cityName,
                )

                val baselines = listOf(
                    HeatingBaseline(dayType = DayType.SUNNY, durationMinutes = state.sunnyMinutes),
                    HeatingBaseline(dayType = DayType.PARTLY_CLOUDY, durationMinutes = state.partlyCloudyMinutes),
                    HeatingBaseline(dayType = DayType.CLOUDY, durationMinutes = state.cloudyMinutes),
                )

                completeOnboardingUseCase(config, baselines)
                uiState = uiState.copy(isSaving = false, onboardingComplete = true)
            } catch (e: Exception) {
                uiState = uiState.copy(isSaving = false)
            }
        }
    }

    // --- Validation helpers ---

    val isBoilerSetupValid: Boolean
        get() = uiState.capacityLiters > 0 && uiState.heatingPowerKw > 0

    val isLocationValid: Boolean
        get() = uiState.cityName.isNotBlank()
}
