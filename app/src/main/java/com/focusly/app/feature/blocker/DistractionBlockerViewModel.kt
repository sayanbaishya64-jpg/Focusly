package com.focusly.app.feature.blocker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.domain.model.DistractionBlockerState
import com.focusly.app.domain.repository.DistractionBlockerRepository
import com.focusly.app.services.blocker.DistractionBlockerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DistractionBlockerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val blockerRepository: DistractionBlockerRepository,
    private val blockerManager: DistractionBlockerManager
) : ViewModel() {

    val uiState: StateFlow<DistractionBlockerState> = blockerRepository
        .observeBlockerState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DistractionBlockerState()
        )

    private val _testStatusSummary = MutableStateFlow<String?>(null)
    val testStatusSummary: StateFlow<String?> = _testStatusSummary.asStateFlow()

    init {
        viewModelScope.launch {
            blockerRepository.syncFromOnboardingIfNeeded()
            blockerRepository.refreshPermissionAndApps()
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            blockerRepository.setEnabled(enabled)
        }
    }

    fun toggleApp(packageName: String) {
        viewModelScope.launch {
            blockerRepository.toggleAppSelection(packageName)
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val latest = blockerRepository.refreshPermissionAndApps()
            _testStatusSummary.value = buildStatusMessage(latest)
        }
    }

    fun openUsageAccessSettings() {
        runCatching {
            appContext.startActivity(blockerManager.buildUsageAccessSettingsIntent())
        }
    }

    fun openAppSettingsForRevoke() {
        runCatching {
            appContext.startActivity(blockerManager.buildAppDetailsSettingsIntent())
        }
    }

    private fun buildStatusMessage(state: DistractionBlockerState): String {
        return when {
            !state.isEnabled -> "Distraction awareness is currently off. Timer works normally."
            state.selectedPackageNames.isEmpty() -> "Enabled, but no distraction apps are selected yet."
            state.permissionStatus == com.focusly.app.domain.model.DistractionPermissionStatus.GRANTED ->
                "Ready: monitoring ${state.selectedPackageNames.size} selected app(s) strictly during active focus sessions."
            else ->
                "Checklist mode active: ${state.selectedPackageNames.size} app(s) tracked without Usage Access permission."
        }
    }
}
