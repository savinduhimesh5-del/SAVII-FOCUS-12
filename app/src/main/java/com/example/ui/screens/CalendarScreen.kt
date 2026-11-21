package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.SaviiUiState
import com.example.ui.components.SaviiEmptyState
import com.example.ui.components.SaviiGlassCard
import com.example.ui.components.SaviiModuleHeader
import com.example.ui.components.SaviiStatusPill
import com.example.ui.components.statusColorFor
import com.example.ui.theme.LocalSaviiColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarWindow(val label: String, val days: Int) {
    DAILY("Today", 1),
    WEEKLY("7 Days", 7),
    MONTHLY("30 Days", 30),
    YEARLY("365 Days", 365),
}

private data class CalendarAgendaItem(
    val uniqueKey: String,
    val title: String,
    val subtitle: String,
    val dateKey: String,
    val epochMs: Long,
    val kindLabel: String,
    val isExam: Boolean,
)

@Composable
fun CalendarScreen(
    uiState: SaviiUiState,
    onBack: () -> Unit,
) {
    var window by remember { mutableStateOf(CalendarWindow.MONTHLY) }
    val dayKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val saviiColors = LocalSaviiColors.current

    val startOfTodayMs = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val agendaItems = remember(uiState.tasks, uiState.exams, window, startOfTodayMs) {
        val maxMs = startOfTodayMs + window.days.toLong() * 86_400_000L
        val taskItems = uiState.tasks.mapNotNull { task ->
            val due = task.deadlineEpochMs ?: return@mapNotNull null
            if (due < startOfTodayMs || due > maxMs) return@mapNotNull null
            CalendarAgendaItem(
                uniqueKey = "task_${task.id}",
                title = task.title,
                subtitle = "${task.category} • ${task.estimatedMinutes}m • ${task.status.label}",
                dateKey = dayKeyFormat.format(Date(due)),
                epochMs = due,
                kindLabel = "Task",
                isExam = false,
            )
        }
        val examItems = uiState.exams.mapNotNull { exam ->
            if (exam.examDateEpochMs < startOfTodayMs || exam.examDateEpochMs > maxMs) return@mapNotNull null
            CalendarAgendaItem(
                uniqueKey = "exam_${exam.id}",
                title = exam.title,
                subtitle = "${exam.category.label} Exam",
                dateKey = dayKeyFormat.format(Date(exam.examDateEpochMs)),
                epochMs = exam.examDateEpochMs,
                kindLabel = "Exam",
                isExam = true,
            )
        }
        (examItems + taskItems).sortedBy { it.epochMs }
    }

    val conflictDays = remember(agendaItems) {
        agendaItems.groupBy { it.dateKey }.filter { (_, items) -> items.size > 1 }.keys
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "calendar_header") {
            SaviiModuleHeader(
                title = "Calendar",
                subtitle = "Linked to Planner & Exams (conflict-checked)",
                onBack = onBack,
            )
        }

        item(key = "window_filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarWindow.entries.forEach { w ->
                    FilterChip(
                        selected = window == w,
                        onClick = { window = w },
                        label = { Text(w.label) },
                    )
                }
            }
        }

        if (conflictDays.isNotEmpty()) {
            item(key = "conflict_banner") {
                SaviiGlassCard(accentBorderColor = saviiColors.statusInProgress) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Schedule conflict",
                            tint = saviiColors.statusInProgress,
                        )
                        Text(
                            text = "${conflictDays.size} day(s) have multiple deadlines or exams scheduled. Consider balancing your workload.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        if (agendaItems.isEmpty()) {
            item(key = "calendar_empty") {
                SaviiEmptyState(
                    icon = Icons.Filled.CalendarMonth,
                    title = "No events in this ${window.label.lowercase()} window",
                    message = "Add tasks with deadlines in Planner or add Exam dates in Exams. They sync here automatically without duplicates.",
                )
            }
        } else {
            items(agendaItems, key = { it.uniqueKey }) { item ->
                val isConflictDay = item.dateKey in conflictDays
                SaviiGlassCard(
                    accentBorderColor = when {
                        item.isExam -> saviiColors.statusImportant.copy(alpha = 0.7f)
                        isConflictDay -> saviiColors.statusInProgress.copy(alpha = 0.6f)
                        else -> null
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayDateFormat.format(Date(item.epochMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SaviiStatusPill(
                            label = if (isConflictDay) "${item.kindLabel} • Conflict" else item.kindLabel,
                            color = if (item.isExam) saviiColors.statusImportant else statusColorFor(com.example.data.TaskStatus.PENDING),
                        )
                    }
                }
            }
        }
    }
}
