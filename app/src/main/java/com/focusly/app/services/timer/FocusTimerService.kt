package com.focusly.app.services.timer

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.focusly.app.domain.model.DistractionPermissionStatus
import com.focusly.app.domain.model.RecoveryTriggerReason
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.repository.DistractionBlockerRepository
import com.focusly.app.domain.repository.FocusRecoveryRepository
import com.focusly.app.domain.repository.FocusSessionRepository
import com.focusly.app.domain.repository.OnboardingRepository
import com.focusly.app.domain.usecase.DashboardStatsCalculator
import com.focusly.app.domain.usecase.SessionStateMachine
import com.focusly.app.domain.usecase.TimerCalculator
import com.focusly.app.services.blocker.DistractionBlockerManager
import com.focusly.app.services.notification.FocusNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusTimerService : Service() {

    @Inject
    lateinit var focusSessionRepository: FocusSessionRepository

    @Inject
    lateinit var onboardingRepository: OnboardingRepository

    @Inject
    lateinit var notificationManager: FocusNotificationManager

    @Inject
    lateinit var distractionBlockerRepository: DistractionBlockerRepository

    @Inject
    lateinit var distractionBlockerManager: DistractionBlockerManager

    @Inject
    lateinit var focusRecoveryRepository: FocusRecoveryRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var lastDistractionCheckMs: Long = 0L
    private var lastDistractionAlertMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> serviceScope.launch { handlePause() }
            ACTION_RESUME -> serviceScope.launch { handleResume() }
            ACTION_END_EARLY -> serviceScope.launch { handleEndEarly() }
            ACTION_STOP_SERVICE -> {
                stopTickerAndService()
                return START_NOT_STICKY
            }
            else -> serviceScope.launch { startOrSync() }
        }
        return START_STICKY
    }

    private suspend fun startOrSync() {
        val snapshot = focusSessionRepository.getActiveTimerSnapshot()
        if (snapshot == null || !snapshot.isActive) {
            stopTickerAndService()
            return
        }

        val progress = TimerCalculator.calculateProgress(snapshot, System.currentTimeMillis())
        val notification = notificationManager.buildTimerNotification(
            sessionType = snapshot.sessionType,
            status = snapshot.status,
            remainingSeconds = progress.remainingSeconds
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FocusNotificationManager.NOTIFICATION_ID_TIMER,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FocusNotificationManager.NOTIFICATION_ID_TIMER, notification)
        }

        startTickerLoop()
    }

    private fun startTickerLoop() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            val sysNotifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            while (isActive) {
                val snapshot = focusSessionRepository.getActiveTimerSnapshot()
                if (snapshot == null || !snapshot.isActive) {
                    stopTickerAndService()
                    break
                }

                val nowMs = System.currentTimeMillis()
                val progress = TimerCalculator.calculateProgress(snapshot, nowMs)
                if (progress.isCompleted) {
                    val dailyTarget = onboardingRepository.preferencesFlow.first().dailyFocusTargetMinutes
                    focusSessionRepository.recordSessionCompletion(
                        snapshot = snapshot,
                        finalStatus = SessionStatus.COMPLETED,
                        actualDurationSeconds = snapshot.plannedDurationSeconds,
                        completionTimestampMs = nowMs,
                        localDate = DashboardStatsCalculator.localDateFromTimestamp(nowMs),
                        dailyTargetMinutes = dailyTarget
                    )
                    focusRecoveryRepository.resolveRecovery()
                    notificationManager.showCompletionNotification(
                        sessionType = snapshot.sessionType,
                        plannedMinutes = (snapshot.plannedDurationSeconds / 60L).toInt().coerceAtLeast(1)
                    )
                    stopTickerAndService()
                    break
                }

                // Active-only distraction awareness check (every 5 seconds, strictly while RUNNING FOCUS)
                if (snapshot.status == SessionStatus.RUNNING &&
                    snapshot.sessionType == SessionType.FOCUS &&
                    nowMs - lastDistractionCheckMs >= 5_000L
                ) {
                    lastDistractionCheckMs = nowMs
                    checkActiveDistractions(nowMs, progress.remainingSeconds)
                }

                val updatedNotification = notificationManager.buildTimerNotification(
                    sessionType = snapshot.sessionType,
                    status = snapshot.status,
                    remainingSeconds = progress.remainingSeconds
                )
                sysNotifManager.notify(FocusNotificationManager.NOTIFICATION_ID_TIMER, updatedNotification)

                delay(1_000L)
            }
        }
    }

    private suspend fun checkActiveDistractions(nowMs: Long, remainingSeconds: Long) {
        val blockerState = distractionBlockerRepository.observeBlockerState().first()
        if (!blockerState.isEnabled ||
            blockerState.selectedPackageNames.isEmpty() ||
            blockerState.permissionStatus != DistractionPermissionStatus.GRANTED
        ) {
            return
        }
        val detectedPkg = distractionBlockerManager.detectActiveDistractionPackage(
            selectedPackages = blockerState.selectedPackageNames
        )
        distractionBlockerRepository.recordForegroundCheck(detectedPkg)
        if (detectedPkg != null && nowMs - lastDistractionAlertMs >= 30_000L) {
            lastDistractionAlertMs = nowMs
            val appName = blockerState.installedApps
                .firstOrNull { it.packageName == detectedPkg }
                ?.appName
                ?: detectedPkg
            notificationManager.showDistractionReminderNotification(
                appDisplayName = appName,
                remainingSeconds = remainingSeconds
            )
        }
    }

    private suspend fun handlePause() {
        val snapshot = focusSessionRepository.getActiveTimerSnapshot() ?: return
        if (snapshot.status == SessionStatus.RUNNING) {
            val paused = SessionStateMachine.pause(snapshot, System.currentTimeMillis())
            focusSessionRepository.saveActiveTimerSnapshot(paused)
            startOrSync()
        }
    }

    private suspend fun handleResume() {
        val snapshot = focusSessionRepository.getActiveTimerSnapshot() ?: return
        if (snapshot.status == SessionStatus.PAUSED) {
            val resumed = SessionStateMachine.resume(snapshot, System.currentTimeMillis())
            focusSessionRepository.saveActiveTimerSnapshot(resumed)
            startOrSync()
        }
    }

    private suspend fun handleEndEarly() {
        val snapshot = focusSessionRepository.getActiveTimerSnapshot()
        if (snapshot != null && snapshot.isActive) {
            val nowMs = System.currentTimeMillis()
            val progress = TimerCalculator.calculateProgress(snapshot, nowMs)
            val dailyTarget = onboardingRepository.preferencesFlow.first().dailyFocusTargetMinutes
            focusSessionRepository.recordSessionCompletion(
                snapshot = snapshot,
                finalStatus = SessionStatus.ENDED_EARLY,
                actualDurationSeconds = progress.elapsedSeconds,
                completionTimestampMs = nowMs,
                localDate = DashboardStatsCalculator.localDateFromTimestamp(nowMs),
                dailyTargetMinutes = dailyTarget
            )
            if (snapshot.sessionType == SessionType.FOCUS) {
                focusRecoveryRepository.createOrUpdateRecovery(
                    lastSessionId = snapshot.sessionId,
                    triggerReason = RecoveryTriggerReason.ENDED_EARLY,
                    plannedDurationMinutes = (snapshot.plannedDurationSeconds / 60L).toInt().coerceAtLeast(1),
                    actualDurationMinutes = (progress.elapsedSeconds / 60L).toInt(),
                    subjectId = snapshot.subjectId,
                    taskId = snapshot.taskId
                )
            }
        }
        stopTickerAndService()
    }

    private fun stopTickerAndService() {
        tickJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_OR_SYNC = "com.focusly.app.action.START_OR_SYNC"
        const val ACTION_PAUSE = "com.focusly.app.action.PAUSE"
        const val ACTION_RESUME = "com.focusly.app.action.RESUME"
        const val ACTION_END_EARLY = "com.focusly.app.action.END_EARLY"
        const val ACTION_STOP_SERVICE = "com.focusly.app.action.STOP_SERVICE"

        fun startOrSync(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START_OR_SYNC
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
