package com.smartboiler.domain.usecase

import com.smartboiler.domain.model.BoilerConfig
import com.smartboiler.domain.model.HeatingBaseline
import com.smartboiler.domain.repository.BoilerRepository
import javax.inject.Inject

/**
 * Saves the complete onboarding data: boiler config + heating baselines.
 * Marks onboarding as complete after saving.
 */
class CompleteOnboardingUseCase @Inject constructor(
    private val repository: BoilerRepository,
) {
    suspend operator fun invoke(config: BoilerConfig, baselines: List<HeatingBaseline>) {
        repository.saveBoilerConfig(config.copy(onboardingComplete = true))
        repository.saveBaselines(baselines)
    }
}
