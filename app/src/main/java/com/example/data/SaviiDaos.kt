package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

class SaviiTypeConverters {
    @TypeConverter fun fromCurriculum(v: CurriculumType): String = v.name
    @TypeConverter fun toCurriculum(v: String): CurriculumType = CurriculumType.valueOf(v)

    @TypeConverter fun fromMedium(v: StudyMedium): String = v.name
    @TypeConverter fun toMedium(v: String): StudyMedium = StudyMedium.valueOf(v)

    @TypeConverter fun fromSubjectStatus(v: SubjectStatus): String = v.name
    @TypeConverter fun toSubjectStatus(v: String): SubjectStatus = SubjectStatus.valueOf(v)

    @TypeConverter fun fromNodeType(v: SyllabusNodeType): String = v.name
    @TypeConverter fun toNodeType(v: String): SyllabusNodeType = SyllabusNodeType.valueOf(v)

    @TypeConverter fun fromDifficulty(v: DifficultyLevel): String = v.name
    @TypeConverter fun toDifficulty(v: String): DifficultyLevel = DifficultyLevel.valueOf(v)

    @TypeConverter fun fromUnderstanding(v: UnderstandingLevel): String = v.name
    @TypeConverter fun toUnderstanding(v: String): UnderstandingLevel = UnderstandingLevel.valueOf(v)

    @TypeConverter fun fromTaskStatus(v: TaskStatus): String = v.name
    @TypeConverter fun toTaskStatus(v: String): TaskStatus = TaskStatus.valueOf(v)

    @TypeConverter fun fromTaskPriority(v: TaskPriority): String = v.name
    @TypeConverter fun toTaskPriority(v: String): TaskPriority = TaskPriority.valueOf(v)

    @TypeConverter fun fromRepeatRule(v: RepeatRule): String = v.name
    @TypeConverter fun toRepeatRule(v: String): RepeatRule = RepeatRule.valueOf(v)

    @TypeConverter fun fromExamCategory(v: ExamCategory): String = v.name
    @TypeConverter fun toExamCategory(v: String): ExamCategory = ExamCategory.valueOf(v)

    @TypeConverter fun fromFocusMode(v: FocusMode): String = v.name
    @TypeConverter fun toFocusMode(v: String): FocusMode = FocusMode.valueOf(v)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY sortOrder ASC, id ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)
}

@Dao
interface SyllabusNodeDao {
    @Query("SELECT * FROM syllabus_nodes ORDER BY createdAtEpochMs ASC")
    fun observeAllNodes(): Flow<List<SyllabusNodeEntity>>

    @Query("SELECT * FROM syllabus_nodes WHERE subjectId = :subjectId ORDER BY createdAtEpochMs ASC")
    fun observeNodesForSubject(subjectId: Long): Flow<List<SyllabusNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: SyllabusNodeEntity): Long

    @Update
    suspend fun update(node: SyllabusNodeEntity)

    @Delete
    suspend fun delete(node: SyllabusNodeEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAtEpochMs DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM subtasks ORDER BY sortOrder ASC, id ASC")
    fun observeAllSubtasks(): Flow<List<SubtaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDateEpochMs ASC")
    fun observeExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamEntity): Long

    @Update
    suspend fun update(exam: ExamEntity)

    @Delete
    suspend fun delete(exam: ExamEntity)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMs DESC")
    fun observeSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity): Long

    @Delete
    suspend fun delete(session: FocusSessionEntity)
}
