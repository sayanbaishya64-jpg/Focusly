package com.focusly.app.feature.recovery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.theme.FocuslyColors
import kotlinx.coroutines.delay

@Composable
fun BreathingResetDialog(
    onDismiss: () -> Unit
) {
    val totalSeconds = 60
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val isComplete = elapsedSeconds >= totalSeconds

    LaunchedEffect(Unit) {
        while (elapsedSeconds < totalSeconds) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    val cycleSecond = elapsedSeconds % 16
    val phaseStep = cycleSecond / 4
    val phaseRemaining = 4 - (cycleSecond % 4)
    val remainingTotal = (totalSeconds - elapsedSeconds).coerceAtLeast(0)

    val phaseLabel = when {
        isComplete -> stringResource(R.string.breathing_complete)
        phaseStep == 0 -> stringResource(R.string.breathing_inhale)
        phaseStep == 1 -> stringResource(R.string.breathing_hold)
        phaseStep == 2 -> stringResource(R.string.breathing_exhale)
        else -> stringResource(R.string.breathing_rest)
    }

    val targetScale = when {
        isComplete -> 1.0f
        phaseStep == 0 -> 1.24f
        phaseStep == 1 -> 1.24f
        phaseStep == 2 -> 0.88f
        else -> 0.88f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 900),
        label = "breathing_scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocuslyColors.SecondaryBackground,
        title = {
            Text(
                text = stringResource(R.string.breathing_title),
                color = FocuslyColors.PrimaryText,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.breathing_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(FocuslyColors.CardBackground)
                        .border(2.dp, FocuslyColors.SecondaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = phaseLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FocuslyColors.SecondaryAccent
                        )
                        if (!isComplete) {
                            Text(
                                text = "${phaseRemaining}s",
                                style = MaterialTheme.typography.headlineSmall,
                                color = FocuslyColors.PrimaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isComplete) {
                        stringResource(R.string.breathing_done_hint)
                    } else {
                        stringResource(R.string.breathing_remaining_seconds, remainingTotal)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = FocuslyColors.SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.study_close),
                    color = FocuslyColors.PrimaryAccent
                )
            }
        }
    )
}
