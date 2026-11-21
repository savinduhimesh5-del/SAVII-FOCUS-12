package com.example.data

import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class ExamReadinessSummary(
    val exam: ExamEntity,
    val subjectName: String?,
    val daysRemaining: Long,
    val isInPrepWindow: Boolean,
    val isUrgent: Boolean,
    val syllabusCompletionPercent: Int?,
    val weakTopicCount: Int,
    val readinessPercent: Int?,
)

object ExamReadinessCalculator {

    fun daysUntil(examDateEpochMs: Long, nowEpochMs: Long): Long {
        val diff = examDateEpochMs - nowEpochMs
        return if (diff >= 0) {
            TimeUnit.MILLISECONDS.toDays(diff)
        } else {
            -TimeUnit.MILLISECONDS.toDays(-diff)
        }
    }

    fun evaluate(
        exam: ExamEntity,
        subjects: List<SubjectEntity>,
        nodes: List<SyllabusNodeEntity>,
        nowEpochMs: Long,
    ): ExamReadinessSummary {
        val subject = subjects.firstOrNull { it.id == exam.subjectId }
        val relevantNodes = if (exam.subjectId != null) {
            nodes.filter { it.subjectId == exam.subjectId }
        } else {
            emptyList()
        }
        val syllabusPercent = if (relevantNodes.isNotEmpty()) {
            val done = relevantNodes.count { it.isCompleted }
            ((done.toFloat() / relevantNodes.size.toFloat()) * 100f).roundToInt()
        } else {
            null
        }
        val weakCount = relevantNodes.count { it.isWeakTopic }
        val daysLeft = daysUntil(exam.examDateEpochMs, nowEpochMs)
        val inWindow = daysLeft in 0..exam.prepWindowDays.toLong()
        val urgent = daysLeft in 0..7

        // Only compute readiness from real stored syllabus + revision data; never invent a score if empty.
        val readiness = when {
            syllabusPercent != null -> {
                val base = (syllabusPercent * 0.65f + exam.revisionProgressPercent * 0.35f)
                val weakPenalty = (weakCount * 3).coerceAtMost(15)
                (base.roundToInt() - weakPenalty).coerceIn(0, 100)
            }
            exam.revisionProgressPercent > 0 -> exam.revisionProgressPercent.coerceIn(0, 100)
            else -> null
        }

        return ExamReadinessSummary(
            exam = exam,
            subjectName = subject?.name,
            daysRemaining = daysLeft,
            isInPrepWindow = inWindow,
            isUrgent = urgent,
            syllabusCompletionPercent = syllabusPercent,
            weakTopicCount = weakCount,
            readinessPercent = readiness,
        )
    }
}
