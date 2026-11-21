package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExamReadinessCalculator
import com.example.data.TaskStatus
import com.example.focus.FocusTimerEngine
import com.example.ui.SaviiUiState
import com.example.ui.components.SaviiEmptyState
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiModuleHeader
import com.example.ui.components.SaviiStatusPill
import com.example.ui.components.statusColorFor
import com.example.ui.navigation.SaviiRoute
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalSaviiColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: SaviiUiState,
    onNavigate: (String) -> Unit,
    onReconcileTimer: () -> Unit,
) {
    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nowElapsedMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(uiState.timerState.isRunning, uiState.timerState.isPaused) {
        while (true) {
            nowEpochMs = System.currentTimeMillis()
            nowElapsedMs = SystemClock.elapsedRealtime()
            if (uiState.timerState.isRunning && !uiState.timerState.isPaused) {
                onReconcileTimer()
                delay(1_000L)
            } else {
                delay(30_000L)
            }
        }
    }

    val dateFormatted = remember(nowEpochMs / 60_000L) {
        SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(nowEpochMs))
    }

    val timerSnapshot = remember(uiState.timerState, nowElapsedMs, nowEpochMs) {
        FocusTimerEngine.snapshot(uiState.timerState, nowElapsedMs, nowEpochMs)
    }

    val examSummaries = remember(uiState.exams, uiState.subjects, uiState.syllabusNodes, nowEpochMs / 3_600_000L) {
        uiState.exams.map { ExamReadinessCalculator.evaluate(it, uiState.subjects, uiState.syllabusNodes, nowEpochMs) }
    }
    val urgentExams = remember(examSummaries) {
        examSummaries.filter { it.daysRemaining >= 0 && it.isInPrepWindow }.sortedBy { it.daysRemaining }
    }
    val activeTasks = remember(uiState.tasks) {
        uiState.tasks.filter { it.status != TaskStatus.COMPLETED }.take(5)
    }
    val completedSyllabus = remember(uiState.syllabusNodes) {
        uiState.syllabusNodes.count { it.isCompleted }
    }
    val totalSyllabus = uiState.syllabusNodes.size
    val totalFocusMinutes = remember(uiState.focusSessions) {
        uiState.focusSessions.filter { !it.isBreak }.sumOf { it.actualSeconds } / 60
    }
    val saviiColors = LocalSaviiColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            SaviiModuleHeader(
                title = "SAVII FOCUS",
                subtitle = dateFormatted,
                trailingAction = {
                    SaviiStatusPill(
                        label = "Protocol: Off",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { onNavigate(SaviiRoute.PROTOCOL) },
                    )
                },
            )
        }

        // Adaptive Exam Priority Card: surfaces near-term exams automatically
        if (urgentExams.isNotEmpty()) {
            val topExam = urgentExams.first()
            item(key = "urgent_exam") {
                SaviiGlassCard(accentBorderColor = saviiColors.statusImportant.copy(alpha = 0.7f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EXAM PREP PRIORITY • ${topExam.exam.category.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = saviiColors.statusImportant,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = topExam.exam.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = buildString {
                                    append("${topExam.daysRemaining} days left")
                                    topExam.readinessPercent?.let { append(" • Readiness $it%") }
                                    if (topExam.weakTopicCount > 0) append(" • ${topExam.weakTopicCount} weak topics")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(
                            onClick = { onNavigate(SaviiRoute.EXAMS) },
                            modifier = Modifier.testTag("home_open_exams_btn"),
                        ) {
                            Text("Prep")
                        }
                    }
                }
            }
        }

        // Focus Status Card
        item(key = "focus_card") {
            SaviiGlassCard(
                accentBorderColor = if (uiState.timerState.isRunning) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                } else null,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Focus status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    uiState.timerState.isRunning && uiState.timerState.isPaused ->
                                        "${uiState.timerState.mode.label} • Paused"
                                    uiState.timerState.isRunning ->
                                        "${uiState.timerState.mode.label} • Active"
                                    else -> "Focus Ready • ${uiState.timerState.mode.label}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FocusTimerEngine.formatMmSs(timerSnapshot.remainingSeconds),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (uiState.focusSessions.isEmpty()) {
                                "No completed sessions recorded yet"
                            } else {
                                "${uiState.focusSessions.size} sessions • $totalFocusMinutes min logged"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { onNavigate(SaviiRoute.FOCUS) },
                        modifier = Modifier.testTag("home_focus_action"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (uiState.timerState.isRunning) "Open" else "Focus")
                    }
                }
            }
        }

        // Quick Actions Row
        item(key = "quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onNavigate(SaviiRoute.PLANNER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_task"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Planner", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { onNavigate(SaviiRoute.LEARN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_learn"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Learn", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { onNavigate(SaviiRoute.EXAMS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_exams"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exams", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Study Progress Summary (Real Data)
        item(key = "study_progress") {
            SaviiGlassCard(
                modifier = Modifier.clickable { onNavigate(SaviiRoute.LEARN) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Study Progress (A/L & AAT)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (totalSyllabus == 0) "0 topics" else "$completedSyllabus / $totalSyllabus",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (totalSyllabus > 0) {
                    LinearProgressIndicator(
                        progress = { completedSyllabus.toFloat() / totalSyllabus.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = "A/L: Accounting, Business Studies, ICT • AAT: L1 Done, L2 Active, L3 Future",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Active Tasks & Deadlines
        item(key = "tasks_heading") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tasks & Deadlines",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onNavigate(SaviiRoute.PLANNER) }
                        .padding(8.dp),
                )
            }
        }

        if (activeTasks.isEmpty()) {
            item(key = "empty_tasks") {
                SaviiEmptyState(
                    icon = Icons.Filled.CalendarMonth,
                    title = "No tasks scheduled yet",
                    message = "Add real A/L or AAT tasks in Planner so SAVII FOCUS can track deadlines and progress.",
                    actionLabel = "Create First Task",
                    actionTag = "home_create_task_btn",
                    onAction = { onNavigate(SaviiRoute.PLANNER) },
                )
            }
        } else {
            items(activeTasks, key = { it.id }) { task ->
                SaviiGlassCard(
                    modifier = Modifier.clickable { onNavigate(SaviiRoute.PLANNER) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${task.category} • ${task.estimatedMinutes} min • ${task.priority.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SaviiStatusPill(
                            label = task.status.label,
                            color = statusColorFor(task.status),
                        )
                    }
                }
            }
        }

        // Local Analysis / Recommendations Card (Honest, no fake AI)
        item(key = "local_recommendation") {
            val recommendationText = when {
                urgentExams.isNotEmpty() -> {
                    val nearest = urgentExams.first()
                    "Focus recommendation: ${nearest.exam.title} is ${nearest.daysRemaining} days away. Prioritize weak topics and revision blocks."
                }
                totalSyllabus == 0 ->
                    "Local setup tip: Add your A/L or AAT units inside Learn to unlock real syllabus completion and exam readiness tracking."
                activeTasks.isEmpty() ->
                    "Your task list is clear. Start a Focus session or plan your next revision block."
                else ->
                    "You have ${activeTasks.size} active task(s). Start a 45-minute Study session to make measurable progress."
            }
            SaviiGlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Local guidance",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Local Guidance (Approval-First)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = recommendationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onNavigate(SaviiRoute.SAVII_AI) }) {
                        Text("SAVII AI Status")
                    }
                    OutlinedButton(onClick = { onNavigate(SaviiRoute.SHIELD) }) {
                        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shield")
                    }
                }
            }
        }
    }
}
