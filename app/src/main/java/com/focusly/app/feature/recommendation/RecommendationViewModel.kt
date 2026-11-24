package com.focusly.app.feature.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.domain.model.FocusRecommendation
import com.focusly.app.domain.model.RecommendationConfidence
import com.focusly.app.domain.model.RecommendationReasonType
import com.focusly.app.domain.model.RecommendationStatus
import com.focusly.app.domain.repository.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository
) : ViewModel() {

    private val subjectFilterFlow = MutableStateFlow<String?>(null)

    val recommendationState: StateFlow<FocusRecommendation> = subjectFilterFlow
        .flatMapLatest { subjectId ->
            recommendationRepository.observeLatestRecommendation(subjectId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FocusRecommendation(
                id = "rec-init",
                recommendedMinutes = 25,
                reasonType = RecommendationReasonType.INSUFFICIENT_DATA,
                reasonText = "",
                confidence = RecommendationConfidence.INSUFFICIENT_DATA,
                hasEnoughData = false,
                basedOnSessionCount = 0,
                status = RecommendationStatus.ACTIVE
            )
        )

    fun setSubjectContext(subjectId: String?) {
        subjectFilterFlow.value = subjectId
    }

    fun acceptRecommendation(recommendationId: String) {
        viewModelScope.launch {
            recommendationRepository.markRecommendationAccepted(recommendationId)
        }
    }

    fun ignoreRecommendation(recommendationId: String) {
        viewModelScope.launch {
            recommendationRepository.markRecommendationIgnored(recommendationId)
        }
    }
}
