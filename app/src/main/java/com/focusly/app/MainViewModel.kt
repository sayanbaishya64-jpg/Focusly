package com.focusly.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.core.model.OnboardingPreferences
import com.focusly.app.domain.repository.OnboardingRepository
import com.focusly.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    onboardingRepository: OnboardingRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    val preferences: StateFlow<OnboardingPreferences> = onboardingRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OnboardingPreferences()
        )

    val studentName: StateFlow<String> = userProfileRepository.observeStudentName()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    fun saveStudentName(name: String) {
        viewModelScope.launch {
            userProfileRepository.updateStudentName(name)
        }
    }
}
