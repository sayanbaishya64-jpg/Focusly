package com.focusly.app.services.blocker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.focusly.app.core.model.OnboardingDefaults
import com.focusly.app.domain.model.DistractionPermissionStatus
import com.focusly.app.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Permission-safe Distraction Blocker helper.
 * - Never blocks Settings, Phone/Dialer, Emergency, or FOCUSLY itself.
 * - Uses only Android 11+ `<queries>` MAIN/LAUNCHER lookup for user-installed apps.
 * - Uses UsageStatsManager strictly during active focus sessions when Usage Access is granted.
 */
@Singleton
class DistractionBlockerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val protectedPackages = setOf(
        context.packageName,
        "com.android.settings",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.phone",
        "com.android.emergency",
        "com.android.systemui"
    )

    private val suggestedPackages = OnboardingDefaults.commonDistractionApps
        .map { it.packageName }
        .toSet()

    fun getUsagePermissionStatus(): DistractionPermissionStatus {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            if (mode == AppOpsManager.MODE_ALLOWED) {
                DistractionPermissionStatus.GRANTED
            } else {
                DistractionPermissionStatus.DENIED
            }
        } catch (_: Throwable) {
            DistractionPermissionStatus.DENIED
        }
    }

    fun getLaunchableApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
        } catch (_: Throwable) {
            emptyList()
        }

        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg in protectedPackages) return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }
                    .getOrDefault(pkg)
                    .trim()
                if (label.isEmpty()) return@mapNotNull null
                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    isSuggestedDefault = pkg in suggestedPackages
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareByDescending<InstalledAppInfo> { it.isSuggestedDefault }
                    .thenBy { it.appName.lowercase() }
            )
    }

    /**
     * Checks recent foreground app transition only when Usage Access is granted and a focus session is active.
     * Returns the matched distracting package name if one of [selectedPackages] recently moved to foreground.
     */
    fun detectActiveDistractionPackage(selectedPackages: Set<String>, lookbackMs: Long = 10_000L): String? {
        if (selectedPackages.isEmpty()) return null
        if (getUsagePermissionStatus() != DistractionPermissionStatus.GRANTED) return null

        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val begin = end - lookbackMs
            val events = usageStatsManager.queryEvents(begin, end) ?: return null
            val event = UsageEvents.Event()
            var latestForegroundPkg: String? = null
            var latestTimestamp = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForegroundEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                } else {
                    @Suppress("DEPRECATION")
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                }
                if (isForegroundEvent && event.timeStamp >= latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    latestForegroundPkg = event.packageName
                }
            }

            if (latestForegroundPkg != null &&
                latestForegroundPkg !in protectedPackages &&
                latestForegroundPkg in selectedPackages
            ) {
                latestForegroundPkg
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun buildUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildAppDetailsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
