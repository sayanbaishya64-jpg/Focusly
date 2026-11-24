package com.focusly.app.feature.focus.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySecondaryButton
import com.focusly.app.core.designsystem.components.FocuslyTextAction
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.DailyStats
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.usecase.RewardEngine
import com.focusly.app.domain.usecase.TimerCalculator
import kotlin.math.roundToInt

@Composable
fun SessionCompletionCard(
    completedSession: FocusSession,
    subjectName: String?,
    taskTitle: String?,
    dailyStats: DailyStats,
    onStartAnother: () -> Unit,
    onTakeBreak: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBreak = completedSession.type == SessionType.BREAK

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = FocuslyColors.Success,
            modifier = Modifier.size(64.dp)
        )

        Text(
            text = if (isBreak) {
                stringResource(R.string.timer_break_completed_title)
            } else {
                stringResource(R.string.timer_completed_title)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = FocuslyColors.PrimaryText,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.timer_completed_message),
            style = MaterialTheme.typography.bodyLarge,
            color = FocuslyColors.SecondaryText,
            textAlign = TextAlign.Center
        )

        FocuslyCard {
            SummaryRow("Actual Duration", TimerCalculator.formatDuration(completedSession.actualDurationSeconds))
            Spacer(modifier = Modifier.height(10.dp))
            SummaryRow("Planned Duration", TimerCalculator.formatDuration(completedSession.plannedDurationSeconds))
            if (!subjectName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                SummaryRow("Subject", subjectName)
            }
            if (!taskTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                SummaryRow("Task", taskTitle)
            }
            Spacer(modifier = Modifier.height(10.dp))
            SummaryRow(
                "Today's Progress",
                "${dailyStats.completedFocusMinutes} / ${dailyStats.targetMinutes} min (${dailyStats.completionPercentage.roundToInt()}%)"
            )
            val earnedCoins = RewardEngine.calculateSessionCoins(
                plannedDurationSeconds = completedSession.plannedDurationSeconds,
                actualDurationSeconds = completedSession.actualDurationSeconds,
                sessionType = completedSession.type,
                sessionStatus = completedSession.status
            )
            if (earnedCoins > 0 && completedSession.status == SessionStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(10.dp))
                SummaryRow(
                    stringResource(R.string.rewards_session_coins_earned),
                    "+$earnedCoins"
                )
            }
        }

        FocuslyPrimaryButton(
            text = stringResource(R.string.timer_start_another),
            onClick = onStartAnother
        )

        if (!isBreak) {
            FocuslySecondaryButton(
                text = stringResource(R.string.timer_take_break),
                onClick = onTakeBreak
            )
        }

        FocuslyTextAction(
            text = stringResource(R.string.timer_return_home),
            onClick = onReturnHome
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = FocuslyColors.SecondaryText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = FocuslyColors.PrimaryText
        )
    }
}
