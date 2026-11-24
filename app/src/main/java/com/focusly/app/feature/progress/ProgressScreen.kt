package com.focusly.app.feature.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.AnalyticsPeriod
import com.focusly.app.feature.recommendation.FocusRecommendationCard
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onStartRecommendedFocus: (Int) -> Unit,
    onIgnoreRecommendation: (String) -> Unit
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FocuslyColors.PrimaryBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = FocuslyColors.PrimaryAccent)
        }
        return
    }

    val summary = state.summary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocuslyColors.PrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + Period Segmented Toggle
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    text = stringResource(R.string.progress_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = stringResource(R.string.progress_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodTabChip(
                    label = stringResource(R.string.progress_period_week),
                    selected = state.selectedPeriod == AnalyticsPeriod.WEEK,
                    onClick = { onSelectPeriod(AnalyticsPeriod.WEEK) }
                )
                PeriodTabChip(
                    label = stringResource(R.string.progress_period_month),
                    selected = state.selectedPeriod == AnalyticsPeriod.MONTH,
                    onClick = { onSelectPeriod(AnalyticsPeriod.MONTH) }
                )
            }
        }

        // KPI Metrics Grid (2 rows x 2 columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTileCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.progress_total_focus),
                value = "${summary.totalFocusMinutes} min",
                accent = FocuslyColors.PrimaryAccent
            )
            MetricTileCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.progress_completion_rate),
                value = "${summary.completionRatePercent.roundToInt()}%",
                accent = FocuslyColors.Success
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTileCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.progress_avg_session),
                value = "${summary.averageSessionMinutes} min",
                accent = FocuslyColors.SecondaryAccent
            )
            MetricTileCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.progress_tasks_done),
                value = "${state.completedTasksCount}/${state.totalTasksCount}",
                accent = FocuslyColors.PrimaryText
            )
        }

        // Daily / Monthly Focus Activity Chart
        FocuslyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_activity_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = stringResource(
                        R.string.progress_active_days,
                        summary.activeDaysCount,
                        summary.period.days
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = FocuslyColors.SecondaryAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (summary.totalFocusMinutes == 0 && summary.completedSessions == 0) {
                Text(
                    text = stringResource(R.string.progress_no_activity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            } else {
                val maxDayMins = max(1, summary.dailyBreakdown.maxOfOrNull { it.focusMinutes } ?: 1)
                val chartBars = if (summary.period == AnalyticsPeriod.WEEK) {
                    summary.dailyBreakdown
                } else {
                    summary.dailyBreakdown.takeLast(14)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartBars.forEach { day ->
                        val ratio = day.focusMinutes.toFloat() / maxDayMins.toFloat()
                        val barHeight = (10f + ratio * 66f).dp
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = day.dayShortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = FocuslyColors.SecondaryText,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Subject-Wise Focus Breakdown
        FocuslyCard {
            Text(
                text = stringResource(R.string.progress_subject_breakdown),
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (summary.subjectBreakdown.isEmpty()) {
                Text(
                    text = stringResource(R.string.progress_no_subject_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary.subjectBreakdown.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(item.colorHex))
                                    )
                                    Text(
                                        text = item.subjectName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FocuslyColors.PrimaryText
                                    )
                                }
                                Text(
                                    text = "${item.focusMinutes} min · ${item.sharePercentage.roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FocuslyColors.SecondaryText
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (item.sharePercentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(item.colorHex),
                                trackColor = FocuslyColors.SecondaryBackground
                            )
                        }
                    }
                }
            }
        }

        // Adaptive Focus Engine Insights (FAFE)
        FocusRecommendationCard(
            recommendation = state.recommendation,
            onApplyRecommended = onStartRecommendedFocus,
            onIgnore = onIgnoreRecommendation,
            isPrimaryStartLabel = true
        )
    }
}

@Composable
private fun PeriodTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) FocuslyColors.PrimaryAccent else FocuslyColors.SecondaryBackground,
        border = BorderStroke(
            1.dp,
            if (selected) FocuslyColors.PrimaryAccent else FocuslyColors.CardBorder
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) FocuslyColors.PrimaryText else FocuslyColors.SecondaryText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun MetricTileCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = FocuslyColors.CardBackground,
        border = BorderStroke(1.dp, FocuslyColors.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = FocuslyColors.SecondaryText
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent
            )
        }
    }
}
