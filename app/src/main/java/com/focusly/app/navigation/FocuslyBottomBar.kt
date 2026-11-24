package com.focusly.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.theme.FocuslyColors

enum class BottomNavTab(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = FocuslyRoute.Home.route,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    FOCUS(
        route = FocuslyRoute.Focus.route,
        labelRes = R.string.nav_focus,
        selectedIcon = Icons.Rounded.Timer,
        unselectedIcon = Icons.Outlined.Timer
    ),
    STUDY(
        route = FocuslyRoute.Study.route,
        labelRes = R.string.nav_study,
        selectedIcon = Icons.Rounded.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    ),
    PROGRESS(
        route = FocuslyRoute.Progress.route,
        labelRes = R.string.nav_progress,
        selectedIcon = Icons.Rounded.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    ),
    PROFILE(
        route = FocuslyRoute.Profile.route,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

@Composable
fun FocuslyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = FocuslyColors.SecondaryBackground,
        contentColor = FocuslyColors.PrimaryText,
        tonalElevation = 0.dp
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FocuslyColors.PrimaryAccent,
                    selectedTextColor = FocuslyColors.PrimaryAccent,
                    unselectedIconColor = FocuslyColors.SecondaryText,
                    unselectedTextColor = FocuslyColors.SecondaryText,
                    indicatorColor = FocuslyColors.PrimaryAccent.copy(alpha = 0.14f)
                )
            )
        }
    }
}
