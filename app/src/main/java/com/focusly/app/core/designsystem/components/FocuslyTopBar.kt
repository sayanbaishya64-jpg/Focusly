package com.focusly.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.core.designsystem.theme.FocuslyShapes

@Composable
fun FocuslyOnboardingHeader(
    step: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back_label),
                    tint = FocuslyColors.PrimaryText
                )
            }
            Text(
                text = stringResource(R.string.step_progress, step, totalSteps),
                style = MaterialTheme.typography.labelMedium,
                color = FocuslyColors.SecondaryText
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(FocuslyShapes.small),
            color = FocuslyColors.PrimaryAccent,
            trackColor = FocuslyColors.CardBackground,
            strokeCap = StrokeCap.Round
        )
    }
}
