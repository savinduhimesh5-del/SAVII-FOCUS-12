package com.example

import com.example.data.CurriculumType
import com.example.data.ExamCategory
import com.example.data.ExamEntity
import com.example.data.ExamReadinessCalculator
import com.example.data.SaviiDatabase
import com.example.data.SubjectStatus
import com.example.data.SyllabusNodeEntity
import com.example.data.SyllabusNodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExamReadinessTest {

    @Test
    fun `default curriculum contains AL Accounting Business ICT without Economics and AAT 1-3`() {
        val subjects = SaviiDatabase.defaultCurriculumSubjects()
        val alSubjects = subjects.filter { it.curriculum == CurriculumType.AL }
        assertEquals(3, alSubjects.size)
        assertFalse(alSubjects.any { it.name.contains("Economics", ignoreCase = true) })

        val aatSubjects = subjects.filter { it.curriculum == CurriculumType.AAT }
        assertEquals(3, aatSubjects.size)
        assertEquals(SubjectStatus.COMPLETED, aatSubjects[0].status)
        assertEquals(SubjectStatus.IN_PROGRESS, aatSubjects[1].status)
        assertEquals(SubjectStatus.UPCOMING, aatSubjects[2].status)
    }

    @Test
    fun `readiness returns null when no syllabus or revision data exists`() {
        val now = 1_700_000_000_000L
        val exam = ExamEntity(
            id = 1L,
            title = "Term 1 Accounting",
            category = ExamCategory.TERM_TEST,
            subjectId = 1L,
            examDateEpochMs = now + TimeUnit.DAYS.toMillis(15),
        )
        val summary = ExamReadinessCalculator.evaluate(
            exam = exam,
            subjects = SaviiDatabase.defaultCurriculumSubjects(),
            nodes = emptyList(),
            nowEpochMs = now,
        )
        assertEquals(15L, summary.daysRemaining)
        assertTrue(summary.isInPrepWindow)
        assertNull(summary.readinessPercent)
    }

    @Test
    fun `readiness computes from real syllabus completion and revision`() {
        val now = 1_700_000_000_000L
        val exam = ExamEntity(
            id = 2L,
            title = "Class Quiz ICT",
            category = ExamCategory.CLASS_EXAM,
            subjectId = 10L,
            examDateEpochMs = now + TimeUnit.DAYS.toMillis(5),
            revisionProgressPercent = 80,
        )
        val nodes = listOf(
            SyllabusNodeEntity(subjectId = 10L, nodeType = SyllabusNodeType.UNIT, title = "Logic Gates", isCompleted = true),
            SyllabusNodeEntity(subjectId = 10L, nodeType = SyllabusNodeType.UNIT, title = "SQL", isCompleted = false, isWeakTopic = true),
        )
        val summary = ExamReadinessCalculator.evaluate(
            exam = exam,
            subjects = emptyList(),
            nodes = nodes,
            nowEpochMs = now,
        )
        assertTrue(summary.isUrgent)
        assertEquals(50, summary.syllabusCompletionPercent)
        assertEquals(1, summary.weakTopicCount)
        // 50 * 0.65 + 80 * 0.35 = 32.5 + 28 = 60.5 -> 61 - 3 = 58
        assertEquals(58, summary.readinessPercent)
    }
}
