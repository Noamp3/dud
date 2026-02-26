package com.smartboiler.domain.usecase

import com.smartboiler.domain.repository.BoilerRepository
import javax.inject.Inject

/**
 * Checks whether the user has completed onboarding.
 * Used to determine the start destination in navigation.
 */
class CheckOnboardingCompleteUseCase @Inject constructor(
    private val repository: BoilerRepository,
) {
    suspend operator fun invoke(): Boolean = repository.isOnboardingComplete()
}
