package com.focusly.app.feature.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
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
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySecondaryButton
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.FocusRecoveryState

@Composable
fun FocusRecoveryCard(
    recoveryState: FocusRecoveryState,
    onRestartShorter: (Int) -> Unit,
    onTakeBreak: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCalmReset: (() -> Unit)? = null
) {
    FocuslyCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = FocuslyColors.SecondaryAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.recovery_title),
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.recovery_fresh_start),
            style = MaterialTheme.typography.bodyMedium,
            color = FocuslyColors.SecondaryText
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.recovery_shorter_prompt, recoveryState.suggestedRetryMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = FocuslyColors.SecondaryAccent
        )

        Spacer(modifier = Modifier.height(14.dp))

        FocuslyPrimaryButton(
            text = stringResource(R.string.recovery_shorter_btn, recoveryState.suggestedRetryMinutes),
            onClick = { onRestartShorter(recoveryState.suggestedRetryMinutes) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (onCalmReset != null) {
            FocuslySecondaryButton(
                text = stringResource(R.string.breathing_action_btn),
                onClick = onCalmReset
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FocuslySecondaryButton(
                text = stringResource(R.string.timer_take_break),
                onClick = onTakeBreak,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.recovery_dismiss_btn),
                    color = FocuslyColors.SecondaryText
                )
            }
        }
    }
}
