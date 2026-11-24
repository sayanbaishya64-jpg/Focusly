package com.focusly.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.focusly.app.core.designsystem.theme.FocuslyColors

@Composable
fun FocuslyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = FocuslyColors.CardBackground,
        border = BorderStroke(1.dp, FocuslyColors.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun FocuslySelectionCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            FocuslyColors.PrimaryAccent.copy(alpha = 0.16f)
        } else {
            FocuslyColors.CardBackground
        },
        label = "selectionCardColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) FocuslyColors.PrimaryAccent else FocuslyColors.CardBorder,
        label = "selectionBorderColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) FocuslyColors.SecondaryAccent else FocuslyColors.SecondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = FocuslyColors.PrimaryText
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FocuslyColors.SecondaryText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) FocuslyColors.PrimaryAccent else FocuslyColors.SecondaryText.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
