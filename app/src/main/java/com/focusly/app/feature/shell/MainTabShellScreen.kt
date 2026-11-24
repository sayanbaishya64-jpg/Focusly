package com.focusly.app.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.core.model.OnboardingPreferences
import com.focusly.app.navigation.BottomNavTab
import java.time.LocalTime

@Composable
fun MainTabShellScreen(
    tab: BottomNavTab,
    preferences: OnboardingPreferences,
    onStartFocusClick: () -> Unit
) {
    val hour = LocalTime.now().hour
    val greetingRes = when {
        hour < 12 -> R.string.greeting_morning
        hour < 17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocuslyColors.PrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (tab == BottomNavTab.HOME) stringResource(greetingRes) else stringResource(tab.labelRes),
            style = MaterialTheme.typography.headlineMedium,
            color = FocuslyColors.PrimaryText
        )

        FocuslyCard {
            Text(
                text = stringResource(R.string.phase_one_ready),
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.SecondaryAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.daily_target_label)}: ${preferences.dailyFocusTargetMinutes} min",
                style = MaterialTheme.typography.bodyLarge,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Category: ${preferences.studentCategory.name.replace('_', ' ')}",
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
        }

        if (tab == BottomNavTab.HOME) {
            FocuslyPrimaryButton(
                text = stringResource(R.string.nav_focus),
                onClick = onStartFocusClick
            )
        }

        val phaseMessage = when (tab) {
            BottomNavTab.HOME -> "Phase 2 will activate the full Home Dashboard with live Focus progress ring, active task card, Focus Coins, Virtual Garden preview, and Daily Missions."
            BottomNavTab.FOCUS -> "Phase 2 will implement the Smart Focus Timer (15/25/45/60/90 min + custom), Manual/Automatic/Smart modes, and Foreground Service persistence."
            BottomNavTab.STUDY -> "Phase 3 will implement Subject Management, Study Tasks, Priorities, Daily Schedules, and rescheduling."
            BottomNavTab.PROGRESS -> "Phase 6 will implement Room-backed Study Analytics, Adaptive Focus Engine (FAFE) insights, and completion rate charts."
            BottomNavTab.PROFILE -> ""
        }

        FocuslyCard {
            Text(
                text = "Module Roadmap",
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = phaseMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
        }
    }
}
