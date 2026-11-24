package com.focusly.app.feature.shell

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySecondaryButton
import com.focusly.app.core.designsystem.components.FocuslySelectionCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.core.model.AppLanguage
import com.focusly.app.core.model.OnboardingPreferences
import java.time.LocalDate

@Composable
fun ProfileShellScreen(
    preferences: OnboardingPreferences,
    studentName: String,
    backupStatus: BackupStatusUi?,
    onSaveStudentName: (String) -> Unit,
    onOpenBlockerSettings: () -> Unit,
    onOpenRewards: () -> Unit,
    onExportBackupUri: (android.net.Uri) -> Unit,
    onImportBackupUri: (android.net.Uri) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onResetOnboarding: () -> Unit
) {
    var nameDraft by remember(studentName) { mutableStateOf(studentName) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackupUri(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportBackupUri(uri)
        }
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
            text = stringResource(R.string.nav_profile),
            style = MaterialTheme.typography.headlineMedium,
            color = FocuslyColors.PrimaryText
        )

        FocuslyCard {
            Text(
                text = "Student Name (Optional)",
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                label = { Text("Your name for dashboard greeting") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            FocuslyPrimaryButton(
                text = "Save Name",
                onClick = { onSaveStudentName(nameDraft) }
            )
        }

        FocuslyCard {
            Text(
                text = stringResource(R.string.blocker_title),
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.blocker_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            FocuslyPrimaryButton(
                text = "Manage Distraction Blocker",
                onClick = onOpenBlockerSettings
            )
        }

        FocuslyCard {
            Text(
                text = stringResource(R.string.rewards_title),
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.rewards_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            FocuslyPrimaryButton(
                text = stringResource(R.string.home_view_rewards),
                onClick = onOpenRewards
            )
        }

        FocuslyCard {
            Text(
                text = stringResource(R.string.backup_title),
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.backup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
            if (backupStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val statusText = when (backupStatus) {
                    BackupStatusUi.ExportSuccess -> stringResource(R.string.backup_export_success)
                    is BackupStatusUi.ImportSuccess -> stringResource(
                        R.string.backup_import_success,
                        backupStatus.sessionsCount,
                        backupStatus.tasksCount,
                        backupStatus.subjectsCount
                    )
                    is BackupStatusUi.Error -> stringResource(R.string.backup_import_error, backupStatus.reason)
                }
                val statusColor = when (backupStatus) {
                    is BackupStatusUi.Error -> FocuslyColors.Warning
                    else -> FocuslyColors.Success
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            FocuslyPrimaryButton(
                text = stringResource(R.string.backup_export_btn),
                onClick = { exportLauncher.launch("focusly-backup-${LocalDate.now()}.json") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            FocuslySecondaryButton(
                text = stringResource(R.string.backup_import_btn),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            )
        }

        FocuslyCard {
            Text(
                text = "Saved Onboarding Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Category: ${preferences.studentCategory.key}",
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
            Text(
                text = "Daily Focus Target: ${preferences.dailyFocusTargetMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
            Text(
                text = "Study Goals: ${preferences.studyGoals.joinToString { it.key }}",
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
            Text(
                text = "Distraction Apps Selected: ${preferences.selectedDistractionApps.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
        }

        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.titleMedium,
            color = FocuslyColors.PrimaryText
        )

        FocuslySelectionCard(
            title = stringResource(R.string.lang_english),
            icon = Icons.Rounded.Translate,
            selected = preferences.language == AppLanguage.ENGLISH,
            onClick = { onLanguageChange(AppLanguage.ENGLISH) }
        )

        FocuslySelectionCard(
            title = stringResource(R.string.lang_bengali),
            icon = Icons.Rounded.Translate,
            selected = preferences.language == AppLanguage.BENGALI,
            onClick = { onLanguageChange(AppLanguage.BENGALI) }
        )

        Spacer(modifier = Modifier.height(4.dp))
        FocuslySecondaryButton(
            text = "Reset Local Data & Re-run Onboarding",
            onClick = onResetOnboarding
        )
    }
}
