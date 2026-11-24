package com.focusly.app.feature.focus.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySecondaryButton
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType

@Composable
fun TimerControls(
    status: SessionStatus,
    sessionType: SessionType,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRequestEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (status) {
            SessionStatus.IDLE,
            SessionStatus.COMPLETED,
            SessionStatus.ENDED_EARLY -> {
                FocuslyPrimaryButton(
                    text = if (sessionType == SessionType.BREAK) {
                        stringResource(R.string.timer_start_break)
                    } else {
                        stringResource(R.string.timer_start)
                    },
                    onClick = onStart
                )
            }

            SessionStatus.RUNNING -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FocuslyPrimaryButton(
                        text = stringResource(R.string.timer_pause),
                        onClick = onPause,
                        modifier = Modifier.weight(1f)
                    )
                    FocuslySecondaryButton(
                        text = stringResource(R.string.timer_end_session),
                        onClick = onRequestEnd,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SessionStatus.PAUSED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FocuslyPrimaryButton(
                        text = stringResource(R.string.timer_resume),
                        onClick = onResume,
                        modifier = Modifier.weight(1f)
                    )
                    FocuslySecondaryButton(
                        text = stringResource(R.string.timer_end_session),
                        onClick = onRequestEnd,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
