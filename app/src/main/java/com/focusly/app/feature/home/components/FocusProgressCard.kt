package com.focusly.app.feature.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.DailyStats
import kotlin.math.roundToInt

@Composable
fun FocusProgressCard(
    dailyStats: DailyStats,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (dailyStats.completionPercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "dailyProgressRing"
    )

    FocuslyCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier.size(116.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    drawArc(
                        color = FocuslyColors.SecondaryBackground,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = if (dailyStats.completionPercentage >= 100f) {
                            FocuslyColors.Success
                        } else {
                            FocuslyColors.PrimaryAccent
                        },
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${dailyStats.completionPercentage.roundToInt()}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = FocuslyColors.PrimaryText
                    )
                    Text(
                        text = stringResource(R.string.home_progress_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.SecondaryText
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.daily_target_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = "${dailyStats.targetMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryAccent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stringResource(R.string.home_completed_label)}: ${dailyStats.completedFocusMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = "${stringResource(R.string.home_remaining_label)}: ${dailyStats.remainingMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
        }
    }
}
