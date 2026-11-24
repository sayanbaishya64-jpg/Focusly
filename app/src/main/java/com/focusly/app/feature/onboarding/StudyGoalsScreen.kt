package com.focusly.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyOnboardingHeader
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySelectionCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.core.model.StudyGoal

@Composable
fun StudyGoalsScreen(
    selectedGoals: Set<StudyGoal>,
    onToggleGoal: (StudyGoal) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val goals = listOf(
        Triple(StudyGoal.IMPROVE_CONCENTRATION, R.string.goal_concentration, Icons.Rounded.CenterFocusStrong),
        Triple(StudyGoal.REDUCE_DISTRACTIONS, R.string.goal_distractions, Icons.Rounded.NotificationsOff),
        Triple(StudyGoal.BUILD_CONSISTENCY, R.string.goal_consistency, Icons.Rounded.CalendarMonth),
        Triple(StudyGoal.COMPLETE_DAILY_GOALS, R.string.goal_daily_goals, Icons.Rounded.TaskAlt)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocuslyColors.PrimaryBackground)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            FocuslyOnboardingHeader(step = 2, totalSteps = 5, onBack = onBack)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_goals_title),
                style = MaterialTheme.typography.headlineMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_goals_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = FocuslyColors.SecondaryText
            )
            Spacer(modifier = Modifier.height(24.dp))

            goals.forEach { (goal, titleRes, icon) ->
                FocuslySelectionCard(
                    title = stringResource(titleRes),
                    icon = icon,
                    selected = goal in selectedGoals,
                    onClick = { onToggleGoal(goal) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        FocuslyPrimaryButton(
            text = stringResource(R.string.continue_label),
            enabled = selectedGoals.isNotEmpty(),
            onClick = onNext
        )
    }
}
