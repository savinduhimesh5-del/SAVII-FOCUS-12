# SAVII FOCUS — Your Personal Intelligence Ecosystem

Native Android (Kotlin, Jetpack Compose, Material 3) personal study, planning, and focus ecosystem built for **G.C.E. A/L** (Accounting, Business Studies, ICT — Sinhala Medium) and **AAT Sri Lanka** (Levels 1–3).

## Implemented Foundations (Phase 2)
- **Design System**: Obsidian (`#05070E`), Midnight Navy surfaces, Electric Violet (`#7C4DFF`), Electric Blue accents, Dark / Light / System theme modes persisted via DataStore, plus custom bundled fonts (`Plus Jakarta Sans`, `Inter`, `JetBrains Mono`).
- **Offline-First Storage**: Room (`SaviiDatabase`) with reactive `Flow` DAOs + DataStore (`UserPreferencesRepository`).
- **Home Dashboard**: Real-time date/time, active Focus status, exam prep priority card, active tasks, study progress, and honest empty states.
- **Learn & Exams**: Hierarchical syllabus tracking (Units / Lessons / Subtopics / Activities), difficulty, understanding checks, weak topics, revision counter, and exam readiness countdowns (A/L, AAT, Term Test, Class Exam).
- **Planner & Calendar**: Tasks & subtasks with color-coded statuses (Pending Blue, In Progress Yellow, Completed Green, Incomplete Red, Overdue, Important Purple) and calendar conflict detection.
- **Focus Timer**: Monotonic `SystemClock.elapsedRealtime()` + DataStore persistence surviving backgrounding, screen off, and process death without background polling.
- **Insights**: Custom Compose `Canvas` charts computed strictly from real stored Room records.

## How to Build
1. Open in Android Studio or use Gradle 9.3.1+ with JDK 17/21:
   ```bash
   gradle assembleDebug
   gradle :app:testDebugUnitTest
   ```
2. Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.
