package com.focusly.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusly.app.domain.model.ActiveTimerSnapshot
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activeTimerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focusly_active_timer"
)

@Singleton
class ActiveTimerStateStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SESSION_ID = stringPreferencesKey("active_session_id")
        val SESSION_TYPE = stringPreferencesKey("active_session_type")
        val PLANNED_DURATION_SECONDS = longPreferencesKey("active_planned_seconds")
        val START_TIMESTAMP_MS = longPreferencesKey("active_start_ms")
        val PAUSE_STARTED_MS = longPreferencesKey("active_pause_started_ms")
        val ACCUMULATED_PAUSED_MS = longPreferencesKey("active_accumulated_paused_ms")
        val STATUS = stringPreferencesKey("active_status")
        val SUBJECT_ID = stringPreferencesKey("active_subject_id")
        val TASK_ID = stringPreferencesKey("active_task_id")
    }

    val activeTimerFlow: Flow<ActiveTimerSnapshot?> = context.activeTimerDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs.toSnapshotOrNull() }

    suspend fun getSnapshot(): ActiveTimerSnapshot? {
        return activeTimerFlow.first()
    }

    suspend fun saveSnapshot(snapshot: ActiveTimerSnapshot) {
        context.activeTimerDataStore.edit { prefs ->
            prefs[Keys.SESSION_ID] = snapshot.sessionId
            prefs[Keys.SESSION_TYPE] = snapshot.sessionType.name
            prefs[Keys.PLANNED_DURATION_SECONDS] = snapshot.plannedDurationSeconds
            prefs[Keys.START_TIMESTAMP_MS] = snapshot.startTimestampMs
            if (snapshot.pauseStartedTimestampMs != null) {
                prefs[Keys.PAUSE_STARTED_MS] = snapshot.pauseStartedTimestampMs
            } else {
                prefs.remove(Keys.PAUSE_STARTED_MS)
            }
            prefs[Keys.ACCUMULATED_PAUSED_MS] = snapshot.accumulatedPausedMs
            prefs[Keys.STATUS] = snapshot.status.name
            if (snapshot.subjectId != null) {
                prefs[Keys.SUBJECT_ID] = snapshot.subjectId
            } else {
                prefs.remove(Keys.SUBJECT_ID)
            }
            if (snapshot.taskId != null) {
                prefs[Keys.TASK_ID] = snapshot.taskId
            } else {
                prefs.remove(Keys.TASK_ID)
            }
        }
    }

    suspend fun clear() {
        context.activeTimerDataStore.edit { it.clear() }
    }

    private fun Preferences.toSnapshotOrNull(): ActiveTimerSnapshot? {
        val sessionId = this[Keys.SESSION_ID] ?: return null
        val statusName = this[Keys.STATUS] ?: return null
        val status = runCatching { SessionStatus.valueOf(statusName) }.getOrNull() ?: return null
        if (status != SessionStatus.RUNNING && status != SessionStatus.PAUSED) {
            return null
        }

        val sessionType = runCatching {
            SessionType.valueOf(this[Keys.SESSION_TYPE] ?: SessionType.FOCUS.name)
        }.getOrDefault(SessionType.FOCUS)

        return ActiveTimerSnapshot(
            sessionId = sessionId,
            sessionType = sessionType,
            plannedDurationSeconds = this[Keys.PLANNED_DURATION_SECONDS] ?: 0L,
            startTimestampMs = this[Keys.START_TIMESTAMP_MS] ?: 0L,
            pauseStartedTimestampMs = this[Keys.PAUSE_STARTED_MS],
            accumulatedPausedMs = this[Keys.ACCUMULATED_PAUSED_MS] ?: 0L,
            status = status,
            subjectId = this[Keys.SUBJECT_ID],
            taskId = this[Keys.TASK_ID]
        )
    }
}
