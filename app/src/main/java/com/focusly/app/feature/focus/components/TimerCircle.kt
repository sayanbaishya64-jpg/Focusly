package com.focusly.app.feature.focus.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.usecase.TimerCalculator

@Composable
fun TimerCircle(
    remainingSeconds: Long,
    progressFraction: Float,
    sessionType: SessionType,
    status: SessionStatus,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (1f - progressFraction).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "timerCircleProgress"
    )

    val ringColor = when {
        sessionType == SessionType.BREAK -> FocuslyColors.Success
        status == SessionStatus.PAUSED -> FocuslyColors.Warning
        else -> FocuslyColors.PrimaryAccent
    }

    Box(
        modifier = modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            drawArc(
                color = FocuslyColors.CardBackground,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (sessionType == SessionType.BREAK) "BREAK" else "FOCUS",
                style = MaterialTheme.typography.labelMedium,
                color = FocuslyColors.SecondaryAccent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = TimerCalculator.formatDuration(remainingSeconds),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (status) {
                    SessionStatus.PAUSED -> "Paused"
                    SessionStatus.RUNNING -> "Active"
                    else -> "Ready"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
        }
    }
}
