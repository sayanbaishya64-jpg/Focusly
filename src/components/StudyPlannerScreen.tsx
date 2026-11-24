import { useState } from 'react';
import {
  CalendarClock,
  CheckCircle2,
  Circle,
  Pencil,
  Play,
  Plus,
  Trash2,
} from 'lucide-react';
import type { FocuslyStrings } from '../i18n/translations';
import type {
  StudyScheduleFilter,
  StudyTaskItem,
  SubjectItem,
  TaskPriority,
} from '../types/focusly';
import {
  filterAndSortTasks,
  findOverdueUnfinishedTasks,
} from '../utils/studyAnalytics';
import { getLocalDateString, shiftLocalDateString } from '../utils/timerEngine';

interface StudyPlannerScreenProps {
  strings: FocuslyStrings;
  subjects: SubjectItem[];
  tasks: StudyTaskItem[];
  onCreateSubject: (name: string, colorHex: string) => void;
  onDeleteSubject: (subjectId: string) => void;
  onSaveTask: (
    editingTaskId: string | null,
    input: {
      title: string;
      description: string;
      subjectId: string | null;
      plannedDurationMinutes: number;
      priority: TaskPriority;
      scheduledDate: string | null;
    },
  ) => void;
  onToggleTaskStatus: (task: StudyTaskItem) => void;
  onRescheduleTask: (taskId: string, scheduledDate: string | null) => void;
  onRescheduleAllOverdue: () => void;
  onDeleteTask: (taskId: string) => void;
  onStartFocusForTask: (task: StudyTaskItem) => void;
}

const SUBJECT_SWATCHES = [
  '#3B82F6',
  '#22D3EE',
  '#22C55E',
  '#F59E0B',
  '#8B5CF6',
  '#EC4899',
];

export function StudyPlannerScreen({
  strings,
  subjects,
  tasks,
  onCreateSubject,
  onDeleteSubject,
  onSaveTask,
  onToggleTaskStatus,
  onRescheduleTask,
  onRescheduleAllOverdue,
  onDeleteTask,
  onStartFocusForTask,
}: StudyPlannerScreenProps) {
  const todayDate = getLocalDateString();
  const tomorrowDate = shiftLocalDateString(todayDate, 1);

  const [scheduleFilter, setScheduleFilter] = useState<StudyScheduleFilter>('TODAY');
  const [subjectFilterId, setSubjectFilterId] = useState<string | null>(null);
  const [showSubjectsModal, setShowSubjectsModal] = useState(false);
  const [showTaskModal, setShowTaskModal] = useState(false);
  const [editingTask, setEditingTask] = useState<StudyTaskItem | null>(null);

  // Subject modal state
  const [newSubjectName, setNewSubjectName] = useState('');
  const [newSubjectColor, setNewSubjectColor] = useState(SUBJECT_SWATCHES[0]);

  // Task modal state
  const [taskTitle, setTaskTitle] = useState('');
  const [taskNotes, setTaskNotes] = useState('');
  const [taskMinutes, setTaskMinutes] = useState('25');
  const [taskSubjectId, setTaskSubjectId] = useState<string | null>(null);
  const [taskPriority, setTaskPriority] = useState<TaskPriority>('MEDIUM');
  const [taskScheduledDate, setTaskScheduledDate] = useState<string | null>(todayDate);

  const overdueTasks = findOverdueUnfinishedTasks(tasks, todayDate);
  const activeSubjectFilter =
    subjectFilterId && subjects.some((s) => s.id === subjectFilterId) ? subjectFilterId : null;
  const visibleTasks = filterAndSortTasks(tasks, scheduleFilter, activeSubjectFilter, todayDate);

  const subjectMap = new Map(subjects.map((s) => [s.id, s]));

  const openAddTaskModal = () => {
    setEditingTask(null);
    setTaskTitle('');
    setTaskNotes('');
    setTaskMinutes('25');
    setTaskSubjectId(activeSubjectFilter);
    setTaskPriority('MEDIUM');
    setTaskScheduledDate(todayDate);
    setShowTaskModal(true);
  };

  const openEditTaskModal = (task: StudyTaskItem) => {
    setEditingTask(task);
    setTaskTitle(task.title);
    setTaskNotes(task.description);
    setTaskMinutes(String(task.plannedDurationMinutes));
    setTaskSubjectId(task.subjectId);
    setTaskPriority(task.priority);
    setTaskScheduledDate(task.scheduledDate ?? null);
    setShowTaskModal(true);
  };

  const handleTaskSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskTitle.trim()) return;
    const parsed = Number.parseInt(taskMinutes, 10);
    onSaveTask(editingTask?.id ?? null, {
      title: taskTitle.trim(),
      description: taskNotes.trim(),
      subjectId: taskSubjectId,
      plannedDurationMinutes: Number.isNaN(parsed) ? 25 : Math.min(480, Math.max(5, parsed)),
      priority: taskPriority,
      scheduledDate: taskScheduledDate,
    });
    setShowTaskModal(false);
  };

  const handleCreateSubjectSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSubjectName.trim()) return;
    onCreateSubject(newSubjectName.trim(), newSubjectColor);
    setNewSubjectName('');
  };

  const scheduleFilters: { id: StudyScheduleFilter; label: string }[] = [
    { id: 'TODAY', label: strings.phase4.filterToday },
    { id: 'UPCOMING', label: strings.phase4.filterUpcoming },
    { id: 'ALL', label: strings.phase4.filterAll },
    { id: 'COMPLETED', label: strings.phase4.filterCompleted },
  ];

  const getPriorityLabel = (p: TaskPriority) => {
    if (p === 'HIGH') return strings.phase4.priorityHigh;
    if (p === 'LOW') return strings.phase4.priorityLow;
    return strings.phase4.priorityMedium;
  };

  const getScheduleLabel = (dateStr?: string | null) => {
    if (dateStr === todayDate) return strings.phase4.scheduleToday;
    if (dateStr === tomorrowDate) return strings.phase4.scheduleTomorrow;
    if (!dateStr) return strings.phase4.scheduleUnscheduled;
    return dateStr;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl sm:text-3xl font-bold text-white">
            {strings.phase4.studyTitle}
          </h1>
          <p className="text-sm sm:text-base text-[#A7B4C8]">
            {strings.phase4.studySubtitle}
          </p>
        </div>

        <button
          type="button"
          onClick={openAddTaskModal}
          className="min-h-[44px] px-5 py-2.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold inline-flex items-center justify-center gap-2 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
        >
          <Plus className="w-4 h-4 shrink-0" />
          <span>{strings.phase4.addTask}</span>
        </button>
      </header>

      {/* Overdue Unfinished Tasks Reschedule Banner */}
      {overdueTasks.length > 0 && (
        <section className="rounded-2xl bg-[#16243D] border border-[#F59E0B]/40 p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="space-y-1">
            <h2 className="text-base font-semibold text-[#F59E0B]">
              {strings.phase4.overdueTitle(overdueTasks.length)}
            </h2>
            <p className="text-sm text-[#A7B4C8]">{strings.phase4.overdueDesc}</p>
          </div>
          <button
            type="button"
            onClick={onRescheduleAllOverdue}
            className="min-h-[44px] px-4 py-2 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#22D3EE] text-[#22D3EE] text-sm font-semibold whitespace-nowrap shrink-0 cursor-pointer"
          >
            {strings.phase4.moveToToday}
          </button>
        </section>
      )}

      {/* Subjects Toolbar */}
      <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 space-y-3.5">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-base font-semibold text-white">
            {strings.phase4.manageSubjects}
          </h2>
          <button
            type="button"
            onClick={() => setShowSubjectsModal(true)}
            className="min-h-[40px] px-3.5 py-1.5 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] text-xs sm:text-sm font-semibold text-[#22D3EE] inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>{strings.phase4.addSubject}</span>
          </button>
        </div>

        {subjects.length === 0 ? (
          <p className="text-sm text-[#A7B4C8]">{strings.phase4.noSubjects}</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setSubjectFilterId(null)}
              className={`min-h-[40px] px-3.5 py-1.5 rounded-xl border text-xs sm:text-sm font-medium transition-colors whitespace-nowrap cursor-pointer ${
                activeSubjectFilter === null
                  ? 'bg-[#3B82F6] border-[#3B82F6] text-white'
                  : 'bg-[#101B32] border-[#233554] text-[#A7B4C8] hover:text-white'
              }`}
            >
              {strings.phase4.filterAll}
            </button>
            {subjects.map((subj) => {
              const selected = activeSubjectFilter === subj.id;
              return (
                <button
                  key={subj.id}
                  type="button"
                  onClick={() => setSubjectFilterId(selected ? null : subj.id)}
                  className={`min-h-[40px] px-3.5 py-1.5 rounded-xl border text-xs sm:text-sm font-medium inline-flex items-center gap-2 transition-colors whitespace-nowrap cursor-pointer ${
                    selected
                      ? 'bg-[#3B82F6] border-[#3B82F6] text-white'
                      : 'bg-[#101B32] border-[#233554] text-[#A7B4C8] hover:text-white'
                  }`}
                >
                  <span
                    className="w-2.5 h-2.5 rounded-full shrink-0"
                    style={{ backgroundColor: subj.subjectColor }}
                  />
                  <span>{subj.subjectName}</span>
                </button>
              );
            })}
          </div>
        )}
      </section>

      {/* Schedule Filter Segmented Bar */}
      <div className="flex flex-wrap items-center gap-2 p-1.5 rounded-xl bg-[#101B32] border border-[#233554] w-fit">
        {scheduleFilters.map((tab) => {
          const active = scheduleFilter === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setScheduleFilter(tab.id)}
              className={`min-h-[40px] px-4 py-1.5 rounded-lg text-sm font-semibold transition-colors whitespace-nowrap cursor-pointer ${
                active
                  ? 'bg-[#3B82F6] text-white'
                  : 'text-[#A7B4C8] hover:text-white'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Study Tasks List */}
      {visibleTasks.length === 0 ? (
        <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-8 text-center space-y-3">
          <h2 className="text-lg font-semibold text-white">
            {strings.phase4.emptyTasksTitle}
          </h2>
          <p className="text-sm text-[#A7B4C8] max-w-md mx-auto">
            {strings.phase4.emptyTasksDesc}
          </p>
          <div className="pt-2">
            <button
              type="button"
              onClick={openAddTaskModal}
              className="min-h-[44px] px-5 py-2.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold inline-flex items-center gap-2 whitespace-nowrap cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>{strings.phase4.addTask}</span>
            </button>
          </div>
        </section>
      ) : (
        <div className="space-y-3">
          {visibleTasks.map((task) => {
            const isCompleted = task.status === 'COMPLETED';
            const subj = task.subjectId ? subjectMap.get(task.subjectId) : undefined;
            const isOverdue =
              !isCompleted && Boolean(task.scheduledDate) && (task.scheduledDate as string) < todayDate;

            return (
              <article
                key={task.id}
                className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 space-y-3"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3 min-w-0 flex-1">
                    <button
                      type="button"
                      onClick={() => onToggleTaskStatus(task)}
                      aria-label={task.title}
                      className="min-h-[44px] min-w-[44px] -ml-2 -mt-2 flex items-center justify-center text-[#A7B4C8] hover:text-white shrink-0 cursor-pointer"
                    >
                      {isCompleted ? (
                        <CheckCircle2 className="w-6 h-6 text-[#22C55E]" />
                      ) : (
                        <Circle className="w-6 h-6" />
                      )}
                    </button>

                    <div className="min-w-0 space-y-1">
                      <h3
                        className={`text-base font-semibold ${
                          isCompleted ? 'line-through text-[#A7B4C8]' : 'text-white'
                        }`}
                      >
                        {task.title}
                      </h3>

                      {task.description && (
                        <p className="text-sm text-[#A7B4C8]">{task.description}</p>
                      )}

                      {/* Unboxed clean metadata separated by dots */}
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-[#A7B4C8]">
                        {subj && (
                          <>
                            <span style={{ color: subj.subjectColor }} className="font-medium">
                              {subj.subjectName}
                            </span>
                            <span aria-hidden="true">·</span>
                          </>
                        )}
                        <span className="font-mono-tabular">
                          {task.plannedDurationMinutes} {strings.home.minutesUnit}
                        </span>
                        <span aria-hidden="true">·</span>
                        <span
                          className={
                            task.priority === 'HIGH'
                              ? 'text-[#F59E0B] font-medium'
                              : task.priority === 'MEDIUM'
                                ? 'text-[#22D3EE] font-medium'
                                : 'text-[#A7B4C8]'
                          }
                        >
                          {getPriorityLabel(task.priority)}
                        </span>
                        <span aria-hidden="true">·</span>
                        <span className={isOverdue ? 'text-[#F59E0B] font-medium' : ''}>
                          {getScheduleLabel(task.scheduledDate)}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      type="button"
                      onClick={() => openEditTaskModal(task)}
                      aria-label={strings.phase4.editTask}
                      className="min-h-[44px] min-w-[44px] rounded-xl flex items-center justify-center text-[#A7B4C8] hover:text-white hover:bg-[#101B32] transition-colors cursor-pointer"
                    >
                      <Pencil className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => onDeleteTask(task.id)}
                      aria-label={strings.phase4.deleteLabel}
                      className="min-h-[44px] min-w-[44px] rounded-xl flex items-center justify-center text-[#A7B4C8] hover:text-[#F59E0B] hover:bg-[#101B32] transition-colors cursor-pointer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {!isCompleted && (
                  <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-[#233554]">
                    <div className="flex flex-wrap items-center gap-2">
                      {task.scheduledDate !== todayDate && (
                        <button
                          type="button"
                          onClick={() => onRescheduleTask(task.id, todayDate)}
                          className="min-h-[40px] px-3 py-1.5 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#22D3EE] text-xs font-medium text-[#22D3EE] inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
                        >
                          <CalendarClock className="w-3.5 h-3.5" />
                          <span>{strings.phase4.scheduleToday}</span>
                        </button>
                      )}
                      {task.scheduledDate !== tomorrowDate && (
                        <button
                          type="button"
                          onClick={() => onRescheduleTask(task.id, tomorrowDate)}
                          className="min-h-[40px] px-3 py-1.5 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] text-xs font-medium text-[#A7B4C8] hover:text-white whitespace-nowrap cursor-pointer"
                        >
                          {strings.phase4.scheduleTomorrow}
                        </button>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={() => onStartFocusForTask(task)}
                      className="min-h-[44px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs sm:text-sm font-semibold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
                    >
                      <Play className="w-4 h-4 fill-current" />
                      <span>{strings.phase4.focusNow}</span>
                    </button>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}

      {/* Manage Subjects Modal */}
      {showSubjectsModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-5"
        >
          <div className="w-full max-w-md rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-5">
            <h2 className="text-lg font-bold text-white">
              {strings.phase4.manageSubjects}
            </h2>

            <form onSubmit={handleCreateSubjectSubmit} className="space-y-4">
              <div className="space-y-1.5">
                <label className="block text-xs text-[#A7B4C8]">
                  {strings.phase4.subjectNameHint}
                </label>
                <input
                  type="text"
                  required
                  value={newSubjectName}
                  onChange={(e) => setNewSubjectName(e.target.value)}
                  className="w-full min-h-[46px] px-3.5 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm"
                />
              </div>

              <div className="flex items-center gap-2.5">
                {SUBJECT_SWATCHES.map((hex) => (
                  <button
                    key={hex}
                    type="button"
                    onClick={() => setNewSubjectColor(hex)}
                    aria-label={hex}
                    className={`w-7 h-7 rounded-full cursor-pointer transition-transform ${
                      newSubjectColor === hex ? 'ring-2 ring-white scale-110' : ''
                    }`}
                    style={{ backgroundColor: hex }}
                  />
                ))}
              </div>

              <button
                type="submit"
                disabled={!newSubjectName.trim()}
                className="w-full min-h-[44px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 text-white text-sm font-semibold cursor-pointer"
              >
                {strings.phase4.addSubject}
              </button>
            </form>

            {subjects.length > 0 && (
              <div className="max-h-48 overflow-y-auto divide-y divide-[#233554] border-t border-[#233554] pt-2">
                {subjects.map((subj) => (
                  <div
                    key={subj.id}
                    className="py-2 flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-2.5">
                      <span
                        className="w-3 h-3 rounded-full shrink-0"
                        style={{ backgroundColor: subj.subjectColor }}
                      />
                      <span className="text-sm text-white">{subj.subjectName}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => onDeleteSubject(subj.id)}
                      aria-label={strings.phase4.deleteLabel}
                      className="min-h-[40px] min-w-[40px] flex items-center justify-center text-[#A7B4C8] hover:text-[#F59E0B] cursor-pointer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div className="flex justify-end pt-1">
              <button
                type="button"
                onClick={() => setShowSubjectsModal(false)}
                className="min-h-[44px] px-4 py-2 rounded-xl border border-[#233554] text-sm font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
              >
                {strings.phase4.closeLabel}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add / Edit Task Modal */}
      {showTaskModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-5"
        >
          <form
            onSubmit={handleTaskSubmit}
            className="w-full max-w-lg rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-4 max-h-[90vh] overflow-y-auto"
          >
            <h2 className="text-lg font-bold text-white">
              {editingTask ? strings.phase4.editTask : strings.phase4.addTask}
            </h2>

            <div className="space-y-1.5">
              <label className="block text-xs text-[#A7B4C8]">
                {strings.dashboard.taskTitleLabel}
              </label>
              <input
                type="text"
                required
                value={taskTitle}
                onChange={(e) => setTaskTitle(e.target.value)}
                className="w-full min-h-[46px] px-3.5 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs text-[#A7B4C8]">
                {strings.phase4.taskNotesLabel}
              </label>
              <input
                type="text"
                value={taskNotes}
                onChange={(e) => setTaskNotes(e.target.value)}
                className="w-full min-h-[46px] px-3.5 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="block text-xs text-[#A7B4C8]">
                  {strings.dashboard.plannedMinutesLabel}
                </label>
                <input
                  type="text"
                  inputMode="numeric"
                  value={taskMinutes}
                  onChange={(e) => setTaskMinutes(e.target.value.replace(/\D/g, '').slice(0, 3))}
                  className="w-full min-h-[46px] px-3.5 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm font-mono-tabular"
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs text-[#A7B4C8]">
                  {strings.timer.subjectPickerLabel}
                </label>
                <select
                  value={taskSubjectId ?? ''}
                  onChange={(e) => setTaskSubjectId(e.target.value || null)}
                  className="w-full min-h-[46px] px-3.5 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm"
                >
                  <option value="">{strings.timer.noneOption}</option>
                  {subjects.map((subj) => (
                    <option key={subj.id} value={subj.id}>
                      {subj.subjectName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="space-y-1.5">
              <span className="block text-xs text-[#A7B4C8]">
                {strings.phase4.priorityLabel}
              </span>
              <div className="grid grid-cols-3 gap-2">
                {(['LOW', 'MEDIUM', 'HIGH'] as TaskPriority[]).map((prio) => (
                  <button
                    key={prio}
                    type="button"
                    onClick={() => setTaskPriority(prio)}
                    className={`min-h-[44px] px-3 rounded-xl border text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer ${
                      taskPriority === prio
                        ? 'bg-[#3B82F6] border-[#3B82F6] text-white'
                        : 'bg-[#16243D] border-[#233554] text-[#A7B4C8]'
                    }`}
                  >
                    {getPriorityLabel(prio)}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-1.5">
              <span className="block text-xs text-[#A7B4C8]">
                {strings.phase4.scheduleLabel}
              </span>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { value: todayDate, label: strings.phase4.scheduleToday },
                  { value: tomorrowDate, label: strings.phase4.scheduleTomorrow },
                  { value: null, label: strings.phase4.scheduleUnscheduled },
                ].map((opt) => {
                  const selected = taskScheduledDate === opt.value;
                  return (
                    <button
                      key={opt.label}
                      type="button"
                      onClick={() => setTaskScheduledDate(opt.value)}
                      className={`min-h-[44px] px-3 rounded-xl border text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer ${
                        selected
                          ? 'bg-[#3B82F6] border-[#3B82F6] text-white'
                          : 'bg-[#16243D] border-[#233554] text-[#A7B4C8]'
                      }`}
                    >
                      {opt.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowTaskModal(false)}
                className="min-h-[44px] px-4 py-2 rounded-xl border border-[#233554] text-sm font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
              >
                {strings.profile.cancelLabel}
              </button>
              <button
                type="submit"
                disabled={!taskTitle.trim()}
                className="min-h-[44px] px-5 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 text-white text-sm font-semibold cursor-pointer"
              >
                {strings.dashboard.saveTask}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
