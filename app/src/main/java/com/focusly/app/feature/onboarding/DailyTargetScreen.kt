package com.focusly.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyOnboardingHeader
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySelectionCard
import com.focusly.app.core.designsystem.theme.FocuslyColors

@Composable
fun DailyTargetScreen(
    selectedMinutes: Int,
    customMinutesInput: String,
    isCustomSelected: Boolean,
    onSelectPreset: (Int) -> Unit,
    onCustomInputChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val presets = listOf(30, 60, 120, 180)
    val customValid = customMinutesInput.toIntOrNull()?.let { it in 10..480 } ?: false
    val canProceed = !isCustomSelected || customValid

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
            FocuslyOnboardingHeader(step = 3, totalSteps = 5, onBack = onBack)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_target_title),
                style = MaterialTheme.typography.headlineMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_target_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = FocuslyColors.SecondaryText
            )
            Spacer(modifier = Modifier.height(24.dp))

            presets.forEach { minutes ->
                FocuslySelectionCard(
                    title = stringResource(R.string.target_minutes_unit, minutes),
                    icon = Icons.Rounded.Schedule,
                    selected = !isCustomSelected && selectedMinutes == minutes,
                    onClick = { onSelectPreset(minutes) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            FocuslySelectionCard(
                title = stringResource(R.string.target_custom),
                icon = Icons.Rounded.Tune,
                selected = isCustomSelected,
                onClick = { onCustomInputChange(customMinutesInput.ifEmpty { "90" }) }
            )

            if (isCustomSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customMinutesInput,
                    onValueChange = onCustomInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.target_custom_hint)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FocuslyColors.PrimaryAccent,
                        unfocusedBorderColor = FocuslyColors.CardBorder,
                        focusedTextColor = FocuslyColors.PrimaryText,
                        unfocusedTextColor = FocuslyColors.PrimaryText,
                        focusedLabelColor = FocuslyColors.SecondaryAccent,
                        unfocusedLabelColor = FocuslyColors.SecondaryText,
                        focusedContainerColor = FocuslyColors.CardBackground,
                        unfocusedContainerColor = FocuslyColors.CardBackground
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        FocuslyPrimaryButton(
            text = stringResource(R.string.continue_label),
            enabled = canProceed,
            onClick = onNext
        )
    }
}
