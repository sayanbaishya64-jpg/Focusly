package com.focusly.app.feature.recommendation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
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
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.FocusRecommendation
import com.focusly.app.domain.model.RecommendationStatus

@Composable
fun FocusRecommendationCard(
    recommendation: FocusRecommendation,
    onApplyRecommended: (Int) -> Unit,
    onIgnore: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPrimaryStartLabel: Boolean = false
) {
    if (recommendation.status == RecommendationStatus.IGNORED) {
        return
    }

    FocuslyCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = FocuslyColors.SecondaryAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (recommendation.hasEnoughData) {
                    stringResource(R.string.adaptive_title)
                } else {
                    stringResource(R.string.adaptive_insufficient_title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = FocuslyColors.PrimaryText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (recommendation.hasEnoughData) {
                recommendation.reasonText
            } else {
                stringResource(R.string.adaptive_insufficient_desc)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = FocuslyColors.SecondaryText
        )

        if (recommendation.hasEnoughData) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Based on ${recommendation.basedOnSessionCount} local focus sessions · Rule-based",
                style = MaterialTheme.typography.labelMedium,
                color = FocuslyColors.SecondaryAccent
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FocuslyPrimaryButton(
                    text = if (isPrimaryStartLabel) {
                        stringResource(R.string.adaptive_start_recommended, recommendation.recommendedMinutes)
                    } else {
                        stringResource(R.string.adaptive_use_duration, recommendation.recommendedMinutes)
                    },
                    onClick = { onApplyRecommended(recommendation.recommendedMinutes) },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { onIgnore(recommendation.id) }
                ) {
                    Text(
                        text = stringResource(R.string.adaptive_ignore),
                        color = FocuslyColors.SecondaryText
                    )
                }
            }
        }
    }
}
