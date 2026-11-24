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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.components.FocuslyOnboardingHeader
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySelectionCard
import com.focusly.app.core.designsystem.components.FocuslyTextAction
import com.focusly.app.core.designsystem.theme.FocuslyColors

private val commonDistractionCandidates = listOf(
    "com.instagram.android" to "Instagram",
    "com.google.android.youtube" to "YouTube",
    "com.zhiliaoapp.musically" to "Short Video Apps",
    "com.whatsapp" to "WhatsApp"
)

@Composable
fun PermissionExplanationScreen(
    selectedApps: Set<String>,
    onToggleApp: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
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
            FocuslyOnboardingHeader(step = 5, totalSteps = 5, onBack = onBack)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_distraction_title),
                style = MaterialTheme.typography.headlineMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_distraction_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = FocuslyColors.SecondaryText
            )
            Spacer(modifier = Modifier.height(20.dp))

            FocuslyCard {
                Text(
                    text = stringResource(R.string.permission_usage_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.SecondaryAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.permission_usage_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.permission_notif_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.SecondaryAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.permission_notif_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            commonDistractionCandidates.forEach { (packageName, label) ->
                FocuslySelectionCard(
                    title = label,
                    subtitle = packageName,
                    icon = Icons.Rounded.Apps,
                    selected = packageName in selectedApps,
                    onClick = { onToggleApp(packageName) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Column {
            FocuslyPrimaryButton(
                text = stringResource(R.string.finish_setup),
                onClick = onFinish
            )
            Spacer(modifier = Modifier.height(6.dp))
            FocuslyTextAction(
                text = stringResource(R.string.skip_label),
                onClick = onSkip,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
            )
        }
    }
}
