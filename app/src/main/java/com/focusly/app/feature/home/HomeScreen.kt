package com.focusly.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.DistractionBlockerMode
import com.focusly.app.feature.home.components.CurrentTaskCard
import com.focusly.app.feature.home.components.FocusProgressCard
import com.focusly.app.feature.home.components.StartFocusButton
import com.focusly.app.feature.home.components.WeeklySummaryCard
import com.focusly.app.feature.recommendation.FocusRecommendationCard
import com.focusly.app.feature.recovery.BreathingResetDialog
import com.focusly.app.feature.recovery.FocusRecoveryCard
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartFocusClick: () -> Unit,
    onStartRecommendedFocus: (Int) -> Unit,
    onIgnoreRecommendation: (String) -> Unit,
    onRestartShorterFromRecovery: (Int) -> Unit,
    onTakeRecoveryBreak: () -> Unit,
    onDismissRecovery: () -> Unit,
    onOpenBlockerSettings: () -> Unit,
    onOpenRewards: () -> Unit,
    onClaimMission: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onCreateQuickTask: (title: String, subjectName: String, durationMinutes: Int) -> Unit
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showNotificationsInfoDialog by remember { mutableStateOf(false) }
    var showBreathingResetDialog by remember { mutableStateOf(false) }

    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> stringResource(R.string.greeting_morning)
        hour < 17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }
    val greetingLine = if (state.studentName.isNotBlank()) {
        "$greeting, ${state.studentName}"
    } else {
        greeting
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocuslyColors.PrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Logo, Greeting + Student Name, Notification & Settings Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocuslyColors.SecondaryBackground)
                        .border(1.dp, FocuslyColors.CardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = stringResource(R.string.app_name),
                        tint = FocuslyColors.SecondaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.SecondaryAccent
                    )
                    Text(
                        text = greetingLine,
                        style = MaterialTheme.typography.titleLarge,
                        color = FocuslyColors.PrimaryText
                    )
                }
            }

            Row {
                IconButton(onClick = { showNotificationsInfoDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        tint = FocuslyColors.SecondaryText
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = FocuslyColors.SecondaryText
                    )
                }
            }
        }

        FocusProgressCard(dailyStats = state.dailyStats)

        StartFocusButton(
            text = if (state.activeTimer?.isActive == true) {
                stringResource(R.string.timer_recovery_resume)
            } else {
                stringResource(R.string.home_start_focus)
            },
            onClick = onStartFocusClick
        )

        // Supportive Focus Recovery card when an early-ended/interrupted session is active
        val recovery = state.recoveryState
        if (recovery != null && recovery.isActive && state.activeTimer?.isActive != true) {
            FocusRecoveryCard(
                recoveryState = recovery,
                onRestartShorter = onRestartShorterFromRecovery,
                onTakeBreak = onTakeRecoveryBreak,
                onDismiss = onDismissRecovery,
                onCalmReset = { showBreathingResetDialog = true }
            )
        }

        // Adaptive Focus Engine (FAFE) Recommendation Card
        FocusRecommendationCard(
            recommendation = state.recommendation,
            onApplyRecommended = onStartRecommendedFocus,
            onIgnore = onIgnoreRecommendation,
            isPrimaryStartLabel = true
        )

        // Distraction Blocker Status Card
        val blockerStatusLabel = when (state.blockerState.effectiveMode) {
            DistractionBlockerMode.ACTIVE_DETECTION -> stringResource(R.string.blocker_status_active)
            DistractionBlockerMode.CHECKLIST_ONLY -> stringResource(R.string.blocker_status_checklist)
            DistractionBlockerMode.DISABLED -> stringResource(R.string.blocker_status_disabled)
        }
        FocuslyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = if (state.blockerState.isEnabled) FocuslyColors.Success else FocuslyColors.SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.blocker_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = FocuslyColors.PrimaryText
                        )
                        Text(
                            text = "$blockerStatusLabel · ${state.blockerState.selectedPackageNames.size} apps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FocuslyColors.SecondaryText
                        )
                    }
                }
                TextButton(onClick = onOpenBlockerSettings) {
                    Text(
                        text = "Configure",
                        style = MaterialTheme.typography.labelLarge,
                        color = FocuslyColors.SecondaryAccent
                    )
                }
            }
        }

        if (state.dailyStats.completedSessions == 0) {
            FocuslyCard {
                Text(
                    text = stringResource(R.string.home_no_sessions_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
        }

        CurrentTaskCard(
            task = state.currentTask,
            onAddTaskClick = { showAddTaskDialog = true }
        )

        // Daily Study Missions Card
        if (state.dailyMissions.isNotEmpty()) {
            val completedCount = state.dailyMissions.count { it.isCompleted }
            FocuslyCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = FocuslyColors.SecondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.missions_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = FocuslyColors.PrimaryText
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.missions_completed_summary,
                            completedCount,
                            state.dailyMissions.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.SecondaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.dailyMissions.forEach { mission ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FocuslyColors.SecondaryBackground)
                                .border(1.dp, FocuslyColors.CardBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = mission.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FocuslyColors.PrimaryText
                                )
                                Text(
                                    text = mission.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FocuslyColors.SecondaryText
                                )
                            }

                            when {
                                mission.isClaimed -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = FocuslyColors.Success,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.missions_claimed_badge),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = FocuslyColors.Success
                                        )
                                    }
                                }
                                mission.isCompleted -> {
                                    TextButton(onClick = { onClaimMission(mission.id) }) {
                                        Text(
                                            text = stringResource(
                                                R.string.missions_claim_btn,
                                                mission.rewardCoins
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = FocuslyColors.Warning
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "+${mission.rewardCoins}c",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FocuslyColors.SecondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reward Preview Card (Phase 5 integration interface, no fabricated coins)
        FocuslyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_rewards_preview),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.PrimaryText
                )
                TextButton(onClick = onOpenRewards) {
                    Text(
                        text = stringResource(R.string.home_view_rewards),
                        style = MaterialTheme.typography.labelLarge,
                        color = FocuslyColors.SecondaryAccent
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${state.rewardPreview.focusCoins}",
                        style = MaterialTheme.typography.titleLarge,
                        color = FocuslyColors.PrimaryText
                    )
                    Text(
                        text = stringResource(R.string.home_focus_coins),
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.SecondaryText
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${state.rewardPreview.unlockedAchievements}",
                        style = MaterialTheme.typography.titleLarge,
                        color = FocuslyColors.PrimaryText
                    )
                    Text(
                        text = stringResource(R.string.home_achievements),
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.SecondaryText
                    )
                }
            }
        }

        WeeklySummaryCard(weeklyStats = state.weeklyStats)
    }

    if (showAddTaskDialog) {
        QuickAddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, subject, duration ->
                onCreateQuickTask(title, subject, duration)
                showAddTaskDialog = false
            }
        )
    }

    if (showNotificationsInfoDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsInfoDialog = false },
            containerColor = FocuslyColors.SecondaryBackground,
            title = { Text("Focus Notifications", color = FocuslyColors.PrimaryText) },
            text = {
                Text(
                    "Active focus and break sessions display a live countdown notification so you can pause or finish even when the screen is locked.",
                    color = FocuslyColors.SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsInfoDialog = false }) {
                    Text("OK", color = FocuslyColors.PrimaryAccent)
                }
            }
        )
    }

    if (showBreathingResetDialog) {
        BreathingResetDialog(
            onDismiss = { showBreathingResetDialog = false }
        )
    }
}

@Composable
private fun QuickAddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, subjectName: String, durationMinutes: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocuslyColors.SecondaryBackground,
        title = { Text(stringResource(R.string.home_add_task), color = FocuslyColors.PrimaryText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (optional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { minutesInput = it.filter(Char::isDigit).take(3) },
                    label = { Text("Planned minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val mins = minutesInput.toIntOrNull()?.coerceIn(5, 480) ?: 25
                    onSave(title, subject, mins)
                }
            ) {
                Text("Save", color = FocuslyColors.PrimaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FocuslyColors.SecondaryText)
            }
        }
    )
}
