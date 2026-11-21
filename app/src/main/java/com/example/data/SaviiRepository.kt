package com.example.data

import kotlinx.coroutines.flow.Flow

class SaviiRepository(private val db: SaviiDatabase) {
    private val subjectDao = db.subjectDao()
    private val syllabusNodeDao = db.syllabusNodeDao()
    private val taskDao = db.taskDao()
    private val examDao = db.examDao()
    private val focusSessionDao = db.focusSessionDao()

    val subjects: Flow<List<SubjectEntity>> = subjectDao.observeSubjects()
    val syllabusNodes: Flow<List<SyllabusNodeEntity>> = syllabusNodeDao.observeAllNodes()
    val tasks: Flow<List<TaskEntity>> = taskDao.observeTasks()
    val subtasks: Flow<List<SubtaskEntity>> = taskDao.observeAllSubtasks()
    val exams: Flow<List<ExamEntity>> = examDao.observeExams()
    val focusSessions: Flow<List<FocusSessionEntity>> = focusSessionDao.observeSessions()

    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.update(subject)

    suspend fun insertSyllabusNode(node: SyllabusNodeEntity): Long = syllabusNodeDao.insert(node)
    suspend fun updateSyllabusNode(node: SyllabusNodeEntity) = syllabusNodeDao.update(node)
    suspend fun deleteSyllabusNode(node: SyllabusNodeEntity) = syllabusNodeDao.delete(node)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insert(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

    suspend fun insertSubtask(subtask: SubtaskEntity): Long = taskDao.insertSubtask(subtask)
    suspend fun updateSubtask(subtask: SubtaskEntity) = taskDao.updateSubtask(subtask)
    suspend fun deleteSubtask(subtask: SubtaskEntity) = taskDao.deleteSubtask(subtask)

    suspend fun insertExam(exam: ExamEntity): Long = examDao.insert(exam)
    suspend fun updateExam(exam: ExamEntity) = examDao.update(exam)
    suspend fun deleteExam(exam: ExamEntity) = examDao.delete(exam)

    suspend fun recordFocusSession(session: FocusSessionEntity): Long = focusSessionDao.insert(session)
    suspend fun deleteFocusSession(session: FocusSessionEntity) = focusSessionDao.delete(session)
}
