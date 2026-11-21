package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val curriculum: CurriculumType,
    val name: String,
    val levelOrStream: String,
    val medium: StudyMedium = StudyMedium.SINHALA,
    val status: SubjectStatus = SubjectStatus.IN_PROGRESS,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "syllabus_nodes",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("subjectId"), Index("parentId")],
)
data class SyllabusNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val parentId: Long? = null,
    val nodeType: SyllabusNodeType,
    val title: String,
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM,
    val understanding: UnderstandingLevel = UnderstandingLevel.NOT_ASSESSED,
    val isWeakTopic: Boolean = false,
    val isCompleted: Boolean = false,
    val revisionCount: Int = 0,
    val lastRevisedEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "tasks",
    indices = [Index("subjectId"), Index("status"), Index("deadlineEpochMs")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Study",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val estimatedMinutes: Int = 30,
    val deadlineEpochMs: Long? = null,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val reminderEpochMs: Long? = null,
    val notes: String = "",
    val progressPercent: Int = 0,
    val subjectId: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("taskId")],
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "exams",
    indices = [Index("subjectId"), Index("examDateEpochMs")],
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: ExamCategory,
    val subjectId: Long? = null,
    val examDateEpochMs: Long,
    val prepWindowDays: Int = category.defaultPrepDays,
    val weakTopicsNote: String = "",
    val revisionProgressPercent: Int = 0,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "focus_sessions",
    indices = [Index("startedAtEpochMs"), Index("subjectId")],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: FocusMode,
    val subjectId: Long? = null,
    val taskId: Long? = null,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val wasCompleted: Boolean,
    val isBreak: Boolean = false,
)
