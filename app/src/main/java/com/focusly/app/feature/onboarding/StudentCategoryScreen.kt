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
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.School
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
import com.focusly.app.core.model.StudentCategory

@Composable
fun StudentCategoryScreen(
    selectedCategory: StudentCategory,
    onSelectCategory: (StudentCategory) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val options = listOf(
        Triple(StudentCategory.SCHOOL_STUDENT, R.string.category_school, R.string.category_school_desc) to Icons.Rounded.Backpack,
        Triple(StudentCategory.COLLEGE_STUDENT, R.string.category_college, R.string.category_college_desc) to Icons.Rounded.School,
        Triple(StudentCategory.COMPETITIVE_ASPIRANT, R.string.category_competitive, R.string.category_competitive_desc) to Icons.Rounded.AutoStories,
        Triple(StudentCategory.SELF_LEARNER, R.string.category_self, R.string.category_self_desc) to Icons.Rounded.Explore
    )

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
            FocuslyOnboardingHeader(step = 1, totalSteps = 5, onBack = onBack)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_category_title),
                style = MaterialTheme.typography.headlineMedium,
                color = FocuslyColors.PrimaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_category_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = FocuslyColors.SecondaryText
            )
            Spacer(modifier = Modifier.height(24.dp))

            options.forEach { (meta, icon) ->
                val (category, titleRes, descRes) = meta
                FocuslySelectionCard(
                    title = stringResource(titleRes),
                    subtitle = stringResource(descRes),
                    icon = icon,
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        FocuslyPrimaryButton(
            text = stringResource(R.string.continue_label),
            onClick = onNext
        )
    }
}
