package com.focusly.app.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.StudyTask

@Composable
fun CurrentTaskCard(
    task: StudyTask?,
    onAddTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocuslyCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_current_task),
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
            TextButton(onClick = onAddTaskClick) {
                Text(
                    text = stringResource(R.string.home_add_task),
                    style = MaterialTheme.typography.labelLarge,
                    color = FocuslyColors.SecondaryAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (task == null) {
            Text(
                text = stringResource(R.string.home_no_task_selected),
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.SecondaryText
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.subjectName ?: "General Study",
                    style = MaterialTheme.typography.labelMedium,
                    color = FocuslyColors.SecondaryAccent
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = "${task.plannedDurationMinutes} min · ${task.status.name.replace('_', ' ')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
        }
    }
}
