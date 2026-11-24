package com.focusly.app.services.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focusly.app.MainActivity
import com.focusly.app.R
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.usecase.TimerCalculator
import com.focusly.app.services.timer.FocusTimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_TIMER = "focusly_active_timer"
        const val CHANNEL_COMPLETION = "focusly_session_completion"
        const val CHANNEL_DISTRACTION = "focusly_distraction_awareness"
        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_COMPLETION = 1002
        const val NOTIFICATION_ID_DISTRACTION = 1003
    }

    fun ensureChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            description = "Displays the ongoing focus or break countdown timer"
        }

        val completionChannel = NotificationChannel(
            CHANNEL_COMPLETION,
            context.getString(R.string.timer_completion_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a focus or break session finishes"
        }

        val distractionChannel = NotificationChannel(
            CHANNEL_DISTRACTION,
            context.getString(R.string.blocker_title),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gentle reminder when a selected distracting app opens during an active focus block"
        }

        manager.createNotificationChannel(timerChannel)
        manager.createNotificationChannel(completionChannel)
        manager.createNotificationChannel(distractionChannel)
    }

    fun buildTimerNotification(
        sessionType: SessionType,
        status: SessionStatus,
        remainingSeconds: Long
    ): Notification {
        ensureChannels()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTime = TimerCalculator.formatDuration(remainingSeconds)
        val title = when (sessionType) {
            SessionType.FOCUS -> if (status == SessionStatus.PAUSED) {
                "Focus Paused"
            } else {
                "Focus Session Active"
            }
            SessionType.BREAK -> if (status == SessionStatus.PAUSED) {
                "Break Paused"
            } else {
                "Break Active"
            }
        }

        val toggleActionIntent = Intent(context, FocusTimerService::class.java).apply {
            action = if (status == SessionStatus.RUNNING) {
                FocusTimerService.ACTION_PAUSE
            } else {
                FocusTimerService.ACTION_RESUME
            }
        }
        val togglePendingIntent = PendingIntent.getService(
            context,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_END_EARLY
        }
        val endPendingIntent = PendingIntent.getService(
            context,
            2,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleLabel = if (status == SessionStatus.RUNNING) {
            context.getString(R.string.timer_pause)
        } else {
            context.getString(R.string.timer_resume)
        }

        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Remaining: $formattedTime")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, toggleLabel, togglePendingIntent)
            .addAction(0, context.getString(R.string.timer_end_session), endPendingIntent)
            .build()
    }

    fun showCompletionNotification(sessionType: SessionType, plannedMinutes: Int) {
        ensureChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = if (sessionType == SessionType.FOCUS) {
            context.getString(R.string.timer_completed_title)
        } else {
            context.getString(R.string.timer_break_completed_title)
        }
        val body = if (sessionType == SessionType.FOCUS) {
            "Completed $plannedMinutes minutes of focused study."
        } else {
            "Your $plannedMinutes-minute break is complete."
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            10,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_COMPLETION, notification)
    }

    fun showDistractionReminderNotification(appDisplayName: String, remainingSeconds: Long) {
        ensureChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            11,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatted = TimerCalculator.formatDuration(remainingSeconds)
        val notification = NotificationCompat.Builder(context, CHANNEL_DISTRACTION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Stay with your focus block")
            .setContentText("$appDisplayName opened while $formatted remains. Tap to return to FOCUSLY.")
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DISTRACTION, notification)
    }
}
