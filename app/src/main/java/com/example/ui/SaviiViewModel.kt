package com.example.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DifficultyLevel
import com.example.data.ExamCategory
import com.example.data.ExamEntity
import com.example.data.FocusMode
import com.example.data.FocusSessionEntity
import com.example.data.PersistedFocusTimerState
import com.example.data.RepeatRule
import com.example.data.SaviiDatabase
import com.example.data.SaviiRepository
import com.example.data.SubjectEntity
import com.example.data.SubjectStatus
import com.example.data.SubtaskEntity
import com.example.data.SyllabusNodeEntity
import com.example.data.SyllabusNodeType
import com.example.data.TaskEntity
import com.example.data.TaskPriority
import com.example.data.TaskStatus
import com.example.data.ThemeMode
import com.example.data.UnderstandingLevel
import com.example.data.UserPreferencesRepository
import com.example.focus.FocusTimerEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SaviiUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val subjects: List<SubjectEntity> = emptyList(),
    val syllabusNodes: List<SyllabusNodeEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val subtasks: List<SubtaskEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val timerState: PersistedFocusTimerState = PersistedFocusTimerState(),
)

private data class CoreRecordsBundle(
    val subjects: List<SubjectEntity>,
    val syllabusNodes: List<SyllabusNodeEntity>,
    val tasks: List<TaskEntity>,
    val subtasks: List<SubtaskEntity>,
    val exams: List<ExamEntity>,
)

class SaviiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SaviiDatabase.getInstance(application)
    private val repository = SaviiRepository(database)
    private val preferences = UserPreferencesRepository(application)

    private val coreBundleFlow = combine(
        repository.subjects,
        repository.syllabusNodes,
        repository.tasks,
        repository.subtasks,
        repository.exams,
    ) { subjects, nodes, tasks, subtasks, exams ->
        CoreRecordsBundle(
            subjects = subjects,
            syllabusNodes = nodes,
            tasks = tasks,
            subtasks = subtasks,
            exams = exams,
        )
    }

    val uiState: StateFlow<SaviiUiState> = combine(
        coreBundleFlow,
        repository.focusSessions,
        preferences.themeMode,
        preferences.focusTimerState,
    ) { bundle, sessions, themeMode, timerState ->
        SaviiUiState(
            isLoading = false,
            themeMode = themeMode,
            subjects = bundle.subjects,
            syllabusNodes = bundle.syllabusNodes,
            tasks = bundle.tasks,
            subtasks = bundle.subtasks,
            exams = bundle.exams,
            focusSessions = sessions,
            timerState = timerState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SaviiUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    // --- Planner CRUD ---
    fun addTask(
        title: String,
        category: String,
        priority: TaskPriority,
        status: TaskStatus,
        estimatedMinutes: Int,
        deadlineEpochMs: Long?,
        repeatRule: RepeatRule,
        notes: String,
        subjectId: Long?,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = trimmed,
                    category = category.trim().ifEmpty { "Study" },
                    priority = priority,
                    status = status,
                    estimatedMinutes = estimatedMinutes.coerceIn(5, 600),
                    deadlineEpochMs = deadlineEpochMs,
                    repeatRule = repeatRule,
                    notes = notes.trim(),
                    progressPercent = if (status == TaskStatus.COMPLETED) 100 else 0,
                    subjectId = subjectId,
                )
            )
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: TaskStatus) {
        viewModelScope.launch {
            val updatedProgress = when (newStatus) {
                TaskStatus.COMPLETED -> 100
                TaskStatus.PENDING -> 0
                else -> task.progressPercent
            }
            repository.updateTask(task.copy(status = newStatus, progressPercent = updatedProgress))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addSubtask(taskId: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertSubtask(
                SubtaskEntity(taskId = taskId, title = trimmed)
            )
            recalculateTaskProgress(taskId)
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            repository.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
            recalculateTaskProgress(subtask.taskId)
        }
    }

    fun deleteSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            repository.deleteSubtask(subtask)
            recalculateTaskProgress(subtask.taskId)
        }
    }

    private suspend fun recalculateTaskProgress(taskId: Long) {
        val state = uiState.value
        val task = state.tasks.firstOrNull { it.id == taskId } ?: return
        val related = state.subtasks.filter { it.taskId == taskId }
        if (related.isEmpty()) return
        val completed = related.count { it.isCompleted }
        val pct = ((completed.toFloat() / related.size.toFloat()) * 100f).toInt()
        val nextStatus = when {
            completed == related.size -> TaskStatus.COMPLETED
            completed > 0 && task.status == TaskStatus.PENDING -> TaskStatus.IN_PROGRESS
            else -> task.status
        }
        repository.updateTask(task.copy(progressPercent = pct, status = nextStatus))
    }

    // --- Learn & Syllabus CRUD ---
    fun updateSubjectStatus(subject: SubjectEntity, status: SubjectStatus) {
        viewModelScope.launch {
            repository.updateSubject(subject.copy(status = status))
        }
    }

    fun addSyllabusNode(
        subjectId: Long,
        parentId: Long?,
        nodeType: SyllabusNodeType,
        title: String,
        difficulty: DifficultyLevel,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertSyllabusNode(
                SyllabusNodeEntity(
                    subjectId = subjectId,
                    parentId = parentId,
                    nodeType = nodeType,
                    title = trimmed,
                    difficulty = difficulty,
                )
            )
        }
    }

    fun toggleSyllabusNodeComplete(node: SyllabusNodeEntity) {
        viewModelScope.launch {
            val nextDone = !node.isCompleted
            val nextUnderstanding = if (nextDone && node.understanding == UnderstandingLevel.NOT_ASSESSED) {
                UnderstandingLevel.CONFIDENT
            } else {
                node.understanding
            }
            repository.updateSyllabusNode(
                node.copy(isCompleted = nextDone, understanding = nextUnderstanding)
            )
        }
    }

    fun cycleNodeUnderstanding(node: SyllabusNodeEntity) {
        val values = UnderstandingLevel.entries
        val next = values[(node.understanding.ordinal + 1) % values.size]
        viewModelScope.launch {
            repository.updateSyllabusNode(
                node.copy(
                    understanding = next,
                    isWeakTopic = next == UnderstandingLevel.NEEDS_WORK,
                )
            )
        }
    }

    fun toggleWeakTopic(node: SyllabusNodeEntity) {
        viewModelScope.launch {
            repository.updateSyllabusNode(node.copy(isWeakTopic = !node.isWeakTopic))
        }
    }

    fun incrementRevision(node: SyllabusNodeEntity) {
        viewModelScope.launch {
            repository.updateSyllabusNode(
                node.copy(
                    revisionCount = node.revisionCount + 1,
                    lastRevisedEpochMs = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deleteSyllabusNode(node: SyllabusNodeEntity) {
        viewModelScope.launch {
            repository.deleteSyllabusNode(node)
        }
    }

    // --- Exams CRUD ---
    fun addExam(
        title: String,
        category: ExamCategory,
        subjectId: Long?,
        examDateEpochMs: Long,
        prepWindowDays: Int,
        weakTopicsNote: String,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertExam(
                ExamEntity(
                    title = trimmed,
                    category = category,
                    subjectId = subjectId,
                    examDateEpochMs = examDateEpochMs,
                    prepWindowDays = prepWindowDays.coerceIn(1, 730),
                    weakTopicsNote = weakTopicsNote.trim(),
                )
            )
        }
    }

    fun updateExamRevision(exam: ExamEntity, percent: Int) {
        viewModelScope.launch {
            repository.updateExam(exam.copy(revisionProgressPercent = percent.coerceIn(0, 100)))
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }

    // --- Focus Timer Engine Actions (SystemClock.elapsedRealtime + DataStore + Room) ---
    fun selectFocusConfig(
        mode: FocusMode,
        durationMinutes: Int,
        subjectId: Long?,
        taskId: Long?,
    ) {
        val current = uiState.value.timerState
        if (current.isRunning) return
        viewModelScope.launch {
            preferences.saveFocusTimerState(
                current.copy(
                    mode = mode,
                    isBreak = false,
                    targetDurationSec = durationMinutes.coerceIn(1, 240) * 60,
                    accumulatedActiveMs = 0L,
                    startElapsedRealtimeMs = 0L,
                    startEpochMs = 0L,
                    linkedSubjectId = subjectId,
                    linkedTaskId = taskId,
                )
            )
        }
    }

    fun startFocus() {
        val current = uiState.value.timerState
        if (current.isRunning && !current.isPaused) return
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()
        viewModelScope.launch {
            preferences.saveFocusTimerState(
                current.copy(
                    isRunning = true,
                    isPaused = false,
                    startElapsedRealtimeMs = nowElapsed,
                    startEpochMs = nowEpoch,
                    accumulatedActiveMs = if (current.isPaused) current.accumulatedActiveMs else 0L,
                )
            )
        }
    }

    fun pauseFocus() {
        val current = uiState.value.timerState
        if (!current.isRunning || current.isPaused) return
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()
        val activeMs = FocusTimerEngine.computeElapsedActiveMs(current, nowElapsed, nowEpoch)
        viewModelScope.launch {
            preferences.saveFocusTimerState(
                current.copy(
                    isRunning = true,
                    isPaused = true,
                    accumulatedActiveMs = activeMs,
                    startElapsedRealtimeMs = 0L,
                    startEpochMs = 0L,
                )
            )
        }
    }

    fun resumeFocus() {
        startFocus()
    }

    fun resetFocus() {
        val current = uiState.value.timerState
        viewModelScope.launch {
            preferences.saveFocusTimerState(
                current.copy(
                    isRunning = false,
                    isPaused = false,
                    isBreak = false,
                    startElapsedRealtimeMs = 0L,
                    startEpochMs = 0L,
                    accumulatedActiveMs = 0L,
                )
            )
        }
    }

    fun finishFocusSession(wasCompleted: Boolean) {
        val current = uiState.value.timerState
        if (!current.isRunning && current.accumulatedActiveMs == 0L) return
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()
        val activeMs = FocusTimerEngine.computeElapsedActiveMs(current, nowElapsed, nowEpoch)
        val actualSec = (activeMs / 1000L).toInt().coerceAtLeast(1)
        val startedEpoch = (nowEpoch - activeMs).coerceAtLeast(0L)

        viewModelScope.launch {
            if (actualSec >= 10 || wasCompleted) {
                repository.recordFocusSession(
                    FocusSessionEntity(
                        mode = current.mode,
                        subjectId = current.linkedSubjectId,
                        taskId = current.linkedTaskId,
                        plannedMinutes = (current.targetDurationSec / 60).coerceAtLeast(1),
                        actualSeconds = actualSec.coerceAtMost(current.targetDurationSec),
                        startedAtEpochMs = startedEpoch,
                        endedAtEpochMs = nowEpoch,
                        wasCompleted = wasCompleted,
                        isBreak = current.isBreak,
                    )
                )
            }
            preferences.saveFocusTimerState(
                current.copy(
                    isRunning = false,
                    isPaused = false,
                    isBreak = false,
                    startElapsedRealtimeMs = 0L,
                    startEpochMs = 0L,
                    accumulatedActiveMs = 0L,
                )
            )
        }
    }

    fun startBreakTimer(breakMinutes: Int = 5) {
        val current = uiState.value.timerState
        if (current.isRunning) return
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()
        viewModelScope.launch {
            preferences.saveFocusTimerState(
                current.copy(
                    isRunning = true,
                    isPaused = false,
                    isBreak = true,
                    targetDurationSec = breakMinutes.coerceIn(1, 60) * 60,
                    startElapsedRealtimeMs = nowElapsed,
                    startEpochMs = nowEpoch,
                    accumulatedActiveMs = 0L,
                )
            )
        }
    }

    fun reconcileCompletedTimerIfNeeded() {
        val current = uiState.value.timerState
        if (!current.isRunning || current.isPaused) return
        val snap = FocusTimerEngine.snapshot(
            state = current,
            nowElapsedRealtimeMs = SystemClock.elapsedRealtime(),
            nowEpochMs = System.currentTimeMillis(),
        )
        if (snap.isFinished) {
            finishFocusSession(wasCompleted = true)
        }
    }

    fun deleteFocusSession(session: FocusSessionEntity) {
        viewModelScope.launch {
            repository.deleteFocusSession(session)
        }
    }
}
