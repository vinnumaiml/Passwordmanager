package com.novavault.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novavault.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun markOnboardingComplete() {
        viewModelScope.launch {
            val current = settingsRepository.getSettings()
            settingsRepository.updateSettings(current.copy(hasCompletedOnboarding = true))
        }
    }
}
