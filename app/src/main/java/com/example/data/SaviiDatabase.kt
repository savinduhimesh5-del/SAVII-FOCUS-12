package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        SyllabusNodeEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        ExamEntity::class,
        FocusSessionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(SaviiTypeConverters::class)
abstract class SaviiDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun syllabusNodeDao(): SyllabusNodeDao
    abstract fun taskDao(): TaskDao
    abstract fun examDao(): ExamDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: SaviiDatabase? = null

        fun getInstance(context: Context): SaviiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): SaviiDatabase {
            return Room.databaseBuilder(
                appContext,
                SaviiDatabase::class.java,
                "savii_focus.db",
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            INSTANCE?.subjectDao()?.insertAll(defaultCurriculumSubjects())
                        }
                    }
                })
                .build()
        }

        /**
         * Seeds ONLY the user's real declared A/L and AAT curriculum structure
         * (Accounting, Business Studies, ICT — explicitly excluding Economics —
         * plus AAT Level 1 Completed, Level 2 In Progress, Level 3 Upcoming).
         * No fake tasks, fake exams, or fake focus logs are ever seeded.
         */
        fun defaultCurriculumSubjects(): List<SubjectEntity> = listOf(
            SubjectEntity(
                curriculum = CurriculumType.AL,
                name = "Accounting (ගිණුම්කරණය)",
                levelOrStream = "G.C.E. A/L",
                medium = StudyMedium.SINHALA,
                status = SubjectStatus.IN_PROGRESS,
                sortOrder = 1,
            ),
            SubjectEntity(
                curriculum = CurriculumType.AL,
                name = "Business Studies (ව්‍යාපාර අධ්‍යයනය)",
                levelOrStream = "G.C.E. A/L",
                medium = StudyMedium.SINHALA,
                status = SubjectStatus.IN_PROGRESS,
                sortOrder = 2,
            ),
            SubjectEntity(
                curriculum = CurriculumType.AL,
                name = "ICT (තොරතුරු හා සන්නිවේදන තාක්ෂණය)",
                levelOrStream = "G.C.E. A/L",
                medium = StudyMedium.SINHALA,
                status = SubjectStatus.IN_PROGRESS,
                sortOrder = 3,
            ),
            SubjectEntity(
                curriculum = CurriculumType.AAT,
                name = "AAT Level 1",
                levelOrStream = "Foundation",
                medium = StudyMedium.SINHALA,
                status = SubjectStatus.COMPLETED,
                sortOrder = 4,
            ),
            SubjectEntity(
                curriculum = CurriculumType.AAT,
                name = "AAT Level 2",
                levelOrStream = "Intermediate",
                medium = StudyMedium.SINHALA,
                status = SubjectStatus.IN_PROGRESS,
                sortOrder = 5,
            ),
            SubjectEntity(
                curriculum = CurriculumType.AAT,
                name = "AAT Level 3",
                levelOrStream = "Final Level",
                medium = StudyMedium.SINHALA,
                status = SubjectStatus.UPCOMING,
                sortOrder = 6,
            ),
        )
    }
}
