package com.focusly.app.feature.rewards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.AchievementItem
import com.focusly.app.domain.model.GardenGrowthStage
import com.focusly.app.domain.model.GardenStoreItem
import com.focusly.app.domain.model.RewardPurchaseResult
import com.focusly.app.domain.model.RewardTransaction
import com.focusly.app.domain.model.RewardsDashboardState

@Composable
fun RewardsScreen(
    state: RewardsDashboardState,
    purchaseMessage: RewardPurchaseResult?,
    onBack: () -> Unit,
    onPurchaseItem: (String) -> Unit,
    onSetFeaturedItem: (String) -> Unit,
    onDismissPurchaseMessage: () -> Unit
) {
    val featuredItem = state.storeItems.firstOrNull { it.isFeatured }
        ?: state.storeItems.firstOrNull { it.isUnlocked }
        ?: state.storeItems.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocuslyColors.PrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.rewards_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = stringResource(R.string.rewards_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
        }

        if (purchaseMessage != null) {
            val (msgText, msgColor) = when (purchaseMessage) {
                RewardPurchaseResult.Success ->
                    stringResource(R.string.rewards_purchase_success) to FocuslyColors.Success
                RewardPurchaseResult.InsufficientCoins ->
                    stringResource(R.string.rewards_purchase_insufficient) to FocuslyColors.Warning
                RewardPurchaseResult.AlreadyUnlocked ->
                    stringResource(R.string.rewards_purchase_already) to FocuslyColors.SecondaryAccent
                RewardPurchaseResult.NotFound ->
                    stringResource(R.string.rewards_purchase_not_found) to FocuslyColors.Warning
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = FocuslyColors.SecondaryBackground,
                border = BorderStroke(1.dp, msgColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = msgText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = msgColor,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismissPurchaseMessage) {
                        Text(
                            text = stringResource(R.string.study_close),
                            color = FocuslyColors.SecondaryText
                        )
                    }
                }
            }
        }

        // Focus Coins & Streak Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FocuslyCard(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonetizationOn,
                        contentDescription = null,
                        tint = FocuslyColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.home_focus_coins),
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.SecondaryText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${state.coinBalance}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = stringResource(R.string.rewards_lifetime_earned, state.totalEarnedCoins),
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuslyColors.SecondaryText
                )
            }

            FocuslyCard(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Whatshot,
                        contentDescription = null,
                        tint = FocuslyColors.SecondaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.rewards_streak_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.SecondaryText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${state.currentStreakDays}d",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = stringResource(
                        R.string.rewards_sessions_completed_count,
                        state.completedFocusSessionsCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuslyColors.SecondaryText
                )
            }
        }

        // Virtual Garden Showcase Card
        FocuslyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFlorist,
                        contentDescription = null,
                        tint = FocuslyColors.Success,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.rewards_garden_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = FocuslyColors.PrimaryText
                    )
                }

                val stageLabel = when (featuredItem?.growthStage ?: GardenGrowthStage.SEEDLING) {
                    GardenGrowthStage.SEEDLING -> stringResource(R.string.rewards_stage_seedling)
                    GardenGrowthStage.SPROUTING -> stringResource(R.string.rewards_stage_sprouting)
                    GardenGrowthStage.BLOOMING -> stringResource(R.string.rewards_stage_blooming)
                }
                Text(
                    text = stageLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = FocuslyColors.SecondaryAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val stage = featuredItem?.growthStage ?: GardenGrowthStage.SEEDLING
            val accentColor = featuredItem?.let { Color(it.accentHex) } ?: FocuslyColors.Success

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocuslyColors.SecondaryBackground),
                    contentAlignment = Alignment.Center
                ) {
                    GardenPlantArtwork(
                        stage = stage,
                        accent = accentColor,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = featuredItem?.title ?: "Sunlit Sprout",
                        style = MaterialTheme.typography.titleLarge,
                        color = FocuslyColors.PrimaryText
                    )
                    Text(
                        text = featuredItem?.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = FocuslyColors.SecondaryText
                    )
                    val growthHint = when (stage) {
                        GardenGrowthStage.SEEDLING -> stringResource(R.string.rewards_garden_hint_seedling)
                        GardenGrowthStage.SPROUTING -> stringResource(R.string.rewards_garden_hint_sprouting)
                        GardenGrowthStage.BLOOMING -> stringResource(R.string.rewards_garden_hint_blooming)
                    }
                    Text(
                        text = growthHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.Success
                    )
                }
            }
        }

        // Achievements Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = FocuslyColors.Warning,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.home_achievements),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.PrimaryText
                )
            }
            Text(
                text = "${state.unlockedAchievementsCount} / ${state.totalAchievementsCount}",
                style = MaterialTheme.typography.labelLarge,
                color = FocuslyColors.SecondaryAccent
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.achievements.forEach { achievement ->
                AchievementRowCard(achievement = achievement)
            }
        }

        // Reward Store Section
        Text(
            text = stringResource(R.string.rewards_store_title),
            style = MaterialTheme.typography.titleMedium,
            color = FocuslyColors.PrimaryText
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.storeItems.forEach { item ->
                GardenStoreItemCard(
                    item = item,
                    canAfford = state.coinBalance >= item.costCoins,
                    onPurchase = { onPurchaseItem(item.id) },
                    onSetFeatured = { onSetFeaturedItem(item.id) }
                )
            }
        }

        // Recent Coin Ledger
        Text(
            text = stringResource(R.string.rewards_ledger_title),
            style = MaterialTheme.typography.titleMedium,
            color = FocuslyColors.PrimaryText
        )

        FocuslyCard {
            if (state.recentTransactions.isEmpty()) {
                Text(
                    text = stringResource(R.string.rewards_ledger_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.recentTransactions.forEach { tx ->
                        TransactionRow(transaction = tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRowCard(achievement: AchievementItem) {
    FocuslyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) {
                            FocuslyColors.Success.copy(alpha = 0.16f)
                        } else {
                            FocuslyColors.SecondaryBackground
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (achievement.isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked) FocuslyColors.Success else FocuslyColors.SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (achievement.isUnlocked) FocuslyColors.PrimaryText else FocuslyColors.SecondaryText
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuslyColors.SecondaryText
                )
            }

            Text(
                text = "+${achievement.coinBonus}",
                style = MaterialTheme.typography.labelLarge,
                color = if (achievement.isUnlocked) FocuslyColors.Success else FocuslyColors.Warning
            )
        }
    }
}

@Composable
private fun GardenStoreItemCard(
    item: GardenStoreItem,
    canAfford: Boolean,
    onPurchase: () -> Unit,
    onSetFeatured: () -> Unit
) {
    val accent = Color(item.accentHex)
    FocuslyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FocuslyColors.SecondaryBackground),
                contentAlignment = Alignment.Center
            ) {
                GardenPlantArtwork(
                    stage = item.growthStage,
                    accent = accent,
                    modifier = Modifier.size(38.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = FocuslyColors.SecondaryText
                )
            }

            if (item.isUnlocked) {
                if (item.isFeatured) {
                    Text(
                        text = stringResource(R.string.rewards_featured_badge),
                        style = MaterialTheme.typography.labelLarge,
                        color = FocuslyColors.Success
                    )
                } else {
                    OutlinedButton(
                        onClick = onSetFeatured,
                        modifier = Modifier.heightIn(min = 44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FocuslyColors.CardBorder)
                    ) {
                        Text(
                            text = stringResource(R.string.rewards_set_featured),
                            style = MaterialTheme.typography.labelMedium,
                            color = FocuslyColors.PrimaryText
                        )
                    }
                }
            } else {
                Button(
                    onClick = onPurchase,
                    enabled = canAfford,
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FocuslyColors.PrimaryAccent,
                        contentColor = FocuslyColors.PrimaryText,
                        disabledContainerColor = FocuslyColors.SecondaryBackground,
                        disabledContentColor = FocuslyColors.SecondaryText
                    )
                ) {
                    Text(
                        text = "${item.costCoins}c",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: RewardTransaction) {
    val isEarn = transaction.amount >= 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.bodyMedium,
                color = FocuslyColors.PrimaryText
            )
            Text(
                text = transaction.localDate,
                style = MaterialTheme.typography.labelSmall,
                color = FocuslyColors.SecondaryText
            )
        }
        Text(
            text = if (isEarn) "+${transaction.amount}" else "${transaction.amount}",
            style = MaterialTheme.typography.titleSmall,
            color = if (isEarn) FocuslyColors.Success else FocuslyColors.Warning
        )
    }
}

@Composable
private fun GardenPlantArtwork(
    stage: GardenGrowthStage,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Planter pot
        drawRoundRect(
            color = Color(0xFF2B3E5E),
            topLeft = Offset(w * 0.25f, h * 0.72f),
            size = Size(w * 0.50f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )

        // Stem
        val stemTop = when (stage) {
            GardenGrowthStage.SEEDLING -> h * 0.46f
            GardenGrowthStage.SPROUTING -> h * 0.34f
            GardenGrowthStage.BLOOMING -> h * 0.26f
        }
        drawLine(
            color = accent,
            start = Offset(w * 0.5f, h * 0.72f),
            end = Offset(w * 0.5f, stemTop),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round
        )

        // Left & right leaves
        val leftLeaf = Path().apply {
            moveTo(w * 0.5f, h * 0.54f)
            quadraticTo(w * 0.22f, h * 0.42f, w * 0.26f, h * 0.62f)
            close()
        }
        val rightLeaf = Path().apply {
            moveTo(w * 0.5f, h * 0.50f)
            quadraticTo(w * 0.78f, h * 0.38f, w * 0.74f, h * 0.58f)
            close()
        }
        drawPath(leftLeaf, color = accent.copy(alpha = 0.85f))
        if (stage != GardenGrowthStage.SEEDLING) {
            drawPath(rightLeaf, color = accent)
        }

        // Blossom or bud for sprouting / blooming
        when (stage) {
            GardenGrowthStage.SEEDLING -> {
                drawCircle(
                    color = accent.copy(alpha = 0.9f),
                    radius = w * 0.08f,
                    center = Offset(w * 0.5f, stemTop)
                )
            }
            GardenGrowthStage.SPROUTING -> {
                drawCircle(
                    color = accent,
                    radius = w * 0.11f,
                    center = Offset(w * 0.5f, stemTop)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = w * 0.05f,
                    center = Offset(w * 0.5f, stemTop)
                )
            }
            GardenGrowthStage.BLOOMING -> {
                val center = Offset(w * 0.5f, stemTop)
                drawCircle(
                    color = accent.copy(alpha = 0.35f),
                    radius = w * 0.20f,
                    center = center,
                    style = Stroke(width = w * 0.05f)
                )
                drawCircle(
                    color = accent,
                    radius = w * 0.13f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = w * 0.06f,
                    center = center
                )
            }
        }
    }
}
