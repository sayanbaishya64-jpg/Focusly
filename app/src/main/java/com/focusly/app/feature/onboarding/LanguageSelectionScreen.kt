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
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyOnboardingHeader
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.components.FocuslySelectionCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.core.model.AppLanguage

@Composable
fun LanguageSelectionScreen(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
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
            FocuslyOnboardingHeader(step = 4, totalSteps = 5, onBack = onBack)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_language_title),
                style = MaterialTheme.typography.headlineMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_language_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = FocuslyColors.SecondaryText
            )
            Spacer(modifier = Modifier.height(24.dp))

            FocuslySelectionCard(
                title = stringResource(R.string.lang_english),
                subtitle = "Default interface & terminology",
                icon = Icons.Rounded.Translate,
                selected = selectedLanguage == AppLanguage.ENGLISH,
                onClick = { onSelectLanguage(AppLanguage.ENGLISH) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            FocuslySelectionCard(
                title = stringResource(R.string.lang_bengali),
                subtitle = "বাংলা ইন্টারফেস ও নির্দেশনা",
                icon = Icons.Rounded.Translate,
                selected = selectedLanguage == AppLanguage.BENGALI,
                onClick = { onSelectLanguage(AppLanguage.BENGALI) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        FocuslyPrimaryButton(
            text = stringResource(R.string.continue_label),
            onClick = onNext
        )
    }
}
