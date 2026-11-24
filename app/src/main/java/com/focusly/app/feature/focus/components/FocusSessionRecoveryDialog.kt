package com.focusly.app.feature.focus.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySecondaryButton
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.ActiveTimerSnapshot
import com.focusly.app.domain.usecase.FocusRecoveryCalculator
import com.focusly.app.domain.usecase.TimerCalculator

@Composable
fun FocusSessionRecoveryDialog(
    snapshot: ActiveTimerSnapshot,
    onResumeSession: () -> Unit,
    onRestartShorter: (Int) -> Unit,
    onEndSession: () -> Unit
) {
    val progress = TimerCalculator.calculateProgress(snapshot, System.currentTimeMillis())
    val remaining = TimerCalculator.formatDuration(progress.remainingSeconds)
    val plannedMin = (snapshot.plannedDurationSeconds / 60L).toInt().coerceAtLeast(1)
    val actualMin = (progress.elapsedSeconds / 60L).toInt()
    val suggestedRetryMin = FocusRecoveryCalculator.suggestRetryMinutes(plannedMin, actualMin)

    AlertDialog(
        onDismissRequest = {},
        containerColor = FocuslyColors.SecondaryBackground,
        title = {
            Text(
                text = stringResource(R.string.recovery_title),
                color = FocuslyColors.PrimaryText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.recovery_fresh_start),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
                Text(
                    text = "${stringResource(R.string.timer_recovery_body)}\n\nRemaining: $remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                FocuslyPrimaryButton(
                    text = stringResource(R.string.recovery_resume_btn),
                    onClick = onResumeSession
                )
                FocuslySecondaryButton(
                    text = stringResource(R.string.recovery_shorter_btn, suggestedRetryMin),
                    onClick = { onRestartShorter(suggestedRetryMin) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEndSession) {
                Text(
                    text = stringResource(R.string.timer_recovery_end),
                    color = FocuslyColors.Warning
                )
            }
        },
        dismissButton = {}
    )
}
