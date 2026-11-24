package com.focusly.app.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.components.FocuslySecondaryButton
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.DistractionBlockerMode
import com.focusly.app.domain.model.FocusMode
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.feature.focus.components.SessionCompletionCard
import com.focusly.app.feature.focus.components.TimerCircle
import com.focusly.app.feature.focus.components.TimerControls
import com.focusly.app.feature.recommendation.FocusRecommendationCard
import com.focusly.app.feature.recovery.BreathingResetDialog
import com.focusly.app.feature.recovery.FocusRecoveryCard

@Composable
fun FocusScreen(
    state: FocusUiState,
    onSelectSessionType: (SessionType) -> Unit,
    onSelectFocusMode: (FocusMode) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onCustomDurationChange: (String) -> Unit,
    onSelectSubject: (String?) -> Unit,
    onSelectTask: (String?) -> Unit,
    onApplyRecommendedDuration: (Int) -> Unit,
    onIgnoreRecommendation: (String) -> Unit,
    onRestartShorterFromRecovery: (Int) -> Unit,
    onDismissRecovery: () -> Unit,
    onOpenBlockerSettings: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEndEarly: () -> Unit,
    onStartAnother: () -> Unit,
    onTakeBreak: () -> Unit,
    onReturnHome: () -> Unit
) {
    var showEndConfirmDialog by remember { mutableStateOf(false) }
    var showBreathingResetDialog by remember { mutableStateOf(false) }

    val completedSession = state.lastCompletedSession
    if (completedSession != null && state.activeSnapshot == null) {
        val subjectName = state.subjects.firstOrNull { it.id == completedSession.subjectId }?.name
        val taskTitle = state.tasks.firstOrNull { it.id == completedSession.taskId }?.title

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FocuslyColors.PrimaryBackground)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            SessionCompletionCard(
                completedSession = completedSession,
                subjectName = subjectName,
                taskTitle = taskTitle,
                dailyStats = state.dailyStats,
                onStartAnother = onStartAnother,
                onTakeBreak = onTakeBreak,
                onReturnHome = onReturnHome
            )
        }
        return
    }

    val isSessionRunningOrPaused = state.activeSnapshot?.isActive == true
    val presets = if (state.sessionType == SessionType.FOCUS) {
        listOf(15, 25, 45, 60, 90)
    } else {
        listOf(5, 10, 15)
    }

    val selectedSubjectName = state.subjects.firstOrNull { it.id == state.selectedSubjectId }?.name
    val selectedTaskTitle = state.tasks.firstOrNull { it.id == state.selectedTaskId }?.title

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocuslyColors.PrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Focus vs Break Switcher (disabled while active)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                SessionType.FOCUS to stringResource(R.string.timer_mode_focus),
                SessionType.BREAK to stringResource(R.string.timer_mode_break)
            ).forEach { (type, label) ->
                val selected = state.sessionType == type
                FilterChip(
                    selected = selected,
                    enabled = !isSessionRunningOrPaused,
                    onClick = { onSelectSessionType(type) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FocuslyColors.PrimaryAccent,
                        selectedLabelColor = FocuslyColors.PrimaryText,
                        containerColor = FocuslyColors.CardBackground,
                        labelColor = FocuslyColors.SecondaryText
                    )
                )
            }
        }

        // Focus Mode selector (Manual / Auto-Schedule / Smart) when idle in Focus mode
        if (!isSessionRunningOrPaused && state.sessionType == SessionType.FOCUS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    FocusMode.MANUAL to stringResource(R.string.focus_mode_manual),
                    FocusMode.AUTO_SCHEDULE to stringResource(R.string.focus_mode_auto),
                    FocusMode.SMART to stringResource(R.string.focus_mode_smart)
                ).forEach { (mode, label) ->
                    val selected = state.focusMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFocusMode(mode) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FocuslyColors.SecondaryBackground,
                            selectedLabelColor = FocuslyColors.SecondaryAccent,
                            containerColor = FocuslyColors.CardBackground,
                            labelColor = FocuslyColors.SecondaryText
                        )
                    )
                }
            }

            if (state.focusMode == FocusMode.AUTO_SCHEDULE) {
                val pickedTask = state.tasks.firstOrNull { it.id == state.selectedTaskId }
                Text(
                    text = if (pickedTask != null) {
                        stringResource(
                            R.string.focus_mode_auto_hint,
                            pickedTask.title,
                            pickedTask.plannedDurationMinutes
                        )
                    } else {
                        stringResource(R.string.focus_mode_auto_empty)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuslyColors.SecondaryAccent
                )
            } else if (state.focusMode == FocusMode.SMART) {
                Text(
                    text = stringResource(
                        R.string.focus_mode_smart_hint,
                        state.selectedDurationMinutes
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuslyColors.SecondaryAccent
                )
            }
        }

        // Supportive Recovery Prompt (when IDLE after an early-ended focus session)
        val recovery = state.recoveryState
        if (!isSessionRunningOrPaused && recovery != null && recovery.isActive) {
            FocusRecoveryCard(
                recoveryState = recovery,
                onRestartShorter = onRestartShorterFromRecovery,
                onTakeBreak = onTakeBreak,
                onDismiss = onDismissRecovery,
                onCalmReset = { showBreathingResetDialog = true }
            )
        }

        // Adaptive Focus Engine Recommendation Card (IDLE & FOCUS mode)
        if (!isSessionRunningOrPaused && state.sessionType == SessionType.FOCUS) {
            FocusRecommendationCard(
                recommendation = state.recommendation,
                onApplyRecommended = onApplyRecommendedDuration,
                onIgnore = onIgnoreRecommendation,
                isPrimaryStartLabel = false
            )
        }

        // Active Subject & Task Label
        if (state.sessionType == SessionType.FOCUS && (!selectedSubjectName.isNullOrBlank() || !selectedTaskTitle.isNullOrBlank())) {
            Text(
                text = listOfNotNull(selectedSubjectName, selectedTaskTitle).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.SecondaryAccent
            )
        }

        TimerCircle(
            remainingSeconds = state.remainingSeconds,
            progressFraction = state.progressFraction,
            sessionType = state.sessionType,
            status = state.status
        )

        // Preset & Custom Selectors (only when IDLE)
        if (!isSessionRunningOrPaused) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { minutes ->
                    val selected = !state.isCustomDuration && state.selectedDurationMinutes == minutes
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(minutes) },
                        label = { Text("${minutes}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FocuslyColors.PrimaryAccent,
                            selectedLabelColor = FocuslyColors.PrimaryText,
                            containerColor = FocuslyColors.CardBackground,
                            labelColor = FocuslyColors.SecondaryText
                        )
                    )
                }
                FilterChip(
                    selected = state.isCustomDuration,
                    onClick = { onCustomDurationChange(state.customDurationInput.ifEmpty { "35" }) },
                    label = { Text("Custom") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FocuslyColors.PrimaryAccent,
                        selectedLabelColor = FocuslyColors.PrimaryText,
                        containerColor = FocuslyColors.CardBackground,
                        labelColor = FocuslyColors.SecondaryText
                    )
                )
            }

            if (state.isCustomDuration) {
                OutlinedTextField(
                    value = state.customDurationInput,
                    onValueChange = onCustomDurationChange,
                    label = { Text("Custom minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.sessionType == SessionType.FOCUS && (state.subjects.isNotEmpty() || state.tasks.isNotEmpty())) {
                FocuslyCard {
                    if (state.subjects.isNotEmpty()) {
                        Text(
                            text = "Subject",
                            style = MaterialTheme.typography.labelMedium,
                            color = FocuslyColors.SecondaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.subjects.forEach { subject ->
                                FilterChip(
                                    selected = state.selectedSubjectId == subject.id,
                                    onClick = {
                                        onSelectSubject(
                                            if (state.selectedSubjectId == subject.id) null else subject.id
                                        )
                                    },
                                    label = { Text(subject.name) }
                                )
                            }
                        }
                    }
                    if (state.tasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Study Task",
                            style = MaterialTheme.typography.labelMedium,
                            color = FocuslyColors.SecondaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.tasks.forEach { task ->
                                FilterChip(
                                    selected = state.selectedTaskId == task.id,
                                    onClick = {
                                        onSelectTask(
                                            if (state.selectedTaskId == task.id) null else task.id
                                        )
                                    },
                                    label = { Text(task.title) }
                                )
                            }
                        }
                    }
                }
            }
        }

        TimerControls(
            status = state.status,
            sessionType = state.sessionType,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onRequestEnd = { showEndConfirmDialog = true }
        )

        if (!isSessionRunningOrPaused) {
            FocuslySecondaryButton(
                text = stringResource(R.string.breathing_action_btn),
                onClick = { showBreathingResetDialog = true }
            )
        }

        // Live Distraction Blocker Status Card
        val blockerModeText = when (state.blockerState.effectiveMode) {
            DistractionBlockerMode.ACTIVE_DETECTION -> stringResource(R.string.blocker_status_active)
            DistractionBlockerMode.CHECKLIST_ONLY -> stringResource(R.string.blocker_status_checklist)
            DistractionBlockerMode.DISABLED -> stringResource(R.string.blocker_status_disabled)
        }
        FocuslyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = if (state.blockerState.isEnabled) FocuslyColors.Success else FocuslyColors.SecondaryText
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.blocker_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = FocuslyColors.PrimaryText
                        )
                        Text(
                            text = "$blockerModeText · ${state.blockerState.selectedPackageNames.size} apps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FocuslyColors.SecondaryText
                        )
                    }
                }
                TextButton(onClick = onOpenBlockerSettings) {
                    Text(
                        text = "Manage",
                        color = FocuslyColors.SecondaryAccent
                    )
                }
            }
        }
    }

    if (showEndConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            containerColor = FocuslyColors.SecondaryBackground,
            title = {
                Text(
                    text = stringResource(R.string.timer_end_confirm_title),
                    color = FocuslyColors.PrimaryText
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.timer_end_confirm_body),
                    color = FocuslyColors.SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndConfirmDialog = false
                        onEndEarly()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.timer_end_session),
                        color = FocuslyColors.Warning
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmDialog = false }) {
                    Text("Cancel", color = FocuslyColors.SecondaryText)
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
