package com.focusly.app.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.domain.model.StudyScheduleFilter
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.TaskStatus
import com.focusly.app.domain.repository.StudyTaskRepository
import com.focusly.app.domain.repository.SubjectRepository
import com.focusly.app.domain.usecase.StudyAnalyticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class StudyPlannerUiState(
    val isLoading: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    val allTasks: List<StudyTask> = emptyList(),
    val filteredTasks: List<StudyTask> = emptyList(),
    val overdueTasksCount: Int = 0,
    val scheduleFilter: StudyScheduleFilter = StudyScheduleFilter.TODAY,
    val selectedSubjectFilterId: String? = null,
    val todayDate: String = LocalDate.now().toString(),
    val tomorrowDate: String = LocalDate.now().plusDays(1).toString(),
    val errorMessage: String? = null
)

@HiltViewModel
class StudyPlannerViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val studyTaskRepository: StudyTaskRepository
) : ViewModel() {

    private val scheduleFilterState = MutableStateFlow(StudyScheduleFilter.TODAY)
    private val subjectFilterState = MutableStateFlow<String?>(null)
    private val errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StudyPlannerUiState> = combine(
        subjectRepository.observeAllSubjects(),
        studyTaskRepository.observeAllTasks(),
        scheduleFilterState,
        subjectFilterState,
        errorState
    ) { subjects, tasks, scheduleFilter, subjectFilterId, error ->
        val today = LocalDate.now()
        val todayStr = today.toString()
        val tomorrowStr = today.plusDays(1).toString()

        val validSubjectFilter = if (subjectFilterId != null && subjects.any { it.id == subjectFilterId }) {
            subjectFilterId
        } else {
            null
        }

        val overdueCount = StudyAnalyticsCalculator.findOverdueUnfinishedTasks(
            tasks = tasks,
            todayDate = todayStr
        ).size

        val filtered = StudyAnalyticsCalculator.filterAndSortTasks(
            tasks = tasks,
            filter = scheduleFilter,
            subjectFilterId = validSubjectFilter,
            todayDate = todayStr
        )

        StudyPlannerUiState(
            isLoading = false,
            subjects = subjects,
            allTasks = tasks,
            filteredTasks = filtered,
            overdueTasksCount = overdueCount,
            scheduleFilter = scheduleFilter,
            selectedSubjectFilterId = validSubjectFilter,
            todayDate = todayStr,
            tomorrowDate = tomorrowStr,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudyPlannerUiState()
    )

    fun selectScheduleFilter(filter: StudyScheduleFilter) {
        scheduleFilterState.value = filter
    }

    fun selectSubjectFilter(subjectId: String?) {
        subjectFilterState.value = if (subjectFilterState.value == subjectId) null else subjectId
    }

    fun createSubject(name: String, colorHex: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                subjectRepository.upsertSubject(name.trim(), colorHex)
            }.onFailure { err ->
                errorState.value = err.message ?: "Unable to save subject"
            }
        }
    }

    fun updateSubject(subjectId: String, name: String, colorHex: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                subjectRepository.updateSubject(subjectId, name.trim(), colorHex)
            }.onFailure { err ->
                errorState.value = err.message ?: "Unable to update subject"
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            runCatching {
                if (subjectFilterState.value == subjectId) {
                    subjectFilterState.value = null
                }
                subjectRepository.deleteSubject(subjectId)
            }.onFailure { err ->
                errorState.value = err.message ?: "Unable to delete subject"
            }
        }
    }

    fun saveTask(
        editingTaskId: String?,
        title: String,
        description: String,
        subjectId: String?,
        plannedMinutes: Int,
        priority: TaskPriority,
        scheduledDate: String?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                if (editingTaskId == null) {
                    studyTaskRepository.createTask(
                        title = title.trim(),
                        description = description.trim(),
                        subjectId = subjectId,
                        plannedDurationMinutes = plannedMinutes,
                        priority = priority,
                        scheduledDate = scheduledDate
                    )
                } else {
                    studyTaskRepository.updateTask(
                        taskId = editingTaskId,
                        title = title.trim(),
                        description = description.trim(),
                        subjectId = subjectId,
                        plannedDurationMinutes = plannedMinutes,
                        priority = priority,
                        scheduledDate = scheduledDate
                    )
                }
            }.onFailure { err ->
                errorState.value = err.message ?: "Unable to save study task"
            }
        }
    }

    fun toggleTaskCompleted(task: StudyTask) {
        val nextStatus = if (task.status == TaskStatus.COMPLETED) {
            TaskStatus.PENDING
        } else {
            TaskStatus.COMPLETED
        }
        viewModelScope.launch {
            studyTaskRepository.updateTaskStatus(task.id, nextStatus)
        }
    }

    fun rescheduleTaskToDate(taskId: String, scheduledDate: String?) {
        viewModelScope.launch {
            studyTaskRepository.rescheduleTask(taskId, scheduledDate)
        }
    }

    fun rescheduleAllOverdueToToday() {
        val todayStr = LocalDate.now().toString()
        viewModelScope.launch {
            studyTaskRepository.rescheduleUnfinishedTasksToDate(
                beforeDate = todayStr,
                targetDate = todayStr
            )
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            studyTaskRepository.deleteTask(taskId)
        }
    }

    fun clearError() {
        errorState.value = null
    }
}
