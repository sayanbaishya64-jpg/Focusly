package com.focusly.app.domain.usecase

import com.focusly.app.core.model.AppLanguage
import com.focusly.app.core.model.OnboardingPreferences
import com.focusly.app.core.model.StudentCategory
import com.focusly.app.core.model.StudyGoal
import org.json.JSONArray
import org.json.JSONObject

data class BackupSubjectPayload(
    val id: String,
    val subjectName: String,
    val subjectColor: Long,
    val createdAt: Long,
    val updatedAt: Long
)

data class BackupTaskPayload(
    val id: String,
    val title: String,
    val description: String,
    val subjectId: String?,
    val plannedDurationMinutes: Int,
    val priority: String,
    val status: String,
    val scheduledDate: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class BackupSessionPayload(
    val id: String,
    val plannedDurationSeconds: Long,
    val actualDurationSeconds: Long,
    val startTimestamp: Long,
    val completionTimestamp: Long?,
    val pausedDurationSeconds: Long,
    val sessionStatus: String,
    val sessionType: String,
    val localDate: String,
    val subjectId: String?,
    val taskId: String?,
    val createdAt: Long
)

data class BackupTransactionPayload(
    val id: String,
    val type: String,
    val amount: Int,
    val title: String,
    val referenceId: String?,
    val localDate: String,
    val createdAt: Long
)

data class BackupAchievementPayload(
    val achievementId: String,
    val unlockedAt: Long
)

data class BackupGardenItemPayload(
    val itemId: String,
    val purchasedAt: Long,
    val isFeatured: Boolean
)

data class FocuslyBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val preferences: OnboardingPreferences,
    val studentName: String,
    val subjects: List<BackupSubjectPayload>,
    val tasks: List<BackupTaskPayload>,
    val sessions: List<BackupSessionPayload>,
    val rewardTransactions: List<BackupTransactionPayload>,
    val unlockedAchievements: List<BackupAchievementPayload>,
    val unlockedGardenItems: List<BackupGardenItemPayload>
)

object FocuslyBackupCodec {
    const val SUPPORTED_SCHEMA_VERSION = 1

    fun encode(payload: FocuslyBackupPayload): String {
        val root = JSONObject()
        root.put("schemaVersion", payload.schemaVersion)
        root.put("exportedAt", payload.exportedAt)
        root.put("studentName", payload.studentName)

        val prefsObj = JSONObject().apply {
            put("onboardingCompleted", payload.preferences.onboardingCompleted)
            put("studentCategory", payload.preferences.studentCategory.key)
            put("studyGoals", JSONArray(payload.preferences.studyGoals.map { it.key }))
            put("dailyFocusTargetMinutes", payload.preferences.dailyFocusTargetMinutes)
            put("language", payload.preferences.language.languageTag)
            put("selectedDistractionApps", JSONArray(payload.preferences.selectedDistractionApps.toList()))
            put("distractionPreferencesSkipped", payload.preferences.distractionPreferencesSkipped)
        }
        root.put("preferences", prefsObj)

        val subjectsArr = JSONArray()
        payload.subjects.forEach { s ->
            subjectsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("subjectName", s.subjectName)
                    put("subjectColor", s.subjectColor)
                    put("createdAt", s.createdAt)
                    put("updatedAt", s.updatedAt)
                }
            )
        }
        root.put("subjects", subjectsArr)

        val tasksArr = JSONArray()
        payload.tasks.forEach { t ->
            tasksArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("description", t.description)
                    put("subjectId", t.subjectId ?: JSONObject.NULL)
                    put("plannedDurationMinutes", t.plannedDurationMinutes)
                    put("priority", t.priority)
                    put("status", t.status)
                    put("scheduledDate", t.scheduledDate ?: JSONObject.NULL)
                    put("createdAt", t.createdAt)
                    put("updatedAt", t.updatedAt)
                }
            )
        }
        root.put("tasks", tasksArr)

        val sessionsArr = JSONArray()
        payload.sessions.forEach { sess ->
            sessionsArr.put(
                JSONObject().apply {
                    put("id", sess.id)
                    put("plannedDurationSeconds", sess.plannedDurationSeconds)
                    put("actualDurationSeconds", sess.actualDurationSeconds)
                    put("startTimestamp", sess.startTimestamp)
                    put("completionTimestamp", sess.completionTimestamp ?: JSONObject.NULL)
                    put("pausedDurationSeconds", sess.pausedDurationSeconds)
                    put("sessionStatus", sess.sessionStatus)
                    put("sessionType", sess.sessionType)
                    put("localDate", sess.localDate)
                    put("subjectId", sess.subjectId ?: JSONObject.NULL)
                    put("taskId", sess.taskId ?: JSONObject.NULL)
                    put("createdAt", sess.createdAt)
                }
            )
        }
        root.put("sessions", sessionsArr)

        val txArr = JSONArray()
        payload.rewardTransactions.forEach { tx ->
            txArr.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("title", tx.title)
                    put("referenceId", tx.referenceId ?: JSONObject.NULL)
                    put("localDate", tx.localDate)
                    put("createdAt", tx.createdAt)
                }
            )
        }
        root.put("rewardTransactions", txArr)

        val achArr = JSONArray()
        payload.unlockedAchievements.forEach { a ->
            achArr.put(
                JSONObject().apply {
                    put("achievementId", a.achievementId)
                    put("unlockedAt", a.unlockedAt)
                }
            )
        }
        root.put("unlockedAchievements", achArr)

        val gardenArr = JSONArray()
        payload.unlockedGardenItems.forEach { g ->
            gardenArr.put(
                JSONObject().apply {
                    put("itemId", g.itemId)
                    put("purchasedAt", g.purchasedAt)
                    put("isFeatured", g.isFeatured)
                }
            )
        }
        root.put("unlockedGardenItems", gardenArr)

        return root.toString(2)
    }

    fun decode(rawJson: String): Result<FocuslyBackupPayload> {
        return runCatching {
            val root = JSONObject(rawJson)
            val version = root.optInt("schemaVersion", -1)
            require(version == SUPPORTED_SCHEMA_VERSION) {
                "Unsupported backup schema version: $version"
            }

            val prefsObj = root.optJSONObject("preferences")
                ?: error("Missing preferences object")

            val goalKeys = mutableSetOf<String>()
            val goalsArr = prefsObj.optJSONArray("studyGoals")
            if (goalsArr != null) {
                for (i in 0 until goalsArr.length()) {
                    goalsArr.optString(i).takeIf { it.isNotBlank() }?.let { goalKeys += it }
                }
            }

            val appSet = mutableSetOf<String>()
            val appsArr = prefsObj.optJSONArray("selectedDistractionApps")
            if (appsArr != null) {
                for (i in 0 until appsArr.length()) {
                    appsArr.optString(i).takeIf { it.isNotBlank() }?.let { appSet += it }
                }
            }

            val preferences = OnboardingPreferences(
                onboardingCompleted = prefsObj.optBoolean("onboardingCompleted", true),
                studentCategory = StudentCategory.fromKey(prefsObj.optString("studentCategory")),
                studyGoals = StudyGoal.fromKeys(goalKeys),
                dailyFocusTargetMinutes = prefsObj.optInt("dailyFocusTargetMinutes", 60).coerceIn(10, 480),
                language = AppLanguage.fromTag(prefsObj.optString("language")),
                selectedDistractionApps = appSet,
                distractionPreferencesSkipped = prefsObj.optBoolean("distractionPreferencesSkipped", false)
            )

            val subjectsArr = root.optJSONArray("subjects") ?: error("Missing subjects array")
            val subjects = buildList {
                for (i in 0 until subjectsArr.length()) {
                    val o = subjectsArr.getJSONObject(i)
                    val id = o.getString("id")
                    val name = o.getString("subjectName")
                    add(
                        BackupSubjectPayload(
                            id = id,
                            subjectName = name,
                            subjectColor = o.optLong("subjectColor", 0xFF3B82F6L),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val tasksArr = root.optJSONArray("tasks") ?: error("Missing tasks array")
            val tasks = buildList {
                for (i in 0 until tasksArr.length()) {
                    val o = tasksArr.getJSONObject(i)
                    add(
                        BackupTaskPayload(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            description = o.optString("description", ""),
                            subjectId = o.optNullableString("subjectId"),
                            plannedDurationMinutes = o.optInt("plannedDurationMinutes", 25).coerceIn(5, 480),
                            priority = o.optString("priority", "MEDIUM"),
                            status = o.optString("status", "PENDING"),
                            scheduledDate = o.optNullableString("scheduledDate"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val sessionsArr = root.optJSONArray("sessions") ?: error("Missing sessions array")
            val sessions = buildList {
                for (i in 0 until sessionsArr.length()) {
                    val o = sessionsArr.getJSONObject(i)
                    add(
                        BackupSessionPayload(
                            id = o.getString("id"),
                            plannedDurationSeconds = o.getLong("plannedDurationSeconds").coerceAtLeast(1L),
                            actualDurationSeconds = o.getLong("actualDurationSeconds").coerceAtLeast(0L),
                            startTimestamp = o.getLong("startTimestamp"),
                            completionTimestamp = if (o.isNull("completionTimestamp")) null else o.optLong("completionTimestamp"),
                            pausedDurationSeconds = o.optLong("pausedDurationSeconds", 0L).coerceAtLeast(0L),
                            sessionStatus = o.getString("sessionStatus"),
                            sessionType = o.getString("sessionType"),
                            localDate = o.getString("localDate"),
                            subjectId = o.optNullableString("subjectId"),
                            taskId = o.optNullableString("taskId"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val txArr = root.optJSONArray("rewardTransactions") ?: JSONArray()
            val transactions = buildList {
                for (i in 0 until txArr.length()) {
                    val o = txArr.getJSONObject(i)
                    add(
                        BackupTransactionPayload(
                            id = o.getString("id"),
                            type = o.getString("type"),
                            amount = o.getInt("amount"),
                            title = o.getString("title"),
                            referenceId = o.optNullableString("referenceId"),
                            localDate = o.getString("localDate"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val achArr = root.optJSONArray("unlockedAchievements") ?: JSONArray()
            val achievements = buildList {
                for (i in 0 until achArr.length()) {
                    val o = achArr.getJSONObject(i)
                    add(
                        BackupAchievementPayload(
                            achievementId = o.getString("achievementId"),
                            unlockedAt = o.optLong("unlockedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val gardenArr = root.optJSONArray("unlockedGardenItems") ?: JSONArray()
            val gardenItems = buildList {
                for (i in 0 until gardenArr.length()) {
                    val o = gardenArr.getJSONObject(i)
                    add(
                        BackupGardenItemPayload(
                            itemId = o.getString("itemId"),
                            purchasedAt = o.optLong("purchasedAt", System.currentTimeMillis()),
                            isFeatured = o.optBoolean("isFeatured", false)
                        )
                    )
                }
            }

            FocuslyBackupPayload(
                schemaVersion = version,
                exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
                preferences = preferences,
                studentName = root.optString("studentName", ""),
                subjects = subjects,
                tasks = tasks,
                sessions = sessions,
                rewardTransactions = transactions,
                unlockedAchievements = achievements,
                unlockedGardenItems = gardenItems
            )
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotEmpty() }
    }
}
