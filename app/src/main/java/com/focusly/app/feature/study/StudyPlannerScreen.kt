package com.focusly.app.feature.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focusly.app.R
import com.focusly.app.core.designsystem.components.FocuslyCard
import com.focusly.app.core.designsystem.components.FocuslyPrimaryButton
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.domain.model.StudyScheduleFilter
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.TaskStatus

private val SUBJECT_COLOR_SWATCHES = listOf(
    0xFF3B82F6L,
    0xFF22D3EEL,
    0xFF22C55EL,
    0xFFF59E0BL,
    0xFF8B5CF6L,
    0xFFEC4899L
)

@Composable
fun StudyPlannerScreen(
    state: StudyPlannerUiState,
    onSelectScheduleFilter: (StudyScheduleFilter) -> Unit,
    onSelectSubjectFilter: (String?) -> Unit,
    onCreateSubject: (name: String, colorHex: Long) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onSaveTask: (
        editingTaskId: String?,
        title: String,
        description: String,
        subjectId: String?,
        plannedMinutes: Int,
        priority: TaskPriority,
        scheduledDate: String?
    ) -> Unit,
    onToggleTaskCompleted: (StudyTask) -> Unit,
    onRescheduleTask: (taskId: String, scheduledDate: String?) -> Unit,
    onRescheduleAllOverdue: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onStartFocusForTask: (StudyTask) -> Unit
) {
    var showSubjectDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<StudyTask?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FocuslyColors.PrimaryBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = FocuslyColors.PrimaryAccent)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocuslyColors.PrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + Primary Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.study_planner_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = FocuslyColors.PrimaryText
                )
                Text(
                    text = stringResource(R.string.study_planner_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        editingTask = null
                        showTaskDialog = true
                    },
                shape = RoundedCornerShape(12.dp),
                color = FocuslyColors.PrimaryAccent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = FocuslyColors.PrimaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.study_add_task),
                        style = MaterialTheme.typography.labelLarge,
                        color = FocuslyColors.PrimaryText
                    )
                }
            }
        }

        // Overdue unfinished tasks rescheduling banner
        if (state.overdueTasksCount > 0) {
            FocuslyCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.study_overdue_banner_title, state.overdueTasksCount),
                            style = MaterialTheme.typography.titleMedium,
                            color = FocuslyColors.Warning
                        )
                        Text(
                            text = stringResource(R.string.study_overdue_banner_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FocuslyColors.SecondaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(onClick = onRescheduleAllOverdue) {
                        Text(
                            text = stringResource(R.string.study_move_to_today),
                            style = MaterialTheme.typography.labelLarge,
                            color = FocuslyColors.SecondaryAccent
                        )
                    }
                }
            }
        }

        // Subjects section
        FocuslyCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.study_subjects_heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = FocuslyColors.PrimaryText
                )
                TextButton(onClick = { showSubjectDialog = true }) {
                    Text(
                        text = stringResource(R.string.study_add_subject),
                        style = MaterialTheme.typography.labelLarge,
                        color = FocuslyColors.SecondaryAccent
                    )
                }
            }

            if (state.subjects.isEmpty()) {
                Text(
                    text = stringResource(R.string.study_no_subjects),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FocuslyColors.SecondaryText
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterButtonChip(
                        label = stringResource(R.string.study_filter_all),
                        selected = state.selectedSubjectFilterId == null,
                        dotColor = null,
                        onClick = { onSelectSubjectFilter(null) }
                    )
                    state.subjects.forEach { subject ->
                        FilterButtonChip(
                            label = subject.name,
                            selected = state.selectedSubjectFilterId == subject.id,
                            dotColor = Color(subject.colorHex),
                            onClick = { onSelectSubjectFilter(subject.id) }
                        )
                    }
                }
            }
        }

        // Schedule filter tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                StudyScheduleFilter.TODAY to stringResource(R.string.study_filter_today),
                StudyScheduleFilter.UPCOMING to stringResource(R.string.study_filter_upcoming),
                StudyScheduleFilter.ALL to stringResource(R.string.study_filter_all),
                StudyScheduleFilter.COMPLETED to stringResource(R.string.study_filter_completed)
            )
            filters.forEach { (filter, label) ->
                FilterButtonChip(
                    label = label,
                    selected = state.scheduleFilter == filter,
                    dotColor = null,
                    onClick = { onSelectScheduleFilter(filter) }
                )
            }
        }

        // Task List
        if (state.filteredTasks.isEmpty()) {
            FocuslyCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.study_empty_tasks_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = FocuslyColors.PrimaryText
                    )
                    Text(
                        text = stringResource(R.string.study_empty_tasks_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FocuslyColors.SecondaryText
                    )
                    FocuslyPrimaryButton(
                        text = stringResource(R.string.study_add_task),
                        onClick = {
                            editingTask = null
                            showTaskDialog = true
                        }
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.filteredTasks.forEach { task ->
                    StudyTaskItemCard(
                        task = task,
                        todayDate = state.todayDate,
                        tomorrowDate = state.tomorrowDate,
                        onToggleCompleted = { onToggleTaskCompleted(task) },
                        onEdit = {
                            editingTask = task
                            showTaskDialog = true
                        },
                        onDelete = { onDeleteTask(task.id) },
                        onRescheduleToday = { onRescheduleTask(task.id, state.todayDate) },
                        onRescheduleTomorrow = { onRescheduleTask(task.id, state.tomorrowDate) },
                        onStartFocus = { onStartFocusForTask(task) }
                    )
                }
            }
        }
    }

    if (showSubjectDialog) {
        ManageSubjectsDialog(
            subjects = state.subjects,
            onDismiss = { showSubjectDialog = false },
            onCreateSubject = onCreateSubject,
            onDeleteSubject = onDeleteSubject
        )
    }

    if (showTaskDialog) {
        StudyTaskEditorDialog(
            editingTask = editingTask,
            subjects = state.subjects,
            todayDate = state.todayDate,
            tomorrowDate = state.tomorrowDate,
            onDismiss = { showTaskDialog = false },
            onSave = { title, description, subjectId, minutes, priority, scheduledDate ->
                onSaveTask(
                    editingTask?.id,
                    title,
                    description,
                    subjectId,
                    minutes,
                    priority,
                    scheduledDate
                )
                showTaskDialog = false
            }
        )
    }
}

@Composable
private fun FilterButtonChip(
    label: String,
    selected: Boolean,
    dotColor: Color?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) FocuslyColors.PrimaryAccent else FocuslyColors.SecondaryBackground,
        border = BorderStroke(
            1.dp,
            if (selected) FocuslyColors.PrimaryAccent else FocuslyColors.CardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) FocuslyColors.PrimaryText else FocuslyColors.SecondaryText
            )
        }
    }
}

@Composable
private fun StudyTaskItemCard(
    task: StudyTask,
    todayDate: String,
    tomorrowDate: String,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRescheduleToday: () -> Unit,
    onRescheduleTomorrow: () -> Unit,
    onStartFocus: () -> Unit
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isOverdue = !isCompleted && task.scheduledDate != null && task.scheduledDate < todayDate

    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> FocuslyColors.Warning
        TaskPriority.MEDIUM -> FocuslyColors.SecondaryAccent
        TaskPriority.LOW -> FocuslyColors.SecondaryText
    }
    val priorityLabel = when (task.priority) {
        TaskPriority.HIGH -> stringResource(R.string.study_priority_high)
        TaskPriority.MEDIUM -> stringResource(R.string.study_priority_medium)
        TaskPriority.LOW -> stringResource(R.string.study_priority_low)
    }
    val scheduleText = when (task.scheduledDate) {
        todayDate -> stringResource(R.string.study_schedule_today)
        tomorrowDate -> stringResource(R.string.study_schedule_tomorrow)
        null -> stringResource(R.string.study_schedule_unscheduled)
        else -> task.scheduledDate
    }

    FocuslyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onToggleCompleted,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCompleted) FocuslyColors.Success else FocuslyColors.SecondaryText
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCompleted) FocuslyColors.SecondaryText else FocuslyColors.PrimaryText,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FocuslyColors.SecondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!task.subjectName.isNullOrBlank()) {
                            Text(
                                text = task.subjectName,
                                style = MaterialTheme.typography.labelMedium,
                                color = FocuslyColors.SecondaryAccent
                            )
                            Text("·", color = FocuslyColors.SecondaryText)
                        }
                        Text(
                            text = "${task.plannedDurationMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = FocuslyColors.SecondaryText
                        )
                        Text("·", color = FocuslyColors.SecondaryText)
                        Text(
                            text = priorityLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = priorityColor
                        )
                        Text("·", color = FocuslyColors.SecondaryText)
                        Text(
                            text = scheduleText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverdue) FocuslyColors.Warning else FocuslyColors.SecondaryText
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.study_edit_task),
                        tint = FocuslyColors.SecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.study_delete),
                        tint = FocuslyColors.SecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (!isCompleted) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.scheduledDate != todayDate) {
                        TextButton(onClick = onRescheduleToday) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = FocuslyColors.SecondaryAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.study_schedule_today),
                                color = FocuslyColors.SecondaryAccent,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    if (task.scheduledDate != tomorrowDate) {
                        TextButton(onClick = onRescheduleTomorrow) {
                            Text(
                                text = stringResource(R.string.study_schedule_tomorrow),
                                color = FocuslyColors.SecondaryText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onStartFocus),
                    shape = RoundedCornerShape(10.dp),
                    color = FocuslyColors.PrimaryAccent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = FocuslyColors.PrimaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.study_focus_now),
                            style = MaterialTheme.typography.labelLarge,
                            color = FocuslyColors.PrimaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageSubjectsDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onCreateSubject: (name: String, colorHex: Long) -> Unit,
    onDeleteSubject: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(SUBJECT_COLOR_SWATCHES.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocuslyColors.SecondaryBackground,
        title = {
            Text(
                text = stringResource(R.string.study_subjects_heading),
                color = FocuslyColors.PrimaryText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.study_subject_name_hint)) },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SUBJECT_COLOR_SWATCHES.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selectedColor == hex) 2.dp else 0.dp,
                                    color = FocuslyColors.PrimaryText,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                if (subjects.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEach { subj ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(subj.colorHex))
                                    )
                                    Text(
                                        text = subj.name,
                                        color = FocuslyColors.PrimaryText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSubject(subj.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.study_delete),
                                        tint = FocuslyColors.SecondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nameInput.isNotBlank(),
                onClick = {
                    onCreateSubject(nameInput, selectedColor)
                    nameInput = ""
                }
            ) {
                Text(
                    text = stringResource(R.string.study_add_subject),
                    color = FocuslyColors.PrimaryAccent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.study_close), color = FocuslyColors.SecondaryText)
            }
        }
    )
}

@Composable
private fun StudyTaskEditorDialog(
    editingTask: StudyTask?,
    subjects: List<Subject>,
    todayDate: String,
    tomorrowDate: String,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        subjectId: String?,
        minutes: Int,
        priority: TaskPriority,
        scheduledDate: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(editingTask?.title ?: "") }
    var description by remember { mutableStateOf(editingTask?.description ?: "") }
    var minutesInput by remember { mutableStateOf((editingTask?.plannedDurationMinutes ?: 25).toString()) }
    var selectedSubjectId by remember { mutableStateOf(editingTask?.subjectId) }
    var selectedPriority by remember { mutableStateOf(editingTask?.priority ?: TaskPriority.MEDIUM) }
    var selectedDate by remember {
        mutableStateOf(
            if (editingTask != null) editingTask.scheduledDate else todayDate
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocuslyColors.SecondaryBackground,
        title = {
            Text(
                text = if (editingTask == null) {
                    stringResource(R.string.study_add_task)
                } else {
                    stringResource(R.string.study_edit_task)
                },
                color = FocuslyColors.PrimaryText
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.study_task_title_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.study_task_notes_label)) },
                    maxLines = 2
                )
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { minutesInput = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.study_task_minutes_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (subjects.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.study_subjects_heading),
                        style = MaterialTheme.typography.labelMedium,
                        color = FocuslyColors.SecondaryText
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterButtonChip(
                            label = stringResource(R.string.study_schedule_unscheduled),
                            selected = selectedSubjectId == null,
                            dotColor = null,
                            onClick = { selectedSubjectId = null }
                        )
                        subjects.forEach { subj ->
                            FilterButtonChip(
                                label = subj.name,
                                selected = selectedSubjectId == subj.id,
                                dotColor = Color(subj.colorHex),
                                onClick = { selectedSubjectId = subj.id }
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.study_priority_heading),
                    style = MaterialTheme.typography.labelMedium,
                    color = FocuslyColors.SecondaryText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        TaskPriority.LOW to stringResource(R.string.study_priority_low),
                        TaskPriority.MEDIUM to stringResource(R.string.study_priority_medium),
                        TaskPriority.HIGH to stringResource(R.string.study_priority_high)
                    ).forEach { (prio, label) ->
                        FilterButtonChip(
                            label = label,
                            selected = selectedPriority == prio,
                            dotColor = null,
                            onClick = { selectedPriority = prio }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.study_schedule_heading),
                    style = MaterialTheme.typography.labelMedium,
                    color = FocuslyColors.SecondaryText
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterButtonChip(
                        label = stringResource(R.string.study_schedule_today),
                        selected = selectedDate == todayDate,
                        dotColor = null,
                        onClick = { selectedDate = todayDate }
                    )
                    FilterButtonChip(
                        label = stringResource(R.string.study_schedule_tomorrow),
                        selected = selectedDate == tomorrowDate,
                        dotColor = null,
                        onClick = { selectedDate = tomorrowDate }
                    )
                    FilterButtonChip(
                        label = stringResource(R.string.study_schedule_unscheduled),
                        selected = selectedDate == null,
                        dotColor = null,
                        onClick = { selectedDate = null }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val mins = minutesInput.toIntOrNull()?.coerceIn(5, 480) ?: 25
                    onSave(title, description, selectedSubjectId, mins, selectedPriority, selectedDate)
                }
            ) {
                Text(stringResource(R.string.study_save), color = FocuslyColors.PrimaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.study_cancel), color = FocuslyColors.SecondaryText)
            }
        }
    )
}
