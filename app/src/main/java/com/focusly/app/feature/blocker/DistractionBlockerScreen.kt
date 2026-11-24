package com.focusly.app.feature.blocker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySecondaryButton
import com.focusly.app.core.designsystem.components.FocuslySelectionCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.DistractionBlockerMode
import com.focusly.app.domain.model.DistractionBlockerState
import com.focusly.app.domain.model.DistractionPermissionStatus

@Composable
fun DistractionBlockerScreen(
    state: DistractionBlockerState,
    testStatusMessage: String?,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val statusText = when (state.effectiveMode) {
        DistractionBlockerMode.ACTIVE_DETECTION -> stringResource(R.string.blocker_status_active)
        DistractionBlockerMode.CHECKLIST_ONLY -> stringResource(R.string.blocker_status_checklist)
        DistractionBlockerMode.DISABLED -> stringResource(R.string.blocker_status_disabled)
    }

    val permissionGranted = state.permissionStatus == DistractionPermissionStatus.GRANTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocuslyColors.PrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = FocuslyColors.PrimaryText
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.blocker_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = stringResource(R.string.blocker_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
        }

        // Enable / Disable Toggle + Mode Summary
        FocuslyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.blocker_enable_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = FocuslyColors.PrimaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isEnabled) FocuslyColors.SecondaryAccent else FocuslyColors.SecondaryText
                    )
                }
                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FocuslyColors.PrimaryText,
                        checkedTrackColor = FocuslyColors.PrimaryAccent
                    )
                )
            }
        }

        // Usage Access Permission Card + Android Settings Shortcuts (Grant or Revoke)
        FocuslyCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (permissionGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = if (permissionGranted) FocuslyColors.Success else FocuslyColors.Warning,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (permissionGranted) {
                        stringResource(R.string.blocker_perm_granted)
                    } else {
                        stringResource(R.string.blocker_perm_denied)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.PrimaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.blocker_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (!permissionGranted) {
                FocuslyPrimaryButton(
                    text = stringResource(R.string.blocker_grant_perm),
                    onClick = onOpenUsageAccessSettings
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                FocuslySecondaryButton(
                    text = stringResource(R.string.blocker_revoke_perm),
                    onClick = onOpenUsageAccessSettings
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            FocuslySecondaryButton(
                text = stringResource(R.string.blocker_test_status),
                onClick = onRefreshStatus
            )

            if (!testStatusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = testStatusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryAccent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            FocuslySecondaryButton(
                text = "App Info & Permissions",
                onClick = onOpenAppSettings
            )
        }

        // Launchable Installed Apps Picker
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = FocuslyColors.SecondaryAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "${stringResource(R.string.blocker_selected_apps)} (${state.selectedPackageNames.size})",
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
        }

        if (state.installedApps.isEmpty()) {
            FocuslyCard {
                Text(
                    text = "No launchable apps detected in this environment. You can still use the Focus Timer completely offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
        } else {
            state.installedApps.forEach { app ->
                val selected = app.packageName in state.selectedPackageNames
                FocuslySelectionCard(
                    title = app.appName,
                    subtitle = if (app.isSuggestedDefault) "Common distraction · ${app.packageName}" else app.packageName,
                    icon = Icons.Rounded.Apps,
                    selected = selected,
                    onClick = { onToggleApp(app.packageName) }
                )
            }
        }
    }
}
