package com.example.data

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light"),
}

enum class CurriculumType(val label: String) {
    AL("G.C.E. A/L"),
    AAT("AAT Sri Lanka"),
}

enum class StudyMedium(val label: String) {
    SINHALA("Sinhala Medium"),
    ENGLISH("English Medium"),
}

enum class SubjectStatus(val label: String) {
    COMPLETED("Completed"),
    IN_PROGRESS("In Progress"),
    UPCOMING("Upcoming"),
}

enum class SyllabusNodeType(val label: String) {
    UNIT("Unit"),
    LESSON("Lesson"),
    SUBTOPIC("Subtopic"),
    ACTIVITY("Learning Activity"),
}

enum class DifficultyLevel(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
}

enum class UnderstandingLevel(val label: String) {
    NOT_ASSESSED("Not Checked"),
    NEEDS_WORK("Needs Work"),
    CONFIDENT("Confident"),
    MASTERED("Mastered"),
}

enum class TaskStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    INCOMPLETE("Incomplete"),
    OVERDUE("Overdue"),
    IMPORTANT("Important"),
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical"),
}

enum class RepeatRule(val label: String) {
    NONE("No Repeat"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
}

enum class ExamCategory(val label: String, val defaultPrepDays: Int) {
    AL("A/L", 365),
    AAT("AAT", 60),
    TERM_TEST("Term Test", 30),
    CLASS_EXAM("Class Exam", 7),
}

enum class FocusMode(val label: String, val defaultMinutes: Int, val defaultBreakMinutes: Int) {
    NORMAL("Normal", 25, 5),
    STUDY("Study", 45, 10),
    DEEP_FOCUS("Deep Focus", 60, 15),
    EXAM("Exam Drill", 90, 15),
    SLEEP("Wind Down", 20, 0),
    CUSTOM("Custom", 30, 5),
}
