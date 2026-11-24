package com.focusly.app.feature.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.domain.model.RewardPurchaseResult
import com.focusly.app.domain.model.RewardsDashboardState
import com.focusly.app.domain.repository.RewardsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository
) : ViewModel() {

    val uiState: StateFlow<RewardsDashboardState> = rewardsRepository
        .observeRewardsDashboard()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RewardsDashboardState()
        )

    private val _purchaseMessage = MutableStateFlow<RewardPurchaseResult?>(null)
    val purchaseMessage: StateFlow<RewardPurchaseResult?> = _purchaseMessage.asStateFlow()

    fun purchaseItem(itemId: String) {
        viewModelScope.launch {
            val result = rewardsRepository.purchaseStoreItem(
                itemId = itemId,
                localDate = LocalDate.now().toString()
            )
            _purchaseMessage.value = result
        }
    }

    fun setFeaturedItem(itemId: String) {
        viewModelScope.launch {
            rewardsRepository.setFeaturedGardenItem(itemId)
        }
    }

    fun clearPurchaseMessage() {
        _purchaseMessage.value = null
    }
}
