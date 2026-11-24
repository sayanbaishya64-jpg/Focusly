package com.focusly.app.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.WeeklyStats
import kotlin.math.max

@Composable
fun WeeklySummaryCard(
    weeklyStats: WeeklyStats,
    modifier: Modifier = Modifier
) {
    FocuslyCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_weekly_summary),
            style = MaterialTheme.typography.titleMedium,
            color = FocuslyColors.PrimaryText
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${weeklyStats.totalWeeklyFocusMinutes} min",
                    style = MaterialTheme.typography.titleLarge,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = stringResource(R.string.home_weekly_minutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${weeklyStats.completedSessions}",
                    style = MaterialTheme.typography.titleLarge,
                    color = FocuslyColors.SecondaryAccent
                )
                Text(
                    text = stringResource(R.string.home_completed_sessions),
                    style = MaterialTheme.typography.labelMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (weeklyStats.totalWeeklyFocusMinutes == 0 && weeklyStats.completedSessions == 0) {
            Text(
                text = stringResource(R.string.home_no_weekly_activity),
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
        } else {
            val maxMinutes = max(1, weeklyStats.dailyBreakdown.maxOfOrNull { it.focusMinutes } ?: 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.dailyBreakdown.forEach { day ->
                    val ratio = (day.focusMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                    val barHeight = (10 + (48 * ratio)).dp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (day.focusMinutes > 0) {
                                        FocuslyColors.PrimaryAccent
                                    } else {
                                        FocuslyColors.SecondaryBackground
                                    }
                                )
                        )
                        Text(
                            text = day.dayShortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = FocuslyColors.SecondaryText
                        )
                    }
                }
            }
        }
    }
}
